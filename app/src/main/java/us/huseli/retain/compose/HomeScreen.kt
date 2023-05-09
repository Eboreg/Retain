package us.huseli.retain.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Enums
import us.huseli.retain.R
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    onCloseClick: () -> Unit,
    onTrashClick: () -> Unit,
) {
    TopAppBar(
        title = { Text(selectedCount.toString()) },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.exit_selection_mode)
                )
            }
        },
        actions = {
            IconButton(onClick = onTrashClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_selected_notes)
                )
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegularTopAppBar(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_round),
                modifier = Modifier.height(50.dp),
                contentDescription = null
            )
        },
        actions = {
            IconButton(onClick = onDebugClick) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = stringResource(R.string.debug),
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.app_settings),
                )
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreenImpl(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    notes: List<Note>,
    checklistItems: List<ChecklistItem>,
    selectedNoteIds: Collection<UUID>,
    onAddTextNoteClick: () -> Unit = {},
    onAddChecklistClick: () -> Unit = {},
    onCardClick: (Note) -> Unit = {},
    onEndSelectModeClick: () -> Unit = {},
    onDeleteNotesClick: (Collection<UUID>) -> Unit = {},
    onSelectNote: (Note) -> Unit = {},
    onDeselectNote: (Note) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
) {
    val isSelectEnabled = selectedNoteIds.isNotEmpty()
    var isFABExpanded by rememberSaveable { mutableStateOf(false) }
    var deleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val minColumnWidth by settingsViewModel.minColumnWidth.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (isSelectEnabled) SelectionTopAppBar(
                selectedCount = selectedNoteIds.size,
                onCloseClick = onEndSelectModeClick,
                onTrashClick = { deleteDialogOpen = true },
            ) else RegularTopAppBar(
                onSettingsClick = onSettingsClick,
                onDebugClick = onDebugClick,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        FAB(
            expanded = isFABExpanded,
            onAddTextNoteClick = onAddTextNoteClick,
            onAddChecklistClick = onAddChecklistClick,
            onExpandedChange = { isFABExpanded = it },
            onClose = { isFABExpanded = false },
        )

        if (deleteDialogOpen) {
            DeleteNotesDialog(
                selectedNoteIds = selectedNoteIds,
                onDelete = onDeleteNotesClick,
                onClose = { deleteDialogOpen = false },
            )
        }

        LazyVerticalStaggeredGrid(
            // columns = StaggeredGridCells.Adaptive(minSize = 180.dp),
            columns = StaggeredGridCells.Adaptive(minSize = minColumnWidth.dp),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = modifier
                .clickable(interactionSource = interactionSource, indication = null) { isFABExpanded = false }
                .fillMaxHeight()
                .padding(innerPadding)
        ) {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    modifier = Modifier.fillMaxWidth(),
                    note = note,
                    checklistItems = checklistItems.filter { it.noteId == note.id },
                    onClick = {
                        if (isSelectEnabled) {
                            if (selectedNoteIds.contains(note.id)) onDeselectNote(note)
                            else onSelectNote(note)
                        } else onCardClick(note)
                    },
                    onLongClick = {
                        if (!isSelectEnabled) onSelectNote(note)
                        else onDeselectNote(note)
                    },
                    isSelected = selectedNoteIds.contains(note.id),
                )
            }
        }
    }
}


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onAddTextNoteClick: () -> Unit,
    onAddChecklistClick: () -> Unit,
    onCardClick: (Note) -> Unit,
    onSettingsClick: () -> Unit,
    onDebugClick: () -> Unit,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle(emptyList())
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsStateWithLifecycle(emptyList())
    val checklistItems by viewModel.checklistItems.collectAsStateWithLifecycle(emptyList())

    HomeScreenImpl(
        modifier = modifier,
        notes = notes,
        snackbarHostState = snackbarHostState,
        checklistItems = checklistItems,
        selectedNoteIds = selectedNoteIds,
        onAddTextNoteClick = onAddTextNoteClick,
        onAddChecklistClick = onAddChecklistClick,
        onCardClick = onCardClick,
        onEndSelectModeClick = viewModel.deselectAllNotes,
        onDeleteNotesClick = { viewModel.deleteNotes(it) },
        onSelectNote = viewModel.selectNote,
        onDeselectNote = viewModel.deselectNote,
        onSettingsClick = onSettingsClick,
        onDebugClick = onDebugClick,
    )
}


fun getPreviewNotes(): List<Note> {
    return listOf(
        Note(title = "textnote", text = "text of textnote", type = Enums.NoteType.TEXT),
        Note(title = "checklistnote with long fucking title", type = Enums.NoteType.CHECKLIST),
        Note(title = "textnote 2", text = "text of textnote 2", type = Enums.NoteType.TEXT),
        Note(title = "checklistnote 2", type = Enums.NoteType.CHECKLIST),
        Note(type = Enums.NoteType.CHECKLIST),
        Note(type = Enums.NoteType.CHECKLIST),
        Note(title = "empty checklistnote", type = Enums.NoteType.CHECKLIST),
        Note(title = "empty textnote", type = Enums.NoteType.TEXT),
        Note(type = Enums.NoteType.TEXT),
    )
}

fun getPreviewChecklistItems(notes: List<Note>): List<ChecklistItem> {
    return listOf(
        ChecklistItem(text = "item 1", noteId = notes[1].id),
        ChecklistItem(text = "item 2", noteId = notes[1].id, checked = true),
        ChecklistItem(text = "item 3", noteId = notes[1].id),
        ChecklistItem(text = "item 4", noteId = notes[1].id),
        ChecklistItem(text = "item 5", noteId = notes[1].id),
        ChecklistItem(text = "item 6", noteId = notes[1].id),
        ChecklistItem(text = "item 7", noteId = notes[1].id),
        ChecklistItem(text = "item 8", noteId = notes[3].id),
        ChecklistItem(text = "item 9", noteId = notes[3].id, checked = true),
        ChecklistItem(text = "item 10", noteId = notes[4].id),
    )
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 395)
@Composable
fun HomeScreenPreview() {
    val notes = getPreviewNotes()
    val checklistItems = getPreviewChecklistItems(notes)
    val snackbarHostState = remember { SnackbarHostState() }

    RetainTheme {
        HomeScreenImpl(
            notes = notes,
            checklistItems = checklistItems,
            selectedNoteIds = emptyList(),
            snackbarHostState = snackbarHostState,
        )
    }
}

@Preview(showSystemUi = true, showBackground = true, uiMode = UI_MODE_NIGHT_YES, widthDp = 395)
@Composable
fun HomeScreenPreviewDark() {
    val notes = getPreviewNotes()
    val checklistItems = getPreviewChecklistItems(notes)
    val snackbarHostState = remember { SnackbarHostState() }

    RetainTheme {
        HomeScreenImpl(
            notes = notes,
            checklistItems = checklistItems,
            selectedNoteIds = emptyList(),
            snackbarHostState = snackbarHostState,
        )
    }
}
