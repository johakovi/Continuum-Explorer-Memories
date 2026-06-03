package com.troikoss.continuum_explorer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.troikoss.continuum_explorer.managers.SettingsManager
import com.troikoss.continuum_explorer.utils.GlobalEvents
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File

class FtpServerService : Service() {

    private var server: FtpServer? = null
    private var currentMode = SettingsManager.FtpMode.FULL_STORAGE

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable {
        android.util.Log.d("FtpServerService", "Inactivity timeout reached, stopping FTP server")
        SettingsManager.setFtpServerEnabled(applicationContext, false)
    }

    private fun resetTimer() {
        handler.removeCallbacks(stopRunnable)
        val timeoutMinutes = SettingsManager.ftpInactivityTimeout.value
        if (timeoutMinutes > 0) {
            handler.postDelayed(stopRunnable, timeoutMinutes.toLong() * 60 * 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        SettingsManager.init(applicationContext)
        com.troikoss.continuum_explorer.providers.StorageProviders.init(applicationContext)
        createNotificationChannel()
        serviceScope.launch {
            GlobalEvents.activityEvent.collect {
                resetTimer()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopServer()
            stopSelf()
            return START_NOT_STICKY
        }

        val modeName = intent?.getStringExtra(EXTRA_MODE)
        if (modeName != null) {
            currentMode = try {
                SettingsManager.FtpMode.valueOf(modeName)
            } catch (_: Exception) {
                if (modeName == "GAME_SAVES" || modeName == "GAMES") SettingsManager.FtpMode.GAMES else SettingsManager.FtpMode.FULL_STORAGE
            }
        } else {
            currentMode = SettingsManager.ftpMode.value
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        startServer()
        return START_STICKY
    }

    private fun startServer() {
        if (server != null) {
            server?.stop()
            server = null
        }

        try {
            val serverFactory = FtpServerFactory()
            
            val ftplets = mutableMapOf<String, Ftplet>()
            ftplets["activityTracker"] = object : DefaultFtplet() {
                override fun beforeCommand(session: FtpSession?, request: FtpRequest?): FtpletResult {
                    resetTimer()
                    return FtpletResult.DEFAULT
                }

                override fun onConnect(session: FtpSession?): FtpletResult {
                    resetTimer()
                    return FtpletResult.DEFAULT
                }
            }
            serverFactory.ftplets = ftplets

            val listenerFactory = ListenerFactory()
            listenerFactory.port = PORT
            serverFactory.addListener("default", listenerFactory.createListener())

            val userManagerFactory = PropertiesUserManagerFactory()
            val userFile = File(filesDir, "users.properties")
            if (userFile.exists()) userFile.delete()
            userFile.createNewFile()
            userManagerFactory.file = userFile
            
            val userManager = userManagerFactory.createUserManager()

            val ftpUser = SettingsManager.ftpUser.value
            val ftpPass = SettingsManager.ftpPassword.value

            val user = BaseUser()
            user.name = ftpUser
            user.password = ftpPass
            
            val isGameSaves = currentMode == SettingsManager.FtpMode.GAMES
            
            user.homeDirectory = if (isGameSaves) "/" else Environment.getExternalStorageDirectory().absolutePath
            val authorities = mutableListOf<Authority>()
            authorities.add(WritePermission())
            user.authorities = authorities
            userManager.save(user)

            serverFactory.userManager = userManager
            
            serverFactory.fileSystem = object : FileSystemFactory {
                override fun createFileSystemView(user: User): FileSystemView {
                    return if (isGameSaves) {
                        GamesFileSystemView(applicationContext, user)
                    } else {
                        ShizukuFileSystemView(user, user.homeDirectory)
                    }
                }
            }

            server = serverFactory.createServer()
            server?.start()
            resetTimer()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopServer() {
        handler.removeCallbacks(stopRunnable)
        server?.stop()
        server = null
    }

    override fun onDestroy() {
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FTP Server",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val ip = getIpAddress()
        val url = "ftp://$ip:$PORT"
        val ftpUser = SettingsManager.ftpUser.value
        val ftpPass = SettingsManager.ftpPassword.value
        
        val modeTitle = if (currentMode == SettingsManager.FtpMode.GAMES)
            "FTP Game Saves Server Running" else "FTP Server Running"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(modeTitle)
            .setContentText("Address: $url ($ftpUser/$ftpPass)")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun getIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
            "0.0.0.0"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    companion object {
        private const val CHANNEL_ID = "ftp_server_channel"
        private const val NOTIFICATION_ID = 1001
        private const val PORT = 2121
        const val ACTION_STOP = "com.troikoss.continuum_explorer.STOP_FTP_SERVER"
        const val EXTRA_MODE = "ftp_mode"
    }
}
