package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.os.Environment
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import java.io.File

/**
 * Manages music playlists.
 * Supports importing/browsing .m3u, .m3u8, and .wpl files.
 * Scans /Music/Playlists and ~/Playlists/ within user-selected music folders.
 */
object PlaylistManager {
    private val playlistDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Playlists")

    fun createPlaylist(name: String): Boolean {
        if (!playlistDir.exists()) playlistDir.mkdirs()
        val fileName = if (name.lowercase().endsWith(".m3u") || name.lowercase().endsWith(".m3u8") || name.lowercase().endsWith(".wpl")) {
            name
        } else {
            "$name.m3u"
        }
        val file = File(playlistDir, fileName)
        return try {
            if (!file.exists()) {
                file.createNewFile()
                val ext = file.extension.lowercase()
                if (ext.startsWith("m3u")) {
                    file.writeText("#EXTM3U\n")
                } else if (ext == "wpl") {
                    file.writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?wpl version=\"1.0\"?>\n<smil>\n    <head>\n        <meta name=\"Generator\" content=\"Continuum Explorer\"/>\n        <title>${file.nameWithoutExtension}</title>\n    </head>\n    <body>\n        <seq>\n        </seq>\n    </body>\n</smil>")
                }
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addSongToPlaylist(playlistFile: File, songPath: String): Boolean {
        if (!playlistFile.exists() || !playlistFile.isFile) return false
        return try {
            val ext = playlistFile.extension.lowercase()
            if (ext == "wpl") {
                var content = playlistFile.readText()
                if (!content.contains("<seq>")) {
                    content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?wpl version=\"1.0\"?>\n<smil>\n    <body>\n        <seq>\n        </seq>\n    </body>\n</smil>"
                }
                val mediaTag = "<media src=\"$songPath\"/>"
                val newContent = content.replace("</seq>", "            $mediaTag\n        </seq>")
                playlistFile.writeText(newContent)
                true
            } else {
                val currentContent = playlistFile.readText()
                val prefix = if (currentContent.isNotEmpty() && !currentContent.endsWith("\n")) "\n" else ""
                playlistFile.appendText("$prefix$songPath\n")
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getPlaylists(context: Context): List<UniversalFile> {
        val allPlaylists = mutableListOf<File>()

        // 1. Default system location
        if (playlistDir.exists()) {
            allPlaylists.addAll(playlistDir.listFiles()?.filter { isSupportedPlaylist(it) } ?: emptyList())
        } else {
            playlistDir.mkdirs()
        }

        // 2. User selected music folders' subdirectories
        SettingsManager.musicFolders.value.forEach { folderPath ->
            val userPlaylistDir = File(folderPath, "Playlists")
            if (userPlaylistDir.exists() && userPlaylistDir.isDirectory) {
                allPlaylists.addAll(userPlaylistDir.listFiles()?.filter { isSupportedPlaylist(it) } ?: emptyList())
            }
        }

        return allPlaylists.distinctBy { it.absolutePath }
            .map { file ->
                val ext = file.extension.lowercase()
                UniversalFile(
                    name = file.nameWithoutExtension,
                    isDirectory = true,
                    lastModified = file.lastModified(),
                    length = file.length(),
                    provider = LocalProvider,
                    providerId = file.absolutePath,
                    parentId = "virtual://music/playlists",
                    mimeType = if (ext == "wpl") "application/vnd.ms-wpl" else "audio/x-mpegurl"
                )
            }.sortedBy { it.name.lowercase() }
    }

    private fun isSupportedPlaylist(file: File): Boolean {
        if (!file.isFile) return false
        val ext = file.extension.lowercase()
        return ext == "m3u" || ext == "m3u8" || ext == "wpl"
    }

    suspend fun getPlaylistSongs(context: Context, playlistFile: File): List<UniversalFile> {
        val songs = mutableListOf<UniversalFile>()
        if (!playlistFile.exists()) return songs

        try {
            val paths = when (playlistFile.extension.lowercase()) {
                "wpl" -> parseWpl(playlistFile)
                else -> parseM3u(playlistFile)
            }

            paths.forEach { path ->
                val file = resolveFile(playlistFile, path)
                if (file != null) {
                    val metadata = MusicMetadataManager.getSongMetadata(file.absolutePath)
                    songs.add(
                        UniversalFile(
                            name = metadata?.title ?: file.name,
                            isDirectory = false,
                            lastModified = file.lastModified(),
                            length = file.length(),
                            provider = LocalProvider,
                            providerId = file.absolutePath,
                            parentId = file.parentFile?.absolutePath,
                            mimeType = "audio/*"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songs
    }

    private fun parseM3u(file: File): List<String> {
        return file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { it.trim() }
    }

    private fun parseWpl(file: File): List<String> {
        val paths = mutableListOf<String>()
        try {
            val content = file.readText()
            // Quick regex parser for <media src="..." />
            val regex = Regex("<media\\s+src=\"([^\"]+)\"\\s*/?>", RegexOption.IGNORE_CASE)
            regex.findAll(content).forEach { match ->
                paths.add(match.groupValues[1])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return paths
    }

    private fun resolveFile(playlistFile: File, src: String): File? {
        val normalizedSrc = src.replace('\\', '/')
        val file = if (normalizedSrc.startsWith("/") || normalizedSrc.contains(":/")) {
            File(normalizedSrc)
        } else {
            File(playlistFile.parentFile, normalizedSrc)
        }
        return if (file.exists() && file.isFile) file else null
    }
}
