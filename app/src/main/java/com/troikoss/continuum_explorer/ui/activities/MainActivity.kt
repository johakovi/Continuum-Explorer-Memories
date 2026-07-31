package com.troikoss.continuum_explorer.ui.activities
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.troikoss.continuum_explorer.providers.UniversalFileFetcher
import com.troikoss.continuum_explorer.model.LibraryItem
import com.troikoss.continuum_explorer.ui.FileExplorer
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.providers.StorageProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import android.Manifest
import androidx.compose.ui.unit.dp
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import coil.Coil
import rikka.shizuku.Shizuku
import com.troikoss.continuum_explorer.managers.ShizukuManager
import com.troikoss.continuum_explorer.managers.CleanupManager
import android.os.Handler
import android.os.Looper
import com.troikoss.continuum_explorer.utils.GlobalEvents

open class MainActivity : AppCompatActivity() {

    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        android.util.Log.d("MainActivity", "Inactivity timeout reached, closing app")
        finishAffinity()
    }

    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(sleepRunnable)
        val timeoutMinutes = SettingsManager.appInactivityTimeout.value
        if (timeoutMinutes > 0) {
            inactivityHandler.postDelayed(sleepRunnable, timeoutMinutes.toLong() * 60 * 1000)
        }
    }

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
             com.troikoss.continuum_explorer.utils.GlobalEvents.triggerRefresh()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // The user granted the permission. Notifications will work!
        } else {
            // The user denied the permission.
            // Optional: You could show a message here explaining why you need it later.
        }
    }


    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        if (!readGranted || !writeGranted) {
            // Handle permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.setRestrictedCaptionAreaListener { rect ->
                val density = resources.displayMetrics.density
                val screenWidth = resources.displayMetrics.widthPixels
                
                if (rect.width() > 0) {
                    val rightPadding = if (rect.left > screenWidth / 2) rect.width() else 0
                    val leftPadding = if (rect.right < screenWidth / 2) rect.width() else 0
                    
                    com.troikoss.continuum_explorer.managers.WindowManager.updateRestrictedArea(
                        (leftPadding / density).dp,
                        (rightPadding / density).dp
                    )
                } else {
                    com.troikoss.continuum_explorer.managers.WindowManager.updateRestrictedArea(0.dp, 0.dp)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Request permissions for Android 10 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (permissionsToRequest.isNotEmpty()) {
                requestStoragePermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            // Request "All Files Access" permission on Android 11+ (API 30+)
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            }
        }

        // Shizuku check
        if (ShizukuManager.isAvailable() && !ShizukuManager.hasPermission()) {
            ShizukuManager.requestPermission(1002)
        }

        // Initialize settings and storage providers
        SettingsManager.init(applicationContext)
        com.troikoss.continuum_explorer.managers.ThemePackManager.init(applicationContext)
        com.troikoss.continuum_explorer.managers.MusicMetadataManager.init(applicationContext)
        StorageProviders.init(applicationContext)

        resetInactivityTimer()
        lifecycleScope.launch {
            GlobalEvents.activityEvent.collect {
                resetInactivityTimer()
            }
        }

        if (SettingsManager.isFtpServerEnabled.value) {
            SettingsManager.setFtpServerEnabled(applicationContext, true)
        }

        // Clear cache and temp files on startup
        lifecycleScope.launch(Dispatchers.IO) {
            CleanupManager.clearCache(applicationContext)
        }

        val initialPath = intent.getStringExtra("path")
        val initialUri = intent.getStringExtra("uri")
        val initialArchive = run {
            val path = intent.getStringExtra("archivePath")
            path?.let { File(it) }
        }
        val initialArchiveUri = intent.getParcelableExtra<Uri>("archiveUri")
        val initialArchiveName = intent.getStringExtra("archiveName")
        val initialNetworkConnectionId = intent.getStringExtra("networkConnectionId")

        val initialLibraryItemFromIntent = when {
            intent.getBooleanExtra("isRecent", false) -> LibraryItem.Recent
            intent.getBooleanExtra("isGallery", false) -> LibraryItem.Gallery
            intent.getBooleanExtra("isDocuments", false) -> LibraryItem.Documents
            intent.getBooleanExtra("isArchives", false) -> LibraryItem.Archives
            intent.getBooleanExtra("isApks", false) -> LibraryItem.Apks
            intent.getBooleanExtra("isRecycleBin", false) -> LibraryItem.RecycleBin
            else -> LibraryItem.None
        }

        val initialLibraryItem = if (initialLibraryItemFromIntent == LibraryItem.None &&
            initialPath == null && initialUri == null && initialArchive == null &&
            initialArchiveUri == null && initialNetworkConnectionId == null
        ) {
            SettingsManager.startingPage.value
        } else {
            initialLibraryItemFromIntent
        }

        // Build ImageLoader asynchronously to prevent main thread lag on startup
        lifecycleScope.launch(Dispatchers.IO) {
            val videoImageLoader = ImageLoader.Builder(this@MainActivity)
                .components {
                    add(UniversalFileFetcher.Factory())
                    add(VideoFrameDecoder.Factory())
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(coil.decode.ImageDecoderDecoder.Factory())
                    } else {
                        add(coil.decode.GifDecoder.Factory())
                    }
                }
                .crossfade(enable = true)
                .build()

            // Set this as the global loader
            Coil.setImageLoader(videoImageLoader)
        }

        setContent {
            FileExplorerTheme {
                FileExplorer(
                    initialPath = initialPath,
                    initialUri = initialUri,
                    initialArchive = initialArchive,
                    initialArchiveUri = initialArchiveUri,
                    initialArchiveName = initialArchiveName,
                    initialLibraryItem = initialLibraryItem,
                    initialNetworkConnectionId = initialNetworkConnectionId
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        resetInactivityTimer()
        // Aggressive cleanup of remote files when returning to the app
        lifecycleScope.launch(Dispatchers.IO) {
            CleanupManager.clearNetworkCache(applicationContext)
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetInactivityTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        inactivityHandler.removeCallbacks(sleepRunnable)
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        
        com.troikoss.continuum_explorer.managers.AudioManager.release()

        // Final cleanup on exit - use a separate scope so it's not cancelled by lifecycle
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            CleanupManager.clearCache(applicationContext)
        }
    }
}
