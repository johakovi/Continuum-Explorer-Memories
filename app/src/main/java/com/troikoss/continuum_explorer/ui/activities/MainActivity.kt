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
import androidx.appcompat.app.AppCompatActivity
import coil.Coil
import java.util.UUID
import rikka.shizuku.Shizuku
import com.troikoss.continuum_explorer.managers.ShizukuManager

class MainActivity : AppCompatActivity() {

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
        super.onCreate(savedInstanceState)
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
        StorageProviders.init(applicationContext)

        if (intent.action == "com.troikoss.continuum_explorer.OPEN_NEW_WINDOW") {
            val freshIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                data = Uri.fromParts("window", UUID.randomUUID().toString(), null)
                putExtra("path", Environment.getExternalStorageDirectory().absolutePath)
            }
            startActivity(freshIntent)
            finish()
            return
        }

        val initialPath = intent.getStringExtra("path")
        val initialUri = intent.getStringExtra("uri")
        val initialArchive = run {
            val path = intent.getStringExtra("archivePath")
            path?.let { File(it) }
        }
        val initialLibraryItem = when {
            intent.getBooleanExtra("isRecent", false) -> LibraryItem.Recent
            intent.getBooleanExtra("isGallery", false) -> LibraryItem.Gallery
            intent.getBooleanExtra("isDocuments", false) -> LibraryItem.Documents
            intent.getBooleanExtra("isRecycleBin", false) -> LibraryItem.RecycleBin
            else -> LibraryItem.None
        }
        val initialNetworkConnectionId = intent.getStringExtra("networkConnectionId")

        // Build ImageLoader asynchronously to prevent main thread lag on startup
        lifecycleScope.launch(Dispatchers.IO) {
            val videoImageLoader = ImageLoader.Builder(this@MainActivity)
                .components {
                    add(UniversalFileFetcher.Factory())
                    add(VideoFrameDecoder.Factory())
                }
                .crossfade(enable = true)
                .build()

            // Set this as the global loader
            Coil.setImageLoader(videoImageLoader)
        }

        setContent {
            FileExplorerTheme {
                FileExplorer(initialPath = initialPath, initialUri = initialUri, initialArchive = initialArchive, initialLibraryItem = initialLibraryItem, initialNetworkConnectionId = initialNetworkConnectionId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == "com.troikoss.continuum_explorer.OPEN_NEW_WINDOW") {
            val newWindowIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                data = Uri.fromParts("window", UUID.randomUUID().toString(), null)
                putExtra("path", Environment.getExternalStorageDirectory().absolutePath)
            }
            startActivity(newWindowIntent)
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }
}
