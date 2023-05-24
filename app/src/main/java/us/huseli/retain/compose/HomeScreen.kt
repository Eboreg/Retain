package us.huseli.retain.compose

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.Enums.HomeScreenViewType
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
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
    val combos by viewModel.combos.collectAsStateWithLifecycle()
    val selectedNotes by viewModel.selectedNotes.collectAsStateWithLifecycle(emptySet())
    val bitmapImages by viewModel.bitmapImages.collectAsStateWithLifecycle(emptyList())

    HomeScreenImpl(
        modifier = modifier,
        combos = combos,
        bitmapImages = bitmapImages,
        selectedCombos = selectedNotes,
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
    combos: List<NoteCombo>,
    bitmapImages: List<BitmapImage>,
    selectedCombos: Collection<NoteCombo>,
    onAddTextNoteClick: () -> Unit = {},
    onAddChecklistClick: () -> Unit = {},
    onCardClick: (Note) -> Unit = {},
    onEndSelectModeClick: () -> Unit = {},
    onTrashNotesClick: (Collection<NoteCombo>) -> Unit = {},
    onSelectNote: (NoteCombo) -> Unit = {},
    onDeselectNote: (NoteCombo) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
    onSwitchPositions: (ItemPosition, ItemPosition) -> Unit = { _, _ -> },
    onReordered: () -> Unit = {},
) {
    val isSelectEnabled = selectedCombos.isNotEmpty()
    var isFABExpanded by rememberSaveable { mutableStateOf(false) }
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
                selectedCount = selectedCombos.size,
                onCloseClick = onEndSelectModeClick,
                onTrashClick = { onTrashNotesClick(selectedCombos) },
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

        val lazyModifier = modifier
            .clickable(interactionSource = interactionSource, indication = null) { isFABExpanded = false }
            .fillMaxHeight()
            .padding(innerPadding)

        val lazyContent: @Composable (NoteCombo, Modifier) -> Unit = { combo, modifier ->
            NoteCard(
                modifier = modifier.fillMaxWidth(),
                combo = combo,
                bitmapImages = bitmapImages.filter { it.image.noteId == combo.note.id },
                onClick = {
                    if (isSelectEnabled) {
                        if (selectedCombos.contains(combo)) onDeselectNote(combo)
                        else onSelectNote(combo)
                    } else onCardClick(combo.note)
                },
                onLongClick = {
                    if (!isSelectEnabled) onSelectNote(combo)
                    else onDeselectNote(combo)
                },
                isSelected = selectedCombos.contains(combo),
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
                items(combos, key = { it.note.id }) { combo -> lazyContent(combo, Modifier) }
            }
        } else {
            LazyColumn(
                modifier = lazyModifier.reorderable(reorderableState).detectReorder(reorderableState),
                state = reorderableState.listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(combos, key = { it.note.id }) { combo ->
                    ReorderableItem(reorderableState, key = combo.note.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp)
                        lazyContent(combo, Modifier.shadow(elevation))
                    }
                }
            }
        }
    }
}
