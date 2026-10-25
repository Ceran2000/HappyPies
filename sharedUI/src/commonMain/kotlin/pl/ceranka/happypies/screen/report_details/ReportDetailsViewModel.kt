package pl.ceranka.happypies.screen.report_details

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.account.Account
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

    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val deletionState = MutableStateFlow(DeletionState())
    private val commentEditor = MutableStateFlow(CommentEditor())

    val commentText = TextFieldState()

    val uiState: StateFlow<ReportDetailsUiState> = combine(
        retries.onStart { emit(Unit) }.flatMapLatest { load() },
        deletionState,
        commentEditor
    ) { content, deletion, commentEditor ->
        content.copy(
            isDeleted = content.isDeleted && !deletion.inProgress && !deletion.finished,
            isDeleting = deletion.inProgress,
            isDeleteFinished = deletion.finished,
            deleteError = deletion.error,
            commentEditor = commentEditor
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportDetailsUiState())

    private fun load(): Flow<ReportDetailsUiState> =
        flow {
            val viewer = accountRepository.state.value.accountOrNull
            val people = ClientSpacePeople(clientRepository.getClient(clientId), viewer)
            val report = reportRepository.observeReport(reportId).map { report ->
                if (report == null) {
                    ReportDetailsUiState(report = uiState.value.report, isDeleted = true)
                } else {
                    ReportDetailsUiState(report = report.toItem(people, viewer, timeZone))
                }
            }
            emitAll(report)
        }
            .onStart { emit(uiState.value.copy(error = null)) }
            .catch { e ->
                log.e(e) { "Loading report $reportId failed" }
                emit(uiState.value.copy(error = e.toUserMessage()))
            }

    fun onRetry() {
        retries.tryEmit(Unit)
    }

    fun onDelete() {
        if (deletionState.value.inProgress || deletionState.value.finished) return

        deletionState.update { DeletionState(inProgress = true) }
        viewModelScope.launch {
            try {
                reportRepository.deleteReport(reportId)
                deletionState.update { DeletionState(finished = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Deleting report $reportId failed" }
                deletionState.update { DeletionState(error = e.toUserMessage()) }
            }
        }
    }

    fun onAddComment() {
        val report = uiState.value.report ?: return
        if (!report.viewerCanAddComment) return

        commentText.clearText()
        commentEditor.update { CommentEditor(isOpen = true) }
    }

    fun onEditComment() {
        val comment = uiState.value.report?.comment ?: return
        if (!comment.viewerCanEdit) return

        commentText.setTextAndPlaceCursorAtEnd(comment.text)
        commentEditor.update { CommentEditor(isOpen = true) }
    }

    fun onCancelComment() {
        commentEditor.update { if (it.isSaving) it else CommentEditor() }
    }

    fun onSaveComment() {
        val editor = commentEditor.value
        if (!editor.isOpen || editor.isSaving) return

        val text = commentText.text.toString().trim()
        if (text.isEmpty()) {
            commentEditor.update { it.copy(error = "Komentarz nie może być pusty.") }
            return
        }
        val viewer = accountRepository.state.value.accountOrNull ?: return

        writeComment(failureLog = "Saving comment on report $reportId failed") {
            reportRepository.setComment(reportId, text, viewer.uid, viewer.role)
        }
    }

    fun onDeleteComment() {
        val editor = commentEditor.value
        if (uiState.value.report?.comment == null || !editor.isOpen || editor.isSaving) return

        writeComment(failureLog = "Deleting comment on report $reportId failed") {
            reportRepository.deleteComment(reportId)
        }
    }

    /** Locks the editor for the write; closes it on success, keeps it open with the error on failure. */
    private fun writeComment(failureLog: String, write: suspend () -> Unit) {
        commentEditor.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                write()
                commentEditor.update { CommentEditor() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { failureLog }
                commentEditor.update { it.copy(isSaving = false, error = e.toUserMessage()) }
            }
        }
    }

    init {
        snapshotFlow { commentText.text.toString() }
            .onEach { commentEditor.update { it.copy(error = null) } }
            .launchIn(viewModelScope)
    }
}

private data class DeletionState(val inProgress: Boolean = false, val finished: Boolean = false, val error: String? = null)

private fun Report.toItem(people: ClientSpacePeople, viewer: Account?, timeZone: TimeZone) = ReportDetailsItem(
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
            viewerCanEdit = viewer != null && it.authorId == viewer.uid,
        )
    },
    viewerCanAddComment = comment == null && viewer != null && viewer.role != authorRole,
)