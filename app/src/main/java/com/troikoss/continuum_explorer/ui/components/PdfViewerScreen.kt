package com.troikoss.continuum_explorer.ui.components

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.view.KeyEvent
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import java.io.FileOutputStream

@OptIn(ExperimentalPdfApi::class)
class CustomPdfViewerFragment : PdfViewerFragment() {
    private var internalPdfView: PdfView? = null
    var onScrollStateChanged: ((canScrollUp: Boolean, canScrollDown: Boolean) -> Unit)? = null

    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        internalPdfView = pdfView
        try {
            pdfView.minZoom = 0.25f
            pdfView.addOnFirstContentLoadListener {
                pdfView.zoom = 1.5f // 50% zoomed out upon initial load
                updateScrollState(pdfView)
            }
            pdfView.setOnScrollChangeListener { _, _, _, _, _ ->
                updateScrollState(pdfView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateScrollState(pdfView: PdfView) {
        val canUp = pdfView.canScrollVertically(-1)
        val canDown = pdfView.canScrollVertically(1)
        onScrollStateChanged?.invoke(canUp, canDown)
    }

    fun zoomIn() {
        internalPdfView?.let { v ->
            v.zoom = (v.zoom + 0.25f).coerceIn(v.minZoom, v.maxZoom)
        }
    }

    fun zoomOut() {
        internalPdfView?.let { v ->
            v.zoom = (v.zoom - 0.25f).coerceIn(v.minZoom, v.maxZoom)
        }
    }

    fun resetZoom() {
        internalPdfView?.let { v ->
            v.zoom = 1.0f
        }
    }

    fun scrollDown(amount: Int = 200) {
        internalPdfView?.scrollBy(0, amount)
    }

    fun scrollUp(amount: Int = 200) {
        internalPdfView?.scrollBy(0, -amount)
    }

    fun pageDown() {
        internalPdfView?.let { v ->
            v.scrollBy(0, v.height / 2)
        }
    }

    fun pageUp() {
        internalPdfView?.let { v ->
            v.scrollBy(0, -v.height / 2)
        }
    }
}

@Composable
fun PdfViewerScreen(
    uri: Uri,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current
    val documentName = remember(uri) { uri.lastPathSegment ?: "PDF Viewer" }

    var pdfViewerFragment by remember { mutableStateOf<CustomPdfViewerFragment?>(null) }
    val focusRequester = remember { FocusRequester() }

    var canScrollUp by remember { mutableStateOf(false) }
    var canScrollDown by remember { mutableStateOf(true) }

    val configuration = LocalConfiguration.current
    val isPhoneOrTablet = configuration.smallestScreenWidthDp < 840
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val topControlsPadding = if (isPhoneOrTablet) {
        statusBarHeight + 1.dp
    } else { 36.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(extendedColors.fileViewBackground)
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    if (event.isCtrlPressed) {
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_PLUS -> {
                                pdfViewerFragment?.zoomIn()
                                true
                            }
                            KeyEvent.KEYCODE_MINUS -> {
                                pdfViewerFragment?.zoomOut()
                                true
                            }
                            KeyEvent.KEYCODE_0 -> {
                                pdfViewerFragment?.resetZoom()
                                true
                            }
                            else -> false
                        }
                    } else {
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_SPACE -> {
                                pdfViewerFragment?.scrollDown()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                pdfViewerFragment?.scrollUp()
                                true
                            }
                            KeyEvent.KEYCODE_PAGE_DOWN -> {
                                pdfViewerFragment?.pageDown()
                                true
                            }
                            KeyEvent.KEYCODE_PAGE_UP -> {
                                pdfViewerFragment?.pageUp()
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta
                        if (scrollDelta != null) {
                            if (event.keyboardModifiers.isCtrlPressed) {
                                if (scrollDelta.y < 0) {
                                    pdfViewerFragment?.zoomIn()
                                } else if (scrollDelta.y > 0) {
                                    pdfViewerFragment?.zoomOut()
                                }
                                event.changes.forEach { it.consume() }
                            } else {
                                // Normal mouse wheel / touchpad scroll
                                val delta = (scrollDelta.y * 35).toInt()
                                pdfViewerFragment?.scrollDown(delta)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = View.generateViewId()
                    val fragmentActivity = ctx as? FragmentActivity
                    val fragmentManager = fragmentActivity?.supportFragmentManager
                    if (fragmentManager != null) {
                        val fragment = CustomPdfViewerFragment().apply {
                            onScrollStateChanged = { up, down ->
                                canScrollUp = up
                                canScrollDown = down
                            }
                        }
                        pdfViewerFragment = fragment
                        fragmentManager.beginTransaction()
                            .replace(id, fragment, "pdf_viewer_fragment")
                            .commit()

                        fragmentManager.registerFragmentLifecycleCallbacks(
                            object : FragmentManager.FragmentLifecycleCallbacks() {
                                override fun onFragmentStarted(
                                    fm: FragmentManager,
                                    f: Fragment
                                ) {
                                    if (f === fragment) {
                                        try {
                                            fragment.documentUri = uri
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        fm.unregisterFragmentLifecycleCallbacks(this)
                                    }
                                }
                            },
                            false
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Fading Transparency Scrim with 300ms fade animation
        AnimatedVisibility(
            visible = canScrollUp,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 16.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(extendedColors.fileViewBackground, Color.Transparent)
                        )
                    )
            )
        }

        // Bottom Fading Transparency Scrim with 300ms fade animation
        AnimatedVisibility(
            visible = isPhoneOrTablet && canScrollDown,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight + 16.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, extendedColors.fileViewBackground)
                        )
                    )
            )
        }

        // Floating Controls Overlay (Top Left Expandable App Logo FAB + Action Column + Pill shaped Bar)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topControlsPadding, start = 16.dp, end = 16.dp)
        ) {
            PdfViewerTopControls(
                documentName = documentName,
                containerColor = extendedColors.menuBackground,
                contentColor = extendedColors.textColor,
                onBackClick = onBackClick,
                onSearchClick = {
                    pdfViewerFragment?.let { fragment ->
                        fragment.isTextSearchActive = !fragment.isTextSearchActive
                    }
                },
                onPrintClick = {
                    printPdfDocument(context, uri, documentName)
                },
                onShareClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun PdfViewerTopControls(
    documentName: String,
    containerColor: Color,
    contentColor: Color,
    onBackClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onPrintClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(true) }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expandable Top-Left Master FAB + Action Buttons Column
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Master App Logo FAB
            FloatingActionButton(
                onClick = { isExpanded = !isExpanded },
                containerColor = containerColor,
                contentColor = contentColor,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lapp_logo),
                    contentDescription = "App Menu",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Collapsible Action Buttons Column
            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn() + expandVertically(),
                exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Back FAB
                    FloatingActionButton(
                        onClick = onBackClick,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Search FAB
                    FloatingActionButton(
                        onClick = onSearchClick,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Print FAB
                    FloatingActionButton(
                        onClick = onPrintClick,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share FAB
                    FloatingActionButton(
                        onClick = onShareClick,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Pill-shaped Title Bar next to Master App Logo FAB
        Surface(
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier.height(44.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = documentName,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun printPdfDocument(context: Context, uri: Uri, fileName: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(fileName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.localizedMessage)
                }
            }
        }
        printManager.print(fileName, printAdapter, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Preview(showBackground = true, name = "Light Top Controls")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Top Controls")
@Composable
fun PdfViewerTopControlsPreview() {
    FileExplorerTheme {
        val extendedColors = LocalExtendedColors.current
        PdfViewerTopControls(
            documentName = "Sample Document.pdf",
            containerColor = extendedColors.menuBackground,
            contentColor = extendedColors.textColor
        )
    }
}
