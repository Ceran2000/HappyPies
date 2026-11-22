import AVFoundation
import Shared

final class IosVideoTranscoder: VideoTranscoder {

    func readDuration(sourceUri: String) async throws -> VideoDurationResult {
        guard let url = URL(string: sourceUri) else {
            return VideoDurationResult.Failure(message: "Invalid file URI: \(sourceUri)")
        }
        do {
            let duration = try await AVURLAsset(url: url).load(.duration)
            guard duration.isNumeric else {
                return VideoDurationResult.Failure(message: "Unknown duration of \(sourceUri)")
            }
            return VideoDurationResult.Success(durationMs: Int64(duration.seconds * 1000))
        } catch {
            if error is CancellationError { throw error }
            return VideoDurationResult.Failure(message: error.localizedDescription)
        }
    }

    func startCompression(
        sourceUri: String,
        outputUri: String,
        settings: VideoCompressionSettings,
        listener: any VideoCompressionListener
    ) -> any CompressionHandle {
        let compression = VideoCompression(listener: listener)
        guard let source = URL(string: sourceUri), let output = URL(string: outputUri) else {
            compression.finish(VideoCompressionEvent.Failure(message: "Invalid file URI"))
            return compression
        }
        Task { await compression.run(source: source, output: output, settings: settings) }
        return compression
    }
}

private final class VideoCompression: CompressionHandle, @unchecked Sendable {

    private static let maxFrameRate = 30.0
    private static let audioSampleRate = 44_100

    private let listener: any VideoCompressionListener
    private let lock = NSLock()
    private var cancelled = false
    private var finished = false
    private var lastPercent: Int32 = -1

    init(listener: any VideoCompressionListener) {
        self.listener = listener
    }

    func cancel() {
        lock.withLock { cancelled = true }
    }

    private var isCancelled: Bool {
        lock.withLock { cancelled }
    }

    func finish(_ event: VideoCompressionEvent) {
        let shouldSend = lock.withLock {
            if finished || cancelled { return false }
            finished = true
            return true
        }
        if shouldSend { listener.onCompressionEvent(event: event) }
    }

    func run(source: URL, output: URL, settings: VideoCompressionSettings) async {
        do {
            try await transcode(source: source, output: output, settings: settings)
        } catch {
            finish(VideoCompressionEvent.Failure(message: error.localizedDescription))
        }
    }

