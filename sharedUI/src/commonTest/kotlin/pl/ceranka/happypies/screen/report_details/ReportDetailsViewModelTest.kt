package pl.ceranka.happypies.screen.report_details

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.FakeTrainerRepository
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.FakeReportRepository.SavedComment
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailsViewModelTest {

    private val clients = FakeClientRepository()
    private val reports = FakeReportRepository()
    private val client = Account(anna.id, anna.email, anna.name, UserRole.CLIENT)
    private val offline = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."

    private val added = 1_791_191_100_000 // 05.10.2026, 09:05 UTC
    private val later = 1_791_830_400_000 // 12.10.2026, 18:40 UTC
    private val r1 = report("r1", createdAt = added).copy(description = "Trzy próby")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // The state is shared WhileSubscribed, so a test collects it like the screen does.
    private fun TestScope.createViewModel(viewer: Account = trainer) =
        ReportDetailsViewModel(reports, clients, FakeTrainerRepository(), accountOf(viewer), anna.id, "r1", TimeZone.UTC).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    @Test
    fun showsTheReportWithResolvedPeopleAndTheComment() = runTest {
        reports.report = r1.copy(comment = ReportComment(trainer.uid, UserRole.TRAINER, "Dobra robota", later))

        val viewModel = createViewModel()

        assertEquals(
            ReportDetailsUiState(
                report = ReportDetailsItem(
                    title = "Raport r1",
                    description = "Trzy próby",
                    addedBy = "Anna Kowalska · 05.10.2026, 09:05",
                    editedBy = null,
                    comment = CommentItem("Komentarz trenera", "Dobra robota", "12.10.2026, 18:40", viewerCanEdit = true),
                    viewerCanAddComment = false,
                ),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun showsWhoEditedTheReportLast() = runTest {
        reports.report = r1.copy(lastEditedBy = trainer.uid, updatedAt = later)

        val viewModel = createViewModel()

        assertEquals("Jan Trener · 12.10.2026, 18:40", viewModel.uiState.value.report?.editedBy)
    }

    @Test
    fun showsChangesLive() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        reports.report = r1.copy(title = "Nowy tytuł")

        assertEquals("Nowy tytuł", viewModel.uiState.value.report?.title)
    }

    @Test
    fun reportRemovedElsewhereIsShownAsDeleted() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.showDeleteReportButton)

        reports.report = null

        assertTrue(viewModel.uiState.value.isDeleted)
        assertFalse(viewModel.uiState.value.showDeleteReportButton)
    }

    @Test
    fun deletingTheReportFinishes() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        viewModel.onDelete()

        assertEquals(listOf("r1"), reports.deleted)
        val state = viewModel.uiState.value
        assertTrue(state.isDeleteFinished)
        assertEquals(false, state.isDeleting)
    }

    @Test
    fun ownDeletionIsNotShownAsDeletedElsewhere() = runTest {
        reports.report = r1
        val gate = CompletableDeferred<Unit>()
        reports.deleteGate = gate
        val viewModel = createViewModel()

        viewModel.onDelete()
        reports.report = null // the listener sees the local delete before the server confirms it

        assertTrue(viewModel.uiState.value.isDeleting)
        assertEquals(false, viewModel.uiState.value.isDeleted)

        gate.complete(Unit)

        assertTrue(viewModel.uiState.value.isDeleteFinished)
        assertEquals(false, viewModel.uiState.value.isDeleted)
    }

    @Test
    fun failedDeleteShowsTheMessageAndKeepsTheReport() = runTest {
        reports.report = r1
        reports.deleteError = DataException.Unavailable("offline")
        val viewModel = createViewModel()

        viewModel.onDelete()

        val state = viewModel.uiState.value
        assertEquals(offline, state.deleteError)
        assertEquals(false, state.isDeleting)
        assertEquals("Raport r1", state.report?.title)
    }

    @Test
    fun readErrorShowsTheMessageAndRetryLoadsAgain() = runTest {
        reports.failReport(DataException.Unavailable("offline"))
        val viewModel = createViewModel()
        assertEquals(ReportDetailsUiState(error = offline), viewModel.uiState.value)

        reports.report = r1
        viewModel.onRetry()

        assertEquals("Raport r1", viewModel.uiState.value.report?.title)
        assertNull(viewModel.uiState.value.error)
    }

    // Types like the text field does; snapshotFlow sees writes to the global snapshot only after apply notifications.
    private fun ReportDetailsViewModel.type(text: String) {
        commentText.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    private val ReportDetailsViewModel.editor get() = uiState.value.commentEditor

    @Test
    fun theOtherSideCanAddAComment() = runTest {
        reports.report = r1 // added by the client, viewed by the trainer

        val viewModel = createViewModel()

        assertEquals(true, viewModel.uiState.value.report?.viewerCanAddComment)
    }

    @Test
    fun theReportAuthorCannotComment() = runTest {
        reports.report = r1

        val viewModel = createViewModel(viewer = client)

        assertEquals(false, viewModel.uiState.value.report?.viewerCanAddComment)
        viewModel.onAddComment()
        assertEquals(CommentEditor(), viewModel.editor)
    }

    @Test
    fun onlyTheCommentAuthorCanEditIt() = runTest {
        reports.report = r1.copy(comment = ReportComment(trainer.uid, UserRole.TRAINER, "Dobra robota", later))

        val viewModel = createViewModel(viewer = client)

        assertEquals(false, viewModel.uiState.value.report?.comment?.viewerCanEdit)
        viewModel.onEditComment()
        assertEquals(CommentEditor(), viewModel.editor)
    }

    @Test
    fun addingACommentStartsEmptySavesTheTrimmedTextAndClosesTheEditor() = runTest {
        reports.report = r1
        val viewModel = createViewModel()
        viewModel.type("left over from an earlier edit")

        viewModel.onAddComment()
        assertEquals(CommentEditor(isOpen = true), viewModel.editor)
        assertEquals("", viewModel.commentText.text.toString())

        viewModel.type("  Dobra robota  ")
        viewModel.onSaveComment()

        assertEquals(listOf(SavedComment("r1", "Dobra robota", trainer.uid, UserRole.TRAINER)), reports.savedComments)
        assertEquals(CommentEditor(), viewModel.editor)
    }

    @Test
    fun blankCommentShowsAValidationErrorUntilTheTextChanges() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        viewModel.onAddComment()
        viewModel.type("   ")
        viewModel.onSaveComment()

        assertEquals("Komentarz nie może być pusty.", viewModel.editor.error)
        assertTrue(reports.savedComments.isEmpty())

        viewModel.type("D")
        assertNull(viewModel.editor.error)
    }

    @Test
    fun failedSaveKeepsTheEditorOpenWithTheMessageAndTheText() = runTest {
        reports.report = r1
        reports.commentError = DataException.Unavailable("offline")
        val viewModel = createViewModel()

        viewModel.onAddComment()
        viewModel.type("Dobra robota")
        viewModel.onSaveComment()

        assertEquals(CommentEditor(isOpen = true, error = offline), viewModel.editor)
        assertEquals("Dobra robota", viewModel.commentText.text.toString())
    }

    @Test
    fun theEditorIsLockedWhileSaving() = runTest {
        reports.report = r1.copy(comment = ReportComment(trainer.uid, UserRole.TRAINER, "Dobra robota", later))
        reports.commentGate = CompletableDeferred()
        val viewModel = createViewModel()

        viewModel.onEditComment()
        val idle = viewModel.editor
        assertTrue(idle.saveButtonEnabled && idle.cancelButtonEnabled && idle.deleteButtonEnabled && idle.textFieldEnabled)
        assertEquals(true, viewModel.uiState.value.report?.showDeleteCommentButton)
        assertFalse(idle.showSavingProgress)

        viewModel.onSaveComment()

        val saving = viewModel.editor
        assertFalse(saving.saveButtonEnabled || saving.cancelButtonEnabled || saving.deleteButtonEnabled || saving.textFieldEnabled)
        assertTrue(saving.showSavingProgress)
    }

    @Test
    fun cancelDoesNotCloseTheEditorWhileSaving() = runTest {
        reports.report = r1
        val gate = CompletableDeferred<Unit>()
        reports.commentGate = gate
        val viewModel = createViewModel()

        viewModel.onAddComment()
        viewModel.type("Dobra robota")
        viewModel.onSaveComment()
        viewModel.onCancelComment()

        assertTrue(viewModel.editor.isSaving)

        gate.complete(Unit)

        assertEquals(CommentEditor(), viewModel.editor)
    }

    @Test
    fun cancelClosesTheEditorWithoutSaving() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        viewModel.onAddComment()
        viewModel.type("Dobra robota")
        viewModel.onCancelComment()

        assertEquals(CommentEditor(), viewModel.editor)
        assertTrue(reports.savedComments.isEmpty())
    }

    @Test
    fun editingStartsWithTheSavedTextAndCanDeleteTheComment() = runTest {
        reports.report = r1.copy(comment = ReportComment(trainer.uid, UserRole.TRAINER, "Dobra robota", later))
        val viewModel = createViewModel()

        viewModel.onEditComment()
        assertEquals(CommentEditor(isOpen = true), viewModel.editor)
        assertEquals("Dobra robota", viewModel.commentText.text.toString())
        viewModel.onDeleteComment()

        assertEquals(listOf("r1"), reports.deletedComments)
        assertEquals(CommentEditor(), viewModel.editor)
    }

    @Test
    fun aNewCommentCannotBeDeleted() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        viewModel.onAddComment()
        viewModel.onDeleteComment()

        assertTrue(reports.deletedComments.isEmpty())
        assertEquals(CommentEditor(isOpen = true), viewModel.editor)
    }

    @Test
    fun theEditorStaysOpenWhenTheReportChanges() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        viewModel.onAddComment()
        viewModel.type("Dobra")
        reports.report = r1.copy(title = "Nowy tytuł") // edited on another device

        assertEquals("Nowy tytuł", viewModel.uiState.value.report?.title)
        assertEquals(CommentEditor(isOpen = true), viewModel.editor)
        assertEquals("Dobra", viewModel.commentText.text.toString())
    }
}