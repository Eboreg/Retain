package us.huseli.retain.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.Enums
import us.huseli.retain.Enums.HomeScreenViewType
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.ChecklistItem
import us.huseli.retain.data.entities.Note
import us.huseli.retain.ui.theme.RetainTheme
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel


@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = hiltViewModel(),
    onAddTextNoteClick: () -> Unit,
    onAddChecklistClick: () -> Unit,
    onCardClick: (Note) -> Unit,
    onSettingsClick: () -> Unit,
    onDebugClick: () -> Unit,
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle(emptyList())
    val selectedNotes by viewModel.selectedNotes.collectAsStateWithLifecycle(emptySet())
    val checklistItems by viewModel.checklistItems.collectAsStateWithLifecycle(emptyList())
    val bitmapImages by viewModel.bitmapImages.collectAsStateWithLifecycle(emptyList())

    HomeScreenImpl(
        modifier = modifier,
        notes = notes,
        checklistItems = checklistItems,
        bitmapImages = bitmapImages,
        selectedNotes = selectedNotes,
        onAddTextNoteClick = onAddTextNoteClick,
        onAddChecklistClick = onAddChecklistClick,
        onCardClick = onCardClick,
        onEndSelectModeClick = { viewModel.deselectAllNotes() },
        onTrashNotesClick = { viewModel.trashNotes(it) },
        onSelectNote = { viewModel.selectNote(it) },
        onDeselectNote = { viewModel.deselectNote(it) },
        onSettingsClick = onSettingsClick,
        onDebugClick = onDebugClick,
        onSwitchPositions = { from, to -> viewModel.switchNotePositions(from, to) },
        onReordered = { viewModel.saveNotePositions() },
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenImpl(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    notes: List<Note>,
    checklistItems: List<ChecklistItem>,
    bitmapImages: List<BitmapImage>,
    selectedNotes: Collection<Note>,
    onAddTextNoteClick: () -> Unit = {},
    onAddChecklistClick: () -> Unit = {},
    onCardClick: (Note) -> Unit = {},
    onEndSelectModeClick: () -> Unit = {},
    onTrashNotesClick: (Collection<Note>) -> Unit = {},
    onSelectNote: (Note) -> Unit = {},
    onDeselectNote: (Note) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
    onSwitchPositions: (ItemPosition, ItemPosition) -> Unit = { _, _ -> },
    onReordered: () -> Unit = {},
) {
    val isSelectEnabled = selectedNotes.isNotEmpty()
    var isFABExpanded by rememberSaveable { mutableStateOf(false) }
    var trashDialogOpen by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val minColumnWidth by settingsViewModel.minColumnWidth.collectAsStateWithLifecycle()
    var viewType by rememberSaveable { mutableStateOf(HomeScreenViewType.GRID) }
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> onSwitchPositions(from, to) },
        onDragEnd = { _, _ -> onReordered() }
    )

    RetainScaffold(
        topBar = {
            if (isSelectEnabled) SelectionTopAppBar(
                selectedCount = selectedNotes.size,
                onCloseClick = onEndSelectModeClick,
                onTrashClick = { trashDialogOpen = true },
            ) else HomeScreenTopAppBar(
                viewType = viewType,
                onSettingsClick = onSettingsClick,
                onDebugClick = onDebugClick,
                onViewTypeClick = { viewType = it }
            )
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 16.dp, end = 16.dp)
                .zIndex(1f)
        ) {
            FAB(
                expanded = isFABExpanded,
                onAddTextNoteClick = onAddTextNoteClick,
                onAddChecklistClick = onAddChecklistClick,
                onExpandedChange = { isFABExpanded = it },
                onClose = { isFABExpanded = false },
            )
        }

        if (trashDialogOpen) {
            TrashNotesDialog(
                selectedNotes = selectedNotes,
                onTrash = onTrashNotesClick,
                onClose = { trashDialogOpen = false },
            )
        }

        val lazyModifier = modifier
            .clickable(interactionSource = interactionSource, indication = null) { isFABExpanded = false }
            .fillMaxHeight()
            .padding(innerPadding)

        val lazyContent: @Composable (Note, Modifier) -> Unit = { note, modifier ->
            NoteCard(
                modifier = modifier.fillMaxWidth(),
                note = note,
                checklistItems = checklistItems.filter { it.noteId == note.id },
                bitmapImages = bitmapImages.filter { it.image.noteId == note.id },
                onClick = {
                    if (isSelectEnabled) {
                        if (selectedNotes.contains(note)) onDeselectNote(note)
                        else onSelectNote(note)
                    } else onCardClick(note)
                },
                onLongClick = {
                    if (!isSelectEnabled) onSelectNote(note)
                    else onDeselectNote(note)
                },
                isSelected = selectedNotes.contains(note),
                showDragHandle = viewType == HomeScreenViewType.LIST,
                reorderableState = reorderableState,
            )
        }

        if (viewType == HomeScreenViewType.GRID) {
            LazyVerticalStaggeredGrid(
                modifier = lazyModifier,
                columns = StaggeredGridCells.Adaptive(minSize = minColumnWidth.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(notes, key = { it.id }) { note -> lazyContent(note, Modifier) }
            }
        } else {
            LazyColumn(
                modifier = lazyModifier.reorderable(reorderableState).detectReorder(reorderableState),
                state = reorderableState.listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    ReorderableItem(reorderableState, key = note.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)

                        lazyContent(note, Modifier.shadow(elevation))
                    }
                }
            }
        }
    }
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

    RetainTheme {
        HomeScreenImpl(
            notes = notes,
            checklistItems = checklistItems,
            selectedNotes = emptyList(),
            bitmapImages = emptyList(),
        )
    }
}

@Preview(showSystemUi = true, showBackground = true, uiMode = UI_MODE_NIGHT_YES, widthDp = 395)
@Composable
fun HomeScreenPreviewDark() {
    val notes = getPreviewNotes()
    val checklistItems = getPreviewChecklistItems(notes)

    RetainTheme {
        HomeScreenImpl(
            notes = notes,
            checklistItems = checklistItems,
            selectedNotes = emptyList(),
            bitmapImages = emptyList(),
        )
    }
}
