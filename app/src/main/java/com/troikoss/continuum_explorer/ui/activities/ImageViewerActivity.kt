package com.troikoss.continuum_explorer.ui.activities

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import android.content.ClipboardManager
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.utils.AppConfigurations
import com.troikoss.continuum_explorer.utils.getUriForUniversalFile
import coil.compose.AsyncImage
import com.troikoss.continuum_explorer.R
import com.troikoss.continuum_explorer.managers.FileOperationsManager
import com.troikoss.continuum_explorer.managers.OperationType
import com.troikoss.continuum_explorer.utils.GlobalEvents
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme
import com.troikoss.continuum_explorer.ui.theme.LocalExtendedColors
import com.troikoss.continuum_explorer.utils.contextMenuDetector
import com.troikoss.continuum_explorer.utils.deleteFiles
import com.troikoss.continuum_explorer.utils.getSiblingFiles
import com.troikoss.continuum_explorer.utils.renameFile
import com.troikoss.continuum_explorer.utils.toUniversal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.launch
import kotlin.math.abs

class ImageViewerActivity : FullscreenActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri = intent.data?.toString() ?: intent.getStringExtra("IMAGE_URI")

        setContent {
            FileExplorerTheme {
                ImageViewerScreen(
                    initialImageUri = imageUri,
                    onToggleFullscreen = { toggleFullscreen() },
                    isFullscreen = isFullscreen
                )
            }
        }
    }
}

