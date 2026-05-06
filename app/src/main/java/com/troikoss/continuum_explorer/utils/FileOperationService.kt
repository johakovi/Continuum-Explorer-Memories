package com.troikoss.continuum_explorer.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.troikoss.continuum_explorer.managers.FileOperationsManager

class FileOperationService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.startForeground(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }
}
