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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.R
import us.huseli.retain.outlinedTextFieldColors
import us.huseli.retain.ui.theme.getNoteColor
import us.huseli.retain.viewmodels.EditNoteViewModel


@Composable
fun BaseNoteScreen(
    modifier: Modifier = Modifier,
    viewModel: EditNoteViewModel,
    snackbarHostState: SnackbarHostState,
    onTitleFieldNext: (() -> Unit)?,
    onClose: () -> Unit,
    onBackgroundClick: (() -> Unit)? = null,
    content: LazyListScope.(Color) -> Unit,
) {
    val title by viewModel.title.collectAsStateWithLifecycle()
    val colorIdx by viewModel.colorIdx.collectAsStateWithLifecycle()
    val noteColor = getNoteColor(colorIdx)
    val context = LocalContext.current
    val bitmapImages by viewModel.bitmapImages.collectAsStateWithLifecycle(emptyList())
    var currentCarouselIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val interactionSource = remember { MutableInteractionSource() }

    Scaffold(
        topBar = {
            NoteScreenTopAppBar(
                onBackClick = onClose,
                onImagePick = { uri -> viewModel.insertImage(uri, context) },
                onColorSelected = { index -> viewModel.setColorIdx(index) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(noteColor)
                .clickable(interactionSource = interactionSource, indication = null) {
                    onBackgroundClick?.invoke()
                },
        ) {
            item {
                NoteImageGrid(
                    bitmapImages = bitmapImages,
                    showDeleteButton = true,
                    onImageClick = { currentCarouselIndex = bitmapImages.indexOf(it) },
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

        currentCarouselIndex?.let {
            ImageCarousel(
                images = bitmapImages,
                startIndex = it,
                onClose = { currentCarouselIndex = null }
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
