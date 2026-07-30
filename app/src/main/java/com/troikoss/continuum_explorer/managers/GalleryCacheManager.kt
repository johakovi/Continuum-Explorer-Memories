package com.troikoss.continuum_explorer.managers

import android.content.Context
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.SafProvider
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.utils.AppConfigurations
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GalleryCacheManager {
    private const val CACHE_DIR = "gallery_metadata"

    private fun getCacheFolder(context: Context): File {
        val folder = File(context.cacheDir, CACHE_DIR)
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    private fun getCacheFile(context: Context, key: String): File {
        val hash = java.security.MessageDigest.getInstance("MD5")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(getCacheFolder(context), "$hash.json")
    }

    fun saveCache(context: Context, key: String, files: List<UniversalFile>) {
        try {
            val root = JSONObject()
            root.put("timestamp", System.currentTimeMillis())
            val array = JSONArray()
            files.forEach { file ->
                val obj = JSONObject()
                obj.put("n", file.name)
                obj.put("d", file.isDirectory)
                obj.put("m", file.lastModified)
                obj.put("l", file.length)
                obj.put("pk", file.provider.kind.name)
                obj.put("pi", file.providerId)
                obj.put("p", file.parentId)
                obj.put("mt", file.mimeType)
                obj.put("ci", file.provider.connectionId)
                array.put(obj)
            }
            root.put("files", array)
            getCacheFile(context, key).writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCache(context: Context, key: String, maxAgeMs: Long = 3600000): List<UniversalFile>? {
        val file = getCacheFile(context, key)
        if (!file.exists()) return null

        try {
            val root = JSONObject(file.readText())
            val timestamp = root.getLong("timestamp")
            if (System.currentTimeMillis() - timestamp > maxAgeMs) {
                return null // Cache expired
            }

            val array = root.getJSONArray("files")
            val result = mutableListOf<UniversalFile>()
            val appConfigs = AppConfigurations(context)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val kind = ProviderKind.valueOf(obj.getString("pk"))
                val connectionId = obj.optString("ci", "")
                
                val provider = when (kind) {
                    ProviderKind.LOCAL -> LocalProvider
                    ProviderKind.SAF -> SafProvider
                    ProviderKind.SHIZUKU -> ShizukuProvider
                    else -> if (connectionId.isNotEmpty()) {
                        val conn = appConfigs.networkConnections.find { it.id == connectionId }
                        conn?.let { StorageProviders.network(it) } ?: LocalProvider
                    } else LocalProvider
                }

                result.add(UniversalFile(
                    name = obj.getString("n"),
                    isDirectory = obj.getBoolean("d"),
                    lastModified = obj.getLong("m"),
                    length = obj.getLong("l"),
                    provider = provider,
                    providerId = obj.getString("pi"),
                    parentId = if (obj.has("p")) obj.getString("p") else null,
                    mimeType = if (obj.has("mt")) obj.getString("mt") else null
                ))
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun clearCache(context: Context) {
        getCacheFolder(context).deleteRecursively()
    }
}
