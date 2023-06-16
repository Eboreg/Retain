package us.huseli.retain.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.Enums.HomeScreenViewType
import us.huseli.retain.R
import us.huseli.retain.data.entities.Note
import us.huseli.retain.viewmodels.NoteViewModel
import us.huseli.retain.viewmodels.SettingsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
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
    val syncBackend by viewModel.syncBackend.collectAsStateWithLifecycle()
    val isSyncBackendRefreshing by viewModel.isSyncBackendRefreshing.collectAsStateWithLifecycle(false)
    val isSyncBackendEnabled by settingsViewModel.isSyncBackendEnabled.collectAsStateWithLifecycle(false)
    val notes by viewModel.notes.collectAsStateWithLifecycle(emptyList())
    val images by viewModel.images.collectAsStateWithLifecycle(emptyList())
    val checklistData by viewModel.checklistData.collectAsStateWithLifecycle(emptyList())
    val isSelectEnabled by viewModel.isSelectEnabled.collectAsStateWithLifecycle(false)
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsStateWithLifecycle()
    val minColumnWidth by settingsViewModel.minColumnWidth.collectAsStateWithLifecycle()
    val showArchive by viewModel.showArchive.collectAsStateWithLifecycle()
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> viewModel.switchNotePositions(from, to) },
        onDragEnd = { _, _ -> viewModel.saveNotePositions() }
    )
    var isFABExpanded by rememberSaveable { mutableStateOf(false) }
    var viewType by rememberSaveable { mutableStateOf(HomeScreenViewType.GRID) }
    var lazyModifier = modifier
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            isFABExpanded = false
        }
        .fillMaxHeight()

    if (isSyncBackendEnabled) {
        val refreshState = rememberPullRefreshState(
            refreshing = isSyncBackendRefreshing,
            onRefresh = { viewModel.syncBackend() },
        )
        lazyModifier = lazyModifier.pullRefresh(state = refreshState)
    }

    BackHandler(isSelectEnabled) {
        viewModel.deselectAllNotes()
    }

    val lazyContent: @Composable (Note, Boolean) -> Unit = { note, isDragging ->
        NoteCard(
            modifier = Modifier.fillMaxWidth(),
            note = note,
            checklistData = checklistData.find { it.noteId == note.id },
            images = images.filter { it.noteId == note.id },
            isDragging = isDragging,
            onClick = {
                if (isSelectEnabled) viewModel.toggleNoteSelected(note.id)
                else onCardClick(note)
                isFABExpanded = false
            },
            onLongClick = {
                viewModel.toggleNoteSelected(note.id)
                isFABExpanded = false
            },
            isSelected = selectedNoteIds.contains(note.id),
            showDragHandle = viewType == HomeScreenViewType.LIST,
            reorderableState = reorderableState,
        )
    }

    RetainScaffold(
        viewModel = viewModel,
        settingsViewModel = settingsViewModel,
        topBar = {
            if (isSelectEnabled) SelectionTopAppBar(
                selectedCount = selectedNoteIds.size,
                onCloseClick = { viewModel.deselectAllNotes() },
                onTrashClick = { viewModel.trashSelectedNotes() },
                onSelectAllClick = { viewModel.selectAllNotes() },
                onArchiveClick = {
                    if (showArchive) viewModel.unarchiveSelectedNotes()
                    else viewModel.archiveSelectedNotes()
                },
                showArchive = showArchive,
            ) else HomeScreenTopAppBar(
                viewType = viewType,
                onSettingsClick = onSettingsClick,
                onDebugClick = onDebugClick,
                onViewTypeClick = { viewType = it },
                onArchiveClick = { viewModel.toggleShowArchive() },
                showArchive = showArchive,
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

        Column(modifier = lazyModifier.padding(innerPadding).fillMaxWidth()) {
            if (isSyncBackendRefreshing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.syncing_with,
                            syncBackend?.displayName ?: stringResource(R.string.backend)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    EllipsisProgressIndicator(style = MaterialTheme.typography.bodySmall, width = 10.dp)
                }
            }

            if (viewType == HomeScreenViewType.GRID) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = minColumnWidth.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    items(notes, key = { it.id }) { note -> lazyContent(note, false) }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.reorderable(reorderableState).detectReorder(reorderableState),
                    state = reorderableState.listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notes, key = { it.id }) { note ->
                        ReorderableItem(reorderableState, key = note.id) { isDragging ->
                            lazyContent(note, isDragging)
                        }
                    }
                }
            }
        }
    }
}
