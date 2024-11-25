package us.huseli.retain.compose

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import us.huseli.retain.Enums.HomeScreenViewType
import us.huseli.retain.R
import us.huseli.retain.dataclasses.entities.Note
import us.huseli.retain.viewmodels.NoteListViewModel
import us.huseli.retain.viewmodels.SettingsViewModel
import us.huseli.retaintheme.snackbar.SnackbarEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: NoteListViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onAddTextNoteClick: () -> Unit,
    onAddChecklistClick: () -> Unit,
    onCardClick: (Note) -> Unit,
    onSettingsClick: () -> Unit,
    onTestClick: () -> Unit,
) {
    val context = LocalContext.current
    val syncBackend by viewModel.syncBackend.collectAsStateWithLifecycle()
    val isSyncBackendSyncing by viewModel.isSyncBackendSyncing.collectAsStateWithLifecycle(false)
    val isSyncBackendEnabled by settingsViewModel.isSyncBackendEnabled.collectAsStateWithLifecycle(false)
    val pojos by viewModel.pojos.collectAsStateWithLifecycle()
    val isSelectEnabled by viewModel.isSelectEnabled.collectAsStateWithLifecycle(false)
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsStateWithLifecycle()
    val minColumnWidth by settingsViewModel.minColumnWidth.collectAsStateWithLifecycle()
    val showArchive by viewModel.showArchive.collectAsStateWithLifecycle()
    val trashedPojos by viewModel.trashedPojos.collectAsStateWithLifecycle()

    var isFABExpanded by rememberSaveable { mutableStateOf(false) }
    var viewType by rememberSaveable { mutableStateOf(HomeScreenViewType.GRID) }

    BackHandler(isSelectEnabled) {
        viewModel.deselectAllNotes()
    }

    LaunchedEffect(Unit) {
        viewModel.uploadNotes { result ->
            if (!result.success) SnackbarEngine.addError(
                context.getString(R.string.failed_to_upload_notes_to, syncBackend.displayName, result.message)
            )
        }
    }

    LaunchedEffect(trashedPojos) {
        if (trashedPojos.isNotEmpty()) {
            SnackbarEngine.addInfo(
                message = context.resources.getQuantityString(
                    R.plurals.x_notes_trashed,
                    trashedPojos.size,
                    trashedPojos.size,
                ),
                actionLabel = context.resources.getString(R.string.undo).uppercase(),
                onActionPerformed = { viewModel.undoTrashNotes() },
                onDismissed = { viewModel.reallyTrashNotes() },
            )
        }
    }

    RetainScaffold(
        topBar = {
            if (isSelectEnabled) HomeScreenSelectionTopAppBar(
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
                onViewTypeClick = { viewType = it },
                onArchiveClick = { viewModel.toggleShowArchive() },
                showArchive = showArchive,
                onTestClick = onTestClick,
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

        Column(
            modifier = modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    isFABExpanded = false
                }
                .fillMaxHeight()
                .then(
                    if (isSyncBackendEnabled) {
                        val refreshState = rememberPullToRefreshState()

                        Modifier.pullToRefresh(
                            isRefreshing = isSyncBackendSyncing,
                            state = refreshState,
                            onRefresh = { viewModel.syncBackend() },
                        )
                    } else Modifier
                )
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
        ) {
            if (isSyncBackendSyncing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.syncing_with, syncBackend.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    EllipsisProgressIndicator(style = MaterialTheme.typography.bodySmall, width = 10.dp)
                }
            }

            if (viewType == HomeScreenViewType.GRID) {
                val gridState = rememberLazyStaggeredGridState()
                val reorderableState = rememberReorderableLazyStaggeredGridState(
                    lazyStaggeredGridState = gridState,
                    onMove = { from, to -> viewModel.switchNotePositions(from.index, to.index) },
                )

                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(minSize = minColumnWidth.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    contentPadding = PaddingValues(top = 8.dp),
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    items(pojos, key = { it.note.id }) { pojo ->
                        ReorderableItem(state = reorderableState, key = pojo.note.id) { isDragging ->
                            NoteCard(
                                pojo = pojo,
                                isDragging = isDragging,
                                onClick = {
                                    if (isSelectEnabled) viewModel.toggleNoteSelected(pojo.note.id)
                                    else onCardClick(pojo.note)
                                    isFABExpanded = false
                                },
                                onLongClick = {
                                    viewModel.toggleNoteSelected(pojo.note.id)
                                    isFABExpanded = false
                                },
                                isSelected = selectedNoteIds.contains(pojo.note.id),
                                secondaryImageGridRowHeight = if (viewType == HomeScreenViewType.LIST) 200.dp else 100.dp,
                                scope = this,
                                onDragEnd = { viewModel.saveNotePositions() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                val listState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(
                    lazyListState = listState,
                    onMove = { from, to -> viewModel.switchNotePositions(from.index, to.index) },
                )

                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp),
                ) {
                    items(pojos, key = { it.note.id }) { pojo ->
                        ReorderableItem(reorderableState, key = pojo.note.id) { isDragging ->
                            NoteCard(
                                pojo = pojo,
                                isDragging = isDragging,
                                onClick = {
                                    if (isSelectEnabled) viewModel.toggleNoteSelected(pojo.note.id)
                                    else onCardClick(pojo.note)
                                    isFABExpanded = false
                                },
                                onLongClick = {
                                    viewModel.toggleNoteSelected(pojo.note.id)
                                    isFABExpanded = false
                                },
                                isSelected = selectedNoteIds.contains(pojo.note.id),
                                secondaryImageGridRowHeight = if (viewType == HomeScreenViewType.LIST) 200.dp else 100.dp,
                                scope = this,
                                onDragEnd = { viewModel.saveNotePositions() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
