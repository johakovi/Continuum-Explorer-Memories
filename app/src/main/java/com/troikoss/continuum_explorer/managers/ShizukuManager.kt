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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking

object ShizukuManager {
    private var fileService: IFileService? = null
    private var binderDeferred: CompletableDeferred<IFileService>? = null
    private var lastError: String? = null

    private var packageName: String = "com.troikoss.continuum_explorer_memories"

    fun init(packageName: String) {
        this.packageName = packageName
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.isBinderAlive) {
                val service = IFileService.Stub.asInterface(binder)
                fileService = service
                binderDeferred?.complete(service)
            } else {
                binderDeferred?.completeExceptionally(RuntimeException("Binder is null or not alive"))
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileService = null
            binderDeferred = null
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

    fun getLastError(): String = lastError ?: "No error recorded"

    fun requestPermission(requestCode: Int) {
        if (isAvailable()) {
            try {
                Shizuku.requestPermission(requestCode)
            } catch (_: Exception) {}
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getService(): IFileService? {
        val currentService = fileService
        if (currentService != null && currentService.asBinder().isBinderAlive) return currentService
        
        // If already trying to bind, wait for it
        val currentDeferred = binderDeferred
        if (currentDeferred != null && !currentDeferred.isCompleted) {
            return withTimeoutOrNull(10000) { currentDeferred.await() }
        }

        // Start new bind
        val newDeferred = CompletableDeferred<IFileService>()
        binderDeferred = newDeferred
        
        // Re-create args to ensure tag is set correctly
        val args = Shizuku.UserServiceArgs(ComponentName(packageName, ShizukuFileService::class.java.name))
            .daemon(false)
            .processNameSuffix("fileService")
            .tag("fileService")

        return withTimeoutOrNull(10000) {
            try {
                Shizuku.bindUserService(args, serviceConnection)
                newDeferred.await()
            } catch (e: Exception) {
                lastError = e.message
                null
            }
        }
    }

    fun getServiceBlocking(): IFileService? {
        val currentService = fileService
        if (currentService != null && currentService.asBinder().isBinderAlive) return currentService

        // If we're on the main thread and not connected, return null immediately to avoid deadlock.
        // runBlocking on the main thread prevents the service connection callback from running.
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            // If already trying to bind, getService() was likely called from a background thread
            // and it's currently waiting. We'll just return null here.
            return null
        }

        return runBlocking {
            getService()
        }
    }
}
