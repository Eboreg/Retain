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
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.Enums.HomeScreenViewType
import us.huseli.retain.data.entities.Note
import us.huseli.retain.data.entities.NoteCombo
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onAddTextNoteClick: () -> Unit,
    onAddChecklistClick: () -> Unit,
    onCardClick: (Note) -> Unit,
    onSettingsClick: () -> Unit,
    onDebugClick: () -> Unit,
) {
    val combos by viewModel.combos.collectAsStateWithLifecycle()
    val selectedCombos by viewModel.selectedNotes.collectAsStateWithLifecycle(emptySet())
    val isSelectEnabled = selectedCombos.isNotEmpty()
    var isFABExpanded by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val minColumnWidth by settingsViewModel.minColumnWidth.collectAsStateWithLifecycle()
    var viewType by rememberSaveable { mutableStateOf(HomeScreenViewType.GRID) }
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.switchNotePositions(from, to) },
        onDragEnd = { _, _ -> viewModel.saveNotePositions() }
    )

    RetainScaffold(
        topBar = {
            if (isSelectEnabled) SelectionTopAppBar(
                selectedCount = selectedCombos.size,
                onCloseClick = { viewModel.deselectAllNotes() },
                onTrashClick = { viewModel.trashNotes(selectedCombos) },
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
            val bitmapImages by viewModel.getNoteBitmapImages(combo.note.id).collectAsStateWithLifecycle(emptyList())

            NoteCard(
                modifier = modifier.fillMaxWidth(),
                combo = combo,
                bitmapImages = bitmapImages,
                onClick = {
                    if (isSelectEnabled) {
                        if (selectedCombos.contains(combo)) viewModel.deselectNote(combo)
                        else viewModel.selectNote(combo)
                    } else onCardClick(combo.note)
                },
                onLongClick = {
                    if (!isSelectEnabled) viewModel.selectNote(combo)
                    else viewModel.deselectNote(combo)
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
