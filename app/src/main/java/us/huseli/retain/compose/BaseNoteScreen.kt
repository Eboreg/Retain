package us.huseli.retain.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import us.huseli.retain.R
import us.huseli.retain.data.entities.Image
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.BaseEditNoteViewModel


@Composable
fun BaseNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: BaseEditNoteViewModel,
    snackbarHostState: SnackbarHostState,
    carouselImageId: String? = null,
    reorderableState: ReorderableLazyListState? = null,
    onTitleFieldNext: (() -> Unit)?,
    onBackClick: () -> Unit,
    onImageClick: (Image) -> Unit,
    onBackgroundClick: (() -> Unit)? = null,
    content: LazyListScope.(Color) -> Unit,
) {
    val title by viewModel.title.collectAsStateWithLifecycle("")
    val colorIdx by viewModel.colorIdx.collectAsStateWithLifecycle(0)
    val noteColor = getNoteColor(colorIdx)
    val interactionSource = remember { MutableInteractionSource() }

    val bitmapImages by viewModel.bitmapImages.collectAsStateWithLifecycle(emptyList())
    var currentCarouselImageId by remember { mutableStateOf(carouselImageId) }
    val currentCarouselImage = bitmapImages.find { it.image.filename == currentCarouselImageId }
    val currentCarouselImageIdx = bitmapImages.indexOf(currentCarouselImage)

    Scaffold(
        topBar = {
            NoteScreenTopAppBar(
                onBackClick = onBackClick,
                onImagePick = { uri -> viewModel.insertImage(uri) },
                onColorSelected = { index -> viewModel.setColorIdx(index) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        var columnModifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(noteColor)
            .clickable(interactionSource = interactionSource, indication = null) {
                onBackgroundClick?.invoke()
            }
        if (reorderableState != null) columnModifier = columnModifier.reorderable(reorderableState)

        LazyColumn(
            state = reorderableState?.listState ?: rememberLazyListState(),
            modifier = columnModifier,
        ) {
            item {
                NoteImageGrid(
                    bitmapImages = bitmapImages,
                    showDeleteButton = true,
                    onImageClick = { onImageClick(it.image) },
                    onDeleteButtonClick = { viewModel.deleteImage(it.image) },
                    secondaryRowHeight = 200.dp,
                )
            }
            item {
                NoteTitleField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = {
                        viewModel.setTitle(it)
                    },
                    onNext = onTitleFieldNext,
                )
                Spacer(Modifier.height(4.dp))
            }
            content(noteColor)
        }

        currentCarouselImage?.let {
            ImageCarousel(
                bitmapImage = it,
                multiple = bitmapImages.size > 1,
                onClose = onBackClick,
                onSwipeLeft = {
                    currentCarouselImageId =
                        if (currentCarouselImage != bitmapImages.last()) bitmapImages[currentCarouselImageIdx + 1].image.filename
                        else bitmapImages[0].image.filename
                },
                onSwipeRight = {
                    currentCarouselImageId =
                        if (currentCarouselImageIdx > 0) bitmapImages[currentCarouselImageIdx - 1].image.filename
                        else bitmapImages.last().image.filename
                },
            )
        }
    }
}


@Composable
fun NoteTitleField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onNext: (() -> Unit)? = null,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { onValueChange(it) },
        textStyle = MaterialTheme.typography.headlineSmall,
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Next,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        keyboardActions = KeyboardActions(
            onNext = onNext?.let { { onNext() } }
        ),
        placeholder = {
            Text(
                text = stringResource(R.string.title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        },
        singleLine = true,
        colors = outlinedTextFieldColors(),
    )
}