@Composable
fun ImageViewerScreen(
    initialImageUri: String?,
    onToggleFullscreen: () -> Unit,
    isFullscreen: Boolean
) {
    val activity = (LocalView.current.context as? Activity)
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    var currentUri by remember { mutableStateOf<Any?>(initialImageUri) }
    var siblingImages by remember { mutableStateOf<List<Any>>(emptyList()) }

    val pagerState = rememberPagerState(pageCount = { siblingImages.size })

    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null) {
            withContext(Dispatchers.IO) {
                val intent = activity?.intent
                val providerKind = intent?.getStringExtra("PROVIDER_KIND")
                val connectionId = intent?.getStringExtra("CONNECTION_ID")
                val siblingIds = intent?.getStringArrayListExtra("SIBLING_IDS")

                if (providerKind != null && siblingIds != null) {
                    val kind = try { ProviderKind.valueOf(providerKind) } catch (_: Exception) { null }
                    val provider = if (kind != null) {
                        try {
                            if (kind.name.startsWith("NETWORK_")) {
                                val configs = AppConfigurations(context)
                                val conn = configs.networkConnections.find { it.id == connectionId }
                                if (conn != null) StorageProviders.network(conn) else null
                            } else {
                                StorageProviders.providerFor(kind)
                            }
                        } catch (_: Exception) { null }
                    } else null

                    if (provider != null) {
                        val images = siblingIds.map { id ->
                            UniversalFile(
                                name = id.substringAfterLast('/'),
                                isDirectory = false,
                                lastModified = 0L,
                                length = 0L,
                                provider = provider,
                                providerId = id,
                                mimeType = null
                            )
                        }
                        withContext(Dispatchers.Main) {
                            siblingImages = images
                            val currentId = intent.getStringExtra("CURRENT_ID") ?: initialImageUri
                            currentUri = images.find { it.providerId == currentId } ?: images.firstOrNull() ?: initialImageUri
                        }
                        return@withContext
                    }
                }

                val extensions = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
                val images = getSiblingFiles(context, initialImageUri, extensions)
                withContext(Dispatchers.Main) {
                    siblingImages = images
                    val initialFileSegment = Uri.parse(initialImageUri).lastPathSegment
                    val match = images.find { it == initialImageUri || Uri.parse(it).lastPathSegment == initialFileSegment }
                    if (match != null) {
                        currentUri = match
                    } else if (images.isNotEmpty()) {
                        currentUri = images.first()
                    }
                }
            }
        }
    }

    LaunchedEffect(currentUri, siblingImages) {
        val index = siblingImages.indexOf(currentUri)
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.scrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (siblingImages.isNotEmpty() && pagerState.currentPage in siblingImages.indices) {
            val newUri = siblingImages[pagerState.currentPage]
            if (currentUri != newUri) {
                currentUri = newUri
            }
        }
    }

    LaunchedEffect(currentUri) {
        scale = 1f
        offset = Offset.Zero
    }

    Scaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
                .focusRequester(focusRequester)
                .focusable() // Makes the box able to hear the keyboard
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            // 'F' key for Fullscreen
                            Key.F -> {
                                onToggleFullscreen()
                                true // means we handled the event
                            }
                            // 'Ctrl + W' to close
                            Key.W -> {
                                if (keyEvent.isCtrlPressed) {
                                    activity?.finish()
                                    true
                                } else false
                            }
                            // Left/Right arrow keys for navigation
                            Key.DirectionLeft -> {
                                val idx = siblingImages.indexOf(currentUri)
                                if (idx > 0) {
                                    currentUri = siblingImages[idx - 1]
                                }
                                true
                            }

                            Key.DirectionRight -> {
                                val idx = siblingImages.indexOf(currentUri)
                                if (idx >= 0 && idx < siblingImages.size - 1) {
                                    currentUri = siblingImages[idx + 1]
                                }
                                true
                            }

                            else -> false
                        }
                    } else false
                }
        ) {
            val density = LocalDensity.current
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val screenCenter = Offset(screenWidthPx / 2f, screenHeightPx / 2f)

            fun calculatePanLimits(): Pair<Float, Float> {
                if (imageSize == Size.Zero || imageSize.width == 0f || imageSize.height == 0f) {
                    return Pair(0f, 0f)
                }
                val imageAspect = imageSize.width / imageSize.height
                val screenAspect = screenWidthPx / screenHeightPx
                val fittedWidth: Float
                val fittedHeight: Float
                if (imageAspect > screenAspect) {
                    fittedWidth = screenWidthPx
                    fittedHeight = screenWidthPx / imageAspect
                } else {
                    fittedHeight = screenHeightPx
                    fittedWidth = screenHeightPx * imageAspect
                }
                val maxX = maxOf(0f, (fittedWidth * scale - screenWidthPx) / 2f)
                val maxY = maxOf(0f, (fittedHeight * scale - screenHeightPx) / 2f)
                return Pair(maxX, maxY)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = scale == 1f,
                    beyondViewportPageCount = 1
                ) { page ->
                    val item = siblingImages[page]
                    val isCurrent = item == currentUri

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (isCurrent) {
                                Modifier
                                    .pointerInput(Unit) {
                                        detectTapGestures(onDoubleTap = { onToggleFullscreen() })
                                    }
                                    .contextMenuDetector { clickOffset ->
                                        menuOffset = clickOffset
                                        showMenu = true
                                    }
                                    .pointerInput(screenWidthPx, screenHeightPx) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                
                                                // 1. Handle Tertiary Click (Mouse Wheel Click) to close
                                                if (event.type == PointerEventType.Press && event.buttons.isTertiaryPressed) {
                                                    activity?.finish()
                                                }
                                                
                                                // 2. Handle Ctrl + Scroll to Zoom
                                                if (event.type == PointerEventType.Scroll && event.keyboardModifiers.isCtrlPressed) {
                                                    val change = event.changes.first()
                                                    val delta = change.scrollDelta.y
                                                    val oldScale = scale
                                                    val zoomFactor = if (delta < 0) 1.1f else 0.9f
                                                    scale = (scale * zoomFactor).coerceIn(1f, 10f)
                                                    val zoomRatio = scale / oldScale
                                                    val focalPoint = change.position - screenCenter
                                                    val newOffset = offset * zoomRatio + focalPoint * (1f - zoomRatio)
                                                    val (maxX, maxY) = calculatePanLimits()
                                                    offset = Offset(
                                                        x = newOffset.x.coerceIn(-maxX, maxX),
                                                        y = newOffset.y.coerceIn(-maxY, maxY)
                                                    )
                                                }
                                                
                                                // 3. Handle Scroll to Navigate
                                                if (event.type == PointerEventType.Scroll && !event.keyboardModifiers.isCtrlPressed) {
                                                    val change = event.changes.first()
                                                    val direction = change.scrollDelta.y
                                                    val idx = siblingImages.indexOf(currentUri)
                                                    if (idx > 0 && direction < 0f) {
                                                        currentUri = siblingImages[idx - 1]
                                                    }
                                                    if (idx >= 0 && idx < siblingImages.size - 1 && direction > 0f) {
                                                        currentUri = siblingImages[idx + 1]
                                                    }
                                                }
                                                
                                                // 4. Handle Mouse Pan (Ctrl + Primary) or Touch Pan/Zoom
                                                val isCtrlPan = event.type == PointerEventType.Move &&
                                                                event.keyboardModifiers.isCtrlPressed &&
                                                                event.buttons.isPrimaryPressed
                                                
                                                if (isCtrlPan) {
                                                    val change = event.changes.first()
                                                    val mousePan = change.position - change.previousPosition
                                                    val (maxX, maxY) = calculatePanLimits()
                                                    offset = Offset(
                                                        x = (offset.x + mousePan.x).coerceIn(-maxX, maxX),
                                                        y = (offset.y + mousePan.y).coerceIn(-maxY, maxY)
                                                    )
                                                    event.changes.forEach { it.consume() }
                                                } else if (event.type == PointerEventType.Move) {
                                                    val zoom = event.calculateZoom()
                                                    val pan = event.calculatePan()
                                                    val centroid = event.calculateCentroid(useCurrent = false)
                                                    val isActuallyZoomed = scale > 1f || zoom != 1f

                                                    if (isActuallyZoomed && centroid != Offset.Unspecified) {
                                                        val oldScale = scale
                                                        scale = (scale * zoom).coerceIn(1f, 10f)
                                                        if (abs(scale - 1f) < 0.01f) scale = 1f
                                                        
                                                        val zoomRatio = scale / oldScale
                                                        val focalPoint = centroid - screenCenter
                                                        val targetOffset = if (scale > 1f) {
                                                            (offset + pan) * zoomRatio + focalPoint * (1f - zoomRatio)
                                                        } else {
                                                            Offset.Zero
                                                        }
                                                        
                                                        val (maxX, maxY) = calculatePanLimits()
                                                        offset = Offset(
                                                            x = targetOffset.x.coerceIn(-maxX, maxX),
                                                            y = targetOffset.y.coerceIn(-maxY, maxY)
                                                        )
                                                        
                                                        if (zoom != 1f || scale > 1f) {
                                                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = item,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onSuccess = { if (isCurrent) imageSize = it.painter.intrinsicSize },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = if (isCurrent) scale else 1f,
                                    scaleY = if (isCurrent) scale else 1f,
                                    translationX = if (isCurrent) offset.x else 0f,
                                    translationY = if (isCurrent) offset.y else 0f
                                )
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(
                        x = with(LocalDensity.current) { menuOffset.x.toDp() },
                        y = with(LocalDensity.current) { menuOffset.y.toDp() }
                    ),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = LocalExtendedColors.current.menuBackground
                ) {
                    // --- GROUP 1: Open With ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_open_with)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(R.string.menu_open_with)) },
                        onClick = {
                            showMenu = false
                            currentUri?.let { data ->
                                try {
                                    val contentUri = getSecureContentUri(context, data)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(contentUri, "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, resources.getString(R.string.menu_open_with)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, resources.getString(R.string.msg_failed_open_image), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    HorizontalDivider()

                    // --- GROUP 2: Zoom Controls ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_zoom_in))},
                        leadingIcon = { Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.menu_zoom_in)) },
                        onClick = {
                            showMenu = false
                            scale = (scale * 1.5f).coerceIn(1f, 10f)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_reset_zoom)) },
                        leadingIcon = { Icon(Icons.Default.FitScreen, contentDescription = stringResource(R.string.menu_reset_zoom)) },
                        onClick = {
                            showMenu = false
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_zoom_out))},
                        leadingIcon = { Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.menu_zoom_out)) },
                        onClick = {
                            showMenu = false
                            scale = (scale / 1.5f).coerceIn(1f, 10f)
                            if (scale == 1f) offset = Offset.Zero
                        }
                    )

                    HorizontalDivider()

                    // --- GROUP 3: File Operations ---
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_copy_image)) },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = stringResource(R.string.menu_copy_image)) },
                        onClick = {
                            showMenu = false
                            currentUri?.let { data ->
                                try {
                                    val contentUri = getSecureContentUri(context, data)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newUri(context.contentResolver, "Copied Image", contentUri)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, resources.getString(R.string.menu_copy_image), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, resources.getString(R.string.msg_failed_copy_image), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_set_wallpaper)) },
                        leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = stringResource(R.string.menu_set_wallpaper)) },
                        onClick = {
                            showMenu = false
                            currentUri?.let { data ->
                                try {
                                    val contentUri = getSecureContentUri(context, data)
                                    val wallpaperIntent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                                        setDataAndType(contentUri, "image/*")
                                        putExtra("mimeType", "image/*")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(wallpaperIntent, resources.getString(R.string.menu_set_wallpaper)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, resources.getString(R.string.menu_set_wallpaper_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_share)) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.menu_share)) },
                        onClick = {
                            showMenu = false
                            currentUri?.let { data ->
                                try {
                                    val contentUri = getSecureContentUri(context, data)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, contentUri)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.msg_share_image_via)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, resources.getString(R.string.media_share_image_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_rename)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.menu_rename)) },
                        onClick = {
                            showMenu = false
                            withImageFile(currentUri) { file ->
                                val target = file.toUniversal()
                                FileOperationsManager.openRename(target, context) { newName ->
                                    FileOperationsManager.start()
                                    FileOperationsManager.update(0, 1, operationType= OperationType.RENAME)
                                    FileOperationsManager.currentFileName.value = target.name
                                    startPopUpActivity(context)
                                    coroutineScope.launch {
                                        val success = renameFile(target, newName)
                                        if (success) {
                                            val newFile = File(file.parentFile, newName)
                                            val newUriString = Uri.fromFile(newFile).toString()
                                            val index = siblingImages.indexOf(currentUri)
                                            if (index != -1) {
                                                siblingImages = siblingImages.toMutableList().apply { set(index, newUriString) }
                                                currentUri = newUriString
                                            }
                                            GlobalEvents.triggerRefresh()
                                        } else {
                                            withContext(Dispatchers.Main) { Toast.makeText(context, resources.getString(R.string.msg_rename_failed), Toast.LENGTH_SHORT).show() }
                                        }
                                        FileOperationsManager.finish()
                                    }
                                }
                                startPopUpActivity(context)
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.menu_delete)) },
                        onClick = {
                            showMenu = false
                            withImageFile(currentUri) { file ->
                                FileOperationsManager.start()
                                startPopUpActivity(context)
                                coroutineScope.launch {
                                    deleteFiles(context, listOf(file.toUniversal()))
                                    if (!file.exists()) {
                                        val index = siblingImages.indexOf(currentUri)
                                        val newList = siblingImages.filter { it != currentUri }
                                        if (newList.isEmpty()) {
                                            activity?.finish()
                                        } else {
                                            siblingImages = newList
                                            currentUri = if (index < newList.size) newList[index] else newList.last()
                                        }
                                        GlobalEvents.triggerRefresh()
                                    }
                                }
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_properties)) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.menu_properties)) },
                        onClick = {
                            showMenu = false
                            withImageFile(currentUri) { file ->
                                FileOperationsManager.showProperties(listOf(file.toUniversal()))
                                startPopUpActivity(context)
                            }
                        }
                    )

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_fullscreen)) },
                        leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.menu_fullscreen)) },
                        onClick = {
                            showMenu = false
                            onToggleFullscreen()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.close)) },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) },
                        onClick = {
                            showMenu = false
                            activity?.finish()
                        }
                    )
                }

                // Filmstrip overlay at the bottom
                if (siblingImages.size > 1 && !isFullscreen) {
                    val configuration = LocalConfiguration.current
                    val screenWidthDp = configuration.screenWidthDp.dp
                    val itemSizeDp = 64.dp
                    val horizontalPadding = (screenWidthDp - 32.dp - itemSizeDp) / 2

                    LaunchedEffect(listState) {
                        snapshotFlow {
                            val layoutInfo = listState.layoutInfo
                            if (layoutInfo.visibleItemsInfo.isEmpty()) return@snapshotFlow -1
                            val centerOffset = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
                            var closestIndex = -1
                            var minDistance = Int.MAX_VALUE
                            for (item in layoutInfo.visibleItemsInfo) {
                                val distance = abs(item.offset + item.size / 2 - centerOffset)
                                if (distance < minDistance) {
                                    minDistance = distance
                                    closestIndex = item.index
                                }
                            }
                            closestIndex
                        }.collect { index ->
                            if (index in siblingImages.indices) {
                                val newUri = siblingImages[index]
                                if (currentUri != newUri) {
                                    currentUri = newUri
                                }
                            }
                        }
                    }

                    LazyRow(
                        state = listState,
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                        contentPadding = PaddingValues(horizontal = horizontalPadding),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(siblingImages) { imgUri ->
                            val isSelected = imgUri == currentUri
                            AsyncImage(
                                model = imgUri,
                                contentDescription = stringResource(R.string.media_thumbnail),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(itemSizeDp)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            val idx = siblingImages.indexOf(imgUri)
                                            if (idx >= 0) listState.animateScrollToItem(idx)
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun withImageFile(data: Any?, action: (File) -> Unit) {
    if (data is UniversalFile) {
        data.fileRef?.let { if (it.exists()) action(it) }
        return
    }
    val uriString = data as? String
    uriString?.let {
        val uri = Uri.parse(it)
        val path = if (uri.scheme == "file" || uri.scheme == null) uri.path ?: it else null
        path?.let { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                action(file)
            }
        }
    }
}

private fun startPopUpActivity(context: Context) {
    val intent = Intent(context, PopUpActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

fun getSecureContentUri(context: Context, data: Any): Uri {
    if (data is UniversalFile) {
        return getUriForUniversalFile(context, data) ?: Uri.parse(data.providerId)
    }
    val uriString = data.toString()
    val uri = Uri.parse(uriString)
    if (uri.scheme == "content") return uri
    val file = File(uri.path ?: uriString)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
