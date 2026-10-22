package pl.ceranka.happypies.screen.report_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.ui.common.ClientSpacePeople
import pl.ceranka.happypies.ui.common.formatDateTime
import pl.ceranka.happypies.ui.common.toUserMessage

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailsViewModel(
    private val reportRepository: ReportRepository,
    private val clientRepository: ClientRepository,
    private val accountRepository: AccountRepository,
    private val clientId: String,
    private val reportId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val log = Logger.withTag("ReportDetails")

    private data class Deletion(val inProgress: Boolean = false, val finished: Boolean = false, val error: String? = null)

    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val deletion = MutableStateFlow(Deletion())

    val uiState: StateFlow<ReportDetailsUiState> = combine(
        retries.onStart { emit(Unit) }.flatMapLatest { load() },
        deletion,
    ) { content, deletion ->
        content.copy(
            isDeleted = content.isDeleted && !deletion.inProgress && !deletion.finished,
            isDeleting = deletion.inProgress,
            isDeleteFinished = deletion.finished,
            deleteError = deletion.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportDetailsUiState())

    fun onRetry() {
        retries.tryEmit(Unit)
    }

    fun onDelete() {
        if (deletion.value.inProgress || deletion.value.finished) return

        deletion.value = Deletion(inProgress = true)
        viewModelScope.launch {
            deletion.value = try {
                reportRepository.deleteReport(reportId)
                Deletion(finished = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Deleting report $reportId failed" }
                Deletion(error = e.toUserMessage())
            }
        }
    }

    private fun load(): Flow<ReportDetailsUiState> =
        flow {
            val people = ClientSpacePeople(clientRepository.getClient(clientId), accountRepository.state.value.accountOrNull)
            emitAll(reportRepository.observeReport(reportId).map { report ->
                if (report == null) {
                    ReportDetailsUiState(report = uiState.value.report, isDeleted = true)
                } else {
                    ReportDetailsUiState(report = report.toItem(people))
                }
            })
        }
            .onStart { emit(uiState.value.copy(error = null)) }
            .catch { e ->
                log.e(e) { "Loading report $reportId failed" }
                emit(uiState.value.copy(error = e.toUserMessage()))
            }

    private fun Report.toItem(people: ClientSpacePeople) = ReportDetailsItem(
        title = title,
        description = description,
        addedBy = "${people.nameOf(authorId)} · ${formatDateTime(createdAt, timeZone)}",
        editedBy = if (updatedAt > createdAt) {
            "${people.nameOf(lastEditedBy)} · ${formatDateTime(updatedAt, timeZone)}"
        } else {
            null
        },
        comment = comment?.let {
            CommentItem(
                label = if (it.authorRole == UserRole.TRAINER) "Komentarz trenera" else "Komentarz klienta",
                text = it.text,
                date = formatDateTime(it.updatedAt, timeZone),
            )
        },
    )
}