package com.troikoss.continuum_explorer.ui.activities

import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.troikoss.continuum_explorer.ui.components.HorizontalScrollbar
import com.troikoss.continuum_explorer.ui.components.VerticalScrollbar
import androidx.compose.ui.unit.sp
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme
import com.troikoss.continuum_explorer.utils.RestrictedCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class TextEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri == null) {
            Toast.makeText(this, "No file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            FileExplorerTheme {
                TextEditorScreen(uri, onExit = { finish() })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TextEditorScreen(uri: Uri, onExit: () -> Unit) {
        var textState by remember { mutableStateOf(TextFieldValue("")) }
        var originalText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }
        var isSaving by remember { mutableStateOf(false) }
        var isSearchVisible by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        val text = textState.text

        val originalPath = remember { intent.getStringExtra("originalPath") }
        val tempPath = remember { intent.getStringExtra("tempPath") }

        val fileName = remember(uri) {
            uri.lastPathSegment ?: "Unknown File"
        }

        // Load file content
        LaunchedEffect(uri) {
            withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            val content = reader.readText()
                            textState = TextFieldValue(content)
                            originalText = content
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TextEditorActivity, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isLoading = false
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(fileName, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (tempPath != null && text == originalText) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        File(tempPath).delete()
                                    } catch (_: Exception) {}
                                }
                            }
                            onExit()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        // Print Button
                        IconButton(onClick = { printText(text, fileName) }) {
                            Icon(Icons.Default.Print, contentDescription = "Print")
                        }
                        // Share Button
                        IconButton(onClick = { shareText(text, fileName) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        // Save Button / Progress
                        if (isSaving) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    isSaving = true
                                    val success = saveFile(uri, text, originalPath, tempPath)
                                    if (success) {
                                        originalText = text
                                        Toast.makeText(this@TextEditorActivity, "File saved", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@TextEditorActivity, "Failed to save file", Toast.LENGTH_SHORT).show()
                                    }
                                    isSaving = false
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .imePadding()
                    .fillMaxSize()
            ) {
                // Search Bar Header
                if (isSearchVisible) {
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search text...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = {
                                isSearchVisible = false
                                searchQuery = ""
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search")
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        val horizontalScrollState = rememberScrollState()
                        val verticalScrollState = rememberScrollState()
                        val primaryColor = MaterialTheme.colorScheme.primary

                        // Latest layout result from the text field itself.
                        var textLayoutResult by remember {
                            mutableStateOf<TextLayoutResult?>(null)
                        }

                        val currentLine = remember(textState) {
                            val offset = textState.selection.start.coerceIn(0, text.length)
                            text.take(offset).count { it == '\n' } + 1
                        }

                        val editorStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )

                        val searchTransformation = remember(searchQuery) {
                            VisualTransformation { input ->
                                if (searchQuery.isBlank()) {
                                    TransformedText(input, OffsetMapping.Identity)
                                } else {
                                    val annotatedString = buildAnnotatedString {
                                        val fullText = input.text
                                        var startIndex = 0
                                        var index = fullText.indexOf(searchQuery, startIndex, ignoreCase = true)
                                        while (index >= 0) {
                                            append(fullText.substring(startIndex, index))
                                            withStyle(SpanStyle(background = Color.Yellow.copy(alpha = 0.5f), color = Color.Unspecified)) {
                                                append(fullText.substring(index, index + searchQuery.length))
                                            }
                                            startIndex = index + searchQuery.length
                                            index = fullText.indexOf(searchQuery, startIndex, ignoreCase = true)
                                        }
                                        if (startIndex < fullText.length) {
                                            append(fullText.substring(startIndex))
                                        }
                                    }
                                    TransformedText(annotatedString, OffsetMapping.Identity)
                                }
                            }
                        }

                        val density = LocalDensity.current
                        val edgeThresholdPx = with(density) { 48.dp.toPx() }
                        var contentBoxSize by remember { mutableStateOf(IntSize.Zero) }
                        var contentLocalMousePos by remember { mutableStateOf<Offset?>(null) }
                        val showVertical by remember { derivedStateOf {
                            val pos = contentLocalMousePos ?: return@derivedStateOf false
                            pos.x > contentBoxSize.width - edgeThresholdPx
                        } }
                        val showHorizontal by remember { derivedStateOf {
                            val pos = contentLocalMousePos ?: return@derivedStateOf false
                            pos.y > contentBoxSize.height - edgeThresholdPx
                        } }

                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { contentBoxSize = it }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            when (event.type) {
                                                PointerEventType.Move, PointerEventType.Enter ->
                                                    contentLocalMousePos = event.changes.firstOrNull()?.position
                                                PointerEventType.Exit -> contentLocalMousePos = null
                                                else -> {}
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(verticalScrollState)
                                        .padding(1.dp)
                                        .padding(end = 1.dp)
                                ) {
                                    // Line numbers gutter.
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .then(
                                                textLayoutResult?.let { layout ->
                                                    Modifier.height(with(density) { layout.size.height.toDp() })
                                                } ?: Modifier.height(0.dp)
                                            )
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val layout = textLayoutResult ?: return@Canvas
                                            val canvas = drawContext.canvas.nativeCanvas

                                            val paint = Paint().apply {
                                                isAntiAlias = true
                                                textSize = with(density) { 15.sp.toPx() }
                                                textAlign = Paint.Align.RIGHT
                                                color = android.graphics.Color.GRAY
                                            }

                                            val linePaint = Paint().apply {
                                                isAntiAlias = true
                                                color = android.graphics.Color.parseColor("#44888888")
                                                strokeWidth = with(density) { 1.dp.toPx() }
                                            }

                                            val activePaint = Paint().apply {
                                                isAntiAlias = true
                                                color = android.graphics.Color.argb(
                                                    64, // ~25% opacity
                                                    (primaryColor.red * 255).toInt(),
                                                    (primaryColor.green * 255).toInt(),
                                                    (primaryColor.blue * 255).toInt()
                                                )
                                                style = Paint.Style.FILL
                                            }

                                            val rightEdgePx = size.width - with(density) { 12.dp.toPx() }

                                            // Draw continuous vertical dividing line between numbers and text rows
                                            canvas.drawLine(size.width - 2f, 0f, size.width - 2f, size.height, linePaint)

                                            for (line in 0 until layout.lineCount) {
                                                val lineStart = layout.getLineStart(line)
                                                val isNewLogicalLine = line == 0 ||
                                                        text.getOrNull(lineStart - 1) == '\n'
                                                if (!isNewLogicalLine) continue

                                                val lineNumber =
                                                    text.take(lineStart).count { it == '\n' } + 1

                                                val top = layout.getLineTop(line)
                                                val bottom = layout.getLineBottom(line)
                                                val baseline = (top + bottom) / 2f -
                                                        (paint.descent() + paint.ascent()) / 2f + 2f // Shifted down 2px for exact row alignment

                                                // Highlight selected active line number with translucent primary box
                                                if (lineNumber == currentLine) {
                                                    val boxTop = top + 2f + 2f
                                                    val boxBottom = bottom - 2f + 2f
                                                    val boxLeft = -10f
                                                    val boxRight = size.width - with(density) { 12.dp.toPx() } + 15f
                                                    val rect = RectF(boxLeft, boxTop, boxRight, boxBottom)
                                                    canvas.drawRoundRect(rect, 6f, 6f, activePaint)
                                                }

                                                canvas.drawText(
                                                    lineNumber.toString(),
                                                    rightEdgePx,
                                                    baseline,
                                                    paint
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Horizontally scrollable text editor area.
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(horizontalScrollState)
                                    ) {
                                        BasicTextField(
                                            value = textState,
                                            onValueChange = { textState = it },
                                            modifier = Modifier
                                                .width(IntrinsicSize.Max)
                                                .height(IntrinsicSize.Min),
                                            textStyle = editorStyle,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            visualTransformation = searchTransformation,
                                            onTextLayout = { textLayoutResult = it }
                                        )
                                    }
                                }

                                VerticalScrollbar(
                                    scrollState = verticalScrollState,
                                    isNearEdge = showVertical,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .fillMaxHeight()
                                        .padding(top = 16.dp, bottom = 16.dp, end = 4.dp)
                                )

                                HorizontalScrollbar(
                                    scrollState = horizontalScrollState,
                                    isNearEdge = showHorizontal,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(start = 62.dp, end = 16.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun printText(content: String, fileName: String) {
        val printManager = getSystemService(PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val jobName = "$fileName Document"
            printManager.print(
                jobName,
                object : PrintDocumentAdapter() {
                    override fun onWrite(
                        pages: Array<out PageRange>,
                        destination: ParcelFileDescriptor,
                        cancellationSignal: CancellationSignal,
                        callback: WriteResultCallback
                    ) {
                        try {
                            FileOutputStream(destination.fileDescriptor).use { out ->
                                out.write(content.toByteArray(Charsets.UTF_8))
                            }
                            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback.onWriteFailed(e.message)
                        }
                    }

                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal,
                        callback: LayoutResultCallback,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal.isCanceled) {
                            callback.onLayoutCancelled()
                            return
                        }
                        val pinfo = PrintDocumentInfo.Builder(jobName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback.onLayoutFinished(pinfo, true)
                    }
                },
                null
            )
        } else {
            Toast.makeText(this, "Printing not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareText(content: String, fileName: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_TITLE, fileName)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private suspend fun saveFile(uri: Uri, content: String, originalPath: String? = null, tempPath: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val success = contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(content)
                        true
                    }
                } ?: false

                if (success && originalPath != null && tempPath != null) {
                    RestrictedCache.pushBack(this@TextEditorActivity, File(tempPath), originalPath)
                } else {
                    success
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