    private func transcode(source: URL, output: URL, settings: VideoCompressionSettings) async throws {
        let asset = AVURLAsset(url: source)
        let duration = try await asset.load(.duration).seconds
        guard let videoTrack = try await asset.loadTracks(withMediaType: .video).first else {
            throw TranscodeError.noVideoTrack
        }
        let audioTrack = try await asset.loadTracks(withMediaType: .audio).first
        let naturalSize = try await videoTrack.load(.naturalSize)
        let transform = try await videoTrack.load(.preferredTransform)
        if isCancelled { return }

        let reader = try AVAssetReader(asset: asset)
        let writer = try AVAssetWriter(outputURL: output, fileType: .mp4)

        let videoOutput = AVAssetReaderTrackOutput(
            track: videoTrack,
            outputSettings: [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange]
        )
        videoOutput.alwaysCopiesSampleData = false
        reader.add(videoOutput)

        let size = Self.outputSize(naturalSize, maxShortSide: CGFloat(settings.maxShortSide))
        let videoInput = AVAssetWriterInput(mediaType: .video, outputSettings: [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: size.width,
            AVVideoHeightKey: size.height,
            AVVideoScalingModeKey: AVVideoScalingModeResizeAspect,
            AVVideoCompressionPropertiesKey: [
                AVVideoAverageBitRateKey: settings.videoBitrate,
                AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel,
                AVVideoExpectedSourceFrameRateKey: Self.maxFrameRate,
                AVVideoMaxKeyFrameIntervalDurationKey: 2
            ]
        ])
        videoInput.transform = transform
        videoInput.expectsMediaDataInRealTime = false
        writer.add(videoInput)

        var audioPipe: (AVAssetReaderTrackOutput, AVAssetWriterInput)?
        if let audioTrack {
            let audioOutput = AVAssetReaderTrackOutput(track: audioTrack, outputSettings: [
                AVFormatIDKey: kAudioFormatLinearPCM,
                AVNumberOfChannelsKey: settings.audioChannels,
                AVSampleRateKey: Self.audioSampleRate,
                AVLinearPCMBitDepthKey: 16,
                AVLinearPCMIsFloatKey: false,
                AVLinearPCMIsBigEndianKey: false,
                AVLinearPCMIsNonInterleaved: false
            ])
            reader.add(audioOutput)
            let audioInput = AVAssetWriterInput(mediaType: .audio, outputSettings: [
                AVFormatIDKey: kAudioFormatMPEG4AAC,
                AVNumberOfChannelsKey: settings.audioChannels,
                AVSampleRateKey: Self.audioSampleRate,
                AVEncoderBitRateKey: settings.audioBitrate
            ])
            audioInput.expectsMediaDataInRealTime = false
            writer.add(audioInput)
            audioPipe = (audioOutput, audioInput)
        }

        guard reader.startReading() else { throw reader.error ?? TranscodeError.readFailed }
        guard writer.startWriting() else { throw writer.error ?? TranscodeError.writeFailed }
        writer.startSession(atSourceTime: .zero)

        let group = DispatchGroup()
        group.enter()
        var lastKeptTime = -Double.infinity
        let minFrameInterval = 1.0 / Self.maxFrameRate - 0.001
        pump(videoOutput, into: videoInput, queue: DispatchQueue(label: "video-compression.video"), group: group) { sample in
            let time = CMSampleBufferGetPresentationTimeStamp(sample).seconds
            if time - lastKeptTime < minFrameInterval { return false }
            lastKeptTime = time
            if duration > 0 { self.reportProgress(time / duration) }
            return true
        }
        if let audioPipe {
            let (audioOutput, audioInput) = audioPipe
            group.enter()
            pump(audioOutput, into: audioInput, queue: DispatchQueue(label: "video-compression.audio"), group: group) { _ in true }
        }

        await withCheckedContinuation { continuation in
            group.notify(queue: .global()) { continuation.resume() }
        }

        if isCancelled || reader.status == .failed || writer.status == .failed {
            let error = reader.error ?? writer.error
            reader.cancelReading()
            writer.cancelWriting()
            if !isCancelled { throw error ?? TranscodeError.readFailed }
            return
        }
        await writer.finishWriting()
        guard writer.status == .completed else { throw writer.error ?? TranscodeError.writeFailed }
        finish(VideoCompressionEvent.Success.shared)
    }

    private func pump(
        _ output: AVAssetReaderTrackOutput,
        into input: AVAssetWriterInput,
        queue: DispatchQueue,
        group: DispatchGroup,
        keep: @escaping (CMSampleBuffer) -> Bool
    ) {
        var done = false
        input.requestMediaDataWhenReady(on: queue) {
            while input.isReadyForMoreMediaData && !done {
                let sample = self.isCancelled ? nil : output.copyNextSampleBuffer()
                guard let sample, !keep(sample) || input.append(sample) else {
                    done = true
                    input.markAsFinished()
                    group.leave()
                    return
                }
            }
        }
    }

    private func reportProgress(_ fraction: Double) {
        let percent = Int32(min(max(fraction, 0), 1) * 100)
        let shouldSend = lock.withLock {
            if percent == lastPercent { return false }
            lastPercent = percent
            return true
        }
        if shouldSend { listener.onCompressionEvent(event: VideoCompressionEvent.Progress(percent: percent)) }
    }

    private static func outputSize(_ size: CGSize, maxShortSide: CGFloat) -> CGSize {
        let shortSide = min(size.width, size.height)
        let scale = shortSide > maxShortSide ? maxShortSide / shortSide : 1
        return CGSize(width: even(size.width * scale), height: even(size.height * scale))
    }

    private static func even(_ value: CGFloat) -> CGFloat {
        CGFloat(Int(value.rounded()) / 2 * 2)
    }
}

private enum TranscodeError: LocalizedError {
    case noVideoTrack
    case readFailed
    case writeFailed

    var errorDescription: String? {
        switch self {
        case .noVideoTrack: "The file has no video track"
        case .readFailed: "Reading the video failed"
        case .writeFailed: "Writing the compressed video failed"
        }
    }
}