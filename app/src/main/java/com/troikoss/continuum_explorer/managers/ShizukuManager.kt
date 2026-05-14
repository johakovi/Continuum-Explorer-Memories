package com.troikoss.continuum_explorer.managers

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.troikoss.continuum_explorer.IFileService
import com.troikoss.continuum_explorer.services.ShizukuFileService
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CompletableDeferred
import android.content.pm.PackageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking

object ShizukuManager {
    private var fileService: IFileService? = null
    private var binderDeferred = CompletableDeferred<IFileService>()

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName("com.troikoss.continuum_explorer_memories", ShizukuFileService::class.java.name)
    ).daemon(false).tag("file_service")

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = IFileService.Stub.asInterface(binder)
            fileService = service
            binderDeferred.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileService = null
            binderDeferred = CompletableDeferred<IFileService>()
        }
    }

    fun isAvailable(): Boolean = try { Shizuku.pingBinder() } catch (_: Exception) { false }

    fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isAvailable()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getService(): IFileService {
        val currentService = fileService
        if (currentService != null) return currentService
        
        if (binderDeferred.isCompleted) {
            val completed = binderDeferred.getCompleted()
            fileService = completed
            return completed
        }
        
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (_: Exception) {
        }
        return binderDeferred.await()
    }

    fun getServiceBlocking(): IFileService? = runBlocking {
        try {
            getService()
        } catch (_: Exception) {
            null
        }
    }
}
