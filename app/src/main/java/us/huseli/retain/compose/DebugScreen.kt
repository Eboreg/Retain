package us.huseli.retain.compose

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.huseli.retain.Logger
import us.huseli.retain.R
import us.huseli.retain.logLevelToString
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun LogLevelDropdownMenuItem(
    logLevel: Int,
    onClick: (Int) -> Unit
) {
    DropdownMenuItem(
        onClick = { onClick(logLevel) },
        text = { Text(logLevelToString(logLevel)) }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugTopAppBar(
    modifier: Modifier = Modifier,
    logLevel: Int,
    onClose: () -> Unit,
    onLogLevelChange: (Int) -> Unit,
) {
    var dropDownExpanded by remember { mutableStateOf(false) }
    val onDropdownItemClick = { level: Int ->
        dropDownExpanded = false
        onLogLevelChange(level)
    }

    TopAppBar(
        title = { },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Sharp.ArrowBack,
                    contentDescription = stringResource(R.string.close)
                )
            }
        },
        actions = {
            OutlinedButton(onClick = { dropDownExpanded = true }) {
                Text("Level: ${logLevelToString(logLevel)}")
            }
            DropdownMenu(
                expanded = dropDownExpanded,
                onDismissRequest = { dropDownExpanded = false }
            ) {
                LogLevelDropdownMenuItem(Log.DEBUG, onDropdownItemClick)
                LogLevelDropdownMenuItem(Log.INFO, onDropdownItemClick)
                LogLevelDropdownMenuItem(Log.WARN, onDropdownItemClick)
                LogLevelDropdownMenuItem(Log.ERROR, onDropdownItemClick)
            }
        }
    )
}


@Composable
fun DebugScreen(modifier: Modifier = Modifier, logger: Logger, onClose: () -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
    var logLevel by rememberSaveable { mutableStateOf(Log.INFO) }
    val listState = rememberLazyListState()
    val logMessages = remember {
        mutableStateListOf(
            *logger.logMessages.replayCache.filterNotNull().toTypedArray()
        )
    }

    logger.logMessages.collectAsStateWithLifecycle(null).value?.let {
        if (!logMessages.contains(it)) logMessages.add(it)
    }

    LaunchedEffect(logMessages.filter { it.level >= logLevel }.size) {
        listState.animateScrollToItem(
            max(logMessages.filter { it.level >= logLevel }.size - 1, 0)
        )
    }

    RetainScaffold(
        topBar = {
            DebugTopAppBar(
                logLevel = logLevel,
                onClose = onClose,
                onLogLevelChange = { logLevel = it },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier.padding(innerPadding).padding(horizontal = 8.dp),
            state = listState,
        ) {
            items(logMessages) { logMessage ->
                if (logMessage.level >= logLevel) {
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Column {
                            Text(
                                text = "${timeFormatter.format(logMessage.timestamp)} ${logMessage.levelToString()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                color = Color.Gray,
                            )
                            Text(
                                text = logMessage.tag,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                color = Color.Gray,
                            )
                            Text(
                                text = logMessage.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                color = when (logMessage.level) {
                                    Log.DEBUG -> Color.DarkGray
                                    Log.WARN -> Color.Yellow
                                    Log.ERROR -> Color.Red
                                    else -> Color.LightGray
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
