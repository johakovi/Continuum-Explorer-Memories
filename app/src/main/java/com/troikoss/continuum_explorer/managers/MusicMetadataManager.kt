package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaDataSource
import androidx.compose.runtime.*
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.StorageProvider
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.SafProvider
import com.troikoss.continuum_explorer.providers.StorageProviders
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import com.troikoss.continuum_explorer.utils.AppConfigurations
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream

data class MusicSongMetadata(
    val title: String,
    val artist: String?,
    val album: String?,
    val filePath: String,
    val duration: Long,
    val trackNumber: Int?,
    val lastSync: Long,
    val providerKind: String? = null,
    val connectionId: String? = null
)

data class MusicAlbumMetadata(
    val albumName: String,
    val artistName: String?,
    val folderPath: String,
    val trackCount: Int,
    val lastSync: Long,
    val coverPath: String? = null,
    val providerKind: String? = null,
    val connectionId: String? = null
)

private class UniversalMediaDataSource(
    private val provider: StorageProvider,
    private val providerId: String,
    private val size: Long
) : MediaDataSource() {
    private var stream: InputStream? = null
    private var currentPosition: Long = -1

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= this.size) return -1
        val toRead = if (position + size > this.size) (this.size - position).toInt() else size
        if (toRead <= 0) return 0

        try {
            if (stream == null || currentPosition != position) {
                stream?.close()
                stream = provider.openInput(providerId, position)
                currentPosition = position
            }
            
            var totalRead = 0
            while (totalRead < toRead) {
                val read = stream!!.read(buffer, offset + totalRead, toRead - totalRead)
                if (read == -1) break
                totalRead += read
                currentPosition += read
            }
            return if (totalRead == 0) -1 else totalRead
        } catch (e: Exception) {
            return -1
        }
    }

    override fun getSize(): Long = size

    override fun close() {
        stream?.close()
        stream = null
    }
}

object MusicMetadataManager {
    private const val METADATA_FOLDER = "music_metadata"
    private const val METADATA_FILE = "metadata.json"
    private const val SONGS_FILE = "songs.json"
    private const val FAVOURITES_FILE = "favourites.json"
    private const val COVERS_FOLDER = "covers"

    private var albums: List<MusicAlbumMetadata> = emptyList()
    private var songs: List<MusicSongMetadata> = emptyList()
    private val favouriteState = mutableStateMapOf<String, Boolean>()
    private val metadataMutex = kotlinx.coroutines.sync.Mutex()

    fun init(context: Context) {
        loadMetadata(context)
        loadFavourites(context)
    }

    private fun loadFavourites(context: Context) {
        val file = File(getMetadataFolder(context), FAVOURITES_FILE)
        if (file.exists()) {
            try {
                val jsonArray = JSONArray(file.readText())
                favouriteState.clear()
                for (i in 0 until jsonArray.length()) {
                    favouriteState[jsonArray.getString(i)] = true
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveFavourites(context: Context) {
        try {
            val jsonArray = JSONArray()
            favouriteState.keys.forEach { jsonArray.put(it) }
            File(getMetadataFolder(context), FAVOURITES_FILE).writeText(jsonArray.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isFavourite(path: String): Boolean = favouriteState[path] == true

    fun getSongMetadata(filePath: String): MusicSongMetadata? {
        return songs.find { it.filePath == filePath }
    }

    fun toggleFavourite(context: Context, path: String) {
        if (favouriteState.containsKey(path)) {
            favouriteState.remove(path)
        } else {
            favouriteState[path] = true
        }
        saveFavourites(context)
    }

    private fun getProvider(context: Context, kind: String?, connectionId: String?): StorageProvider {
        if (kind == null || kind == ProviderKind.LOCAL.name) return LocalProvider
        if (kind == ProviderKind.SAF.name) return SafProvider
        if (kind == ProviderKind.SHIZUKU.name) return ShizukuProvider
        
        if (connectionId != null) {
            val appConfigs = AppConfigurations(context)
            val conn = appConfigs.networkConnections.find { it.id == connectionId }
            if (conn != null) {
                return StorageProviders.network(conn)
            }
        }
        return LocalProvider
    }

    fun getFavourites(context: Context): List<UniversalFile> {
        if (songs.isEmpty()) {
            loadMetadata(context)
        }
        return songs.filter { favouriteState.containsKey(it.filePath) }.map { song ->
            val provider = getProvider(context, song.providerKind, song.connectionId)
            UniversalFile(
                name = song.title,
                isDirectory = false,
                lastModified = song.lastSync,
                length = 0L,
                provider = provider,
                providerId = song.filePath,
                parentId = provider.parentId(song.filePath),
                mimeType = "audio/*"
            )
        }
    }

    private fun loadMetadata(context: Context) {
        val metadataFolder = getMetadataFolder(context)
        val albumFile = File(metadataFolder, METADATA_FILE)
        val songFile = File(metadataFolder, SONGS_FILE)

        if (albumFile.exists()) {
            try {
                val jsonString = albumFile.readText()
                val jsonArray = JSONArray(jsonString)
                val loadedAlbums = mutableListOf<MusicAlbumMetadata>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    loadedAlbums.add(
                        MusicAlbumMetadata(
                            albumName = obj.getString("albumName"),
                            artistName = obj.optString("artistName", null),
                            folderPath = obj.getString("folderPath"),
                            trackCount = obj.getInt("trackCount"),
                            lastSync = obj.getLong("lastSync"),
                            coverPath = obj.optString("coverPath", null),
                            providerKind = obj.optString("providerKind", null),
                            connectionId = obj.optString("connectionId", null)
                        )
                    )
                }
                albums = loadedAlbums
            } catch (e: Exception) {
                e.printStackTrace()
                albums = emptyList()
            }
        } else {
            albums = emptyList()
        }

        if (songFile.exists()) {
            try {
                val jsonString = songFile.readText()
                val jsonArray = JSONArray(jsonString)
                val loadedSongs = mutableListOf<MusicSongMetadata>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    loadedSongs.add(
                        MusicSongMetadata(
                            title = obj.getString("title"),
                            artist = obj.optString("artist", null),
                            album = obj.optString("album", null),
                            filePath = obj.getString("filePath"),
                            duration = obj.getLong("duration"),
                            trackNumber = if (obj.isNull("trackNumber")) null else obj.getInt("trackNumber"),
                            lastSync = obj.getLong("lastSync"),
                            providerKind = obj.optString("providerKind", null),
                            connectionId = obj.optString("connectionId", null)
                        )
                    )
                }
                songs = loadedSongs
            } catch (e: Exception) {
                e.printStackTrace()
                songs = emptyList()
            }
        } else {
            songs = emptyList()
        }
    }

    private fun saveMetadata(context: Context) {
        val metadataFolder = getMetadataFolder(context)
        val albumFile = File(metadataFolder, METADATA_FILE)
        val songFile = File(metadataFolder, SONGS_FILE)

        try {
            val albumArray = JSONArray()
            albums.forEach { album ->
                val obj = JSONObject().apply {
                    put("albumName", album.albumName)
                    put("artistName", album.artistName ?: JSONObject.NULL)
                    put("folderPath", album.folderPath)
                    put("trackCount", album.trackCount)
                    put("lastSync", album.lastSync)
                    put("coverPath", album.coverPath ?: JSONObject.NULL)
                    put("providerKind", album.providerKind ?: JSONObject.NULL)
                    put("connectionId", album.connectionId ?: JSONObject.NULL)
                }
                albumArray.put(obj)
            }
            albumFile.writeText(albumArray.toString())

            val songArray = JSONArray()
            songs.forEach { song ->
                val obj = JSONObject().apply {
                    put("title", song.title)
                    put("artist", song.artist ?: JSONObject.NULL)
                    put("album", song.album ?: JSONObject.NULL)
                    put("filePath", song.filePath)
                    put("duration", song.duration)
                    put("trackNumber", song.trackNumber ?: JSONObject.NULL)
                    put("lastSync", song.lastSync)
                    put("providerKind", song.providerKind ?: JSONObject.NULL)
                    put("connectionId", song.connectionId ?: JSONObject.NULL)
                }
                songArray.put(obj)
            }
            songFile.writeText(songArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMetadataFolder(context: Context): File {
        val externalFilesDir = context.getExternalFilesDir(null)
        val folder = File(externalFilesDir ?: context.filesDir, METADATA_FOLDER)
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    private fun getCoversFolder(context: Context): File {
        val folder = File(getMetadataFolder(context), COVERS_FOLDER)
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    fun getAlbums(context: Context): List<UniversalFile> {
        if (albums.isEmpty()) {
            loadMetadata(context)
        }
        return albums.map { album ->
            val provider = getProvider(context, album.providerKind, album.connectionId)
            UniversalFile(
                name = album.albumName,
                isDirectory = true,
                lastModified = album.lastSync,
                length = album.trackCount.toLong(),
                provider = provider,
                providerId = "virtual://music/albums/${album.albumName}",
                parentId = "virtual://music/albums",
                mimeType = "album"
            )
        }
    }

    fun getSongs(context: Context): List<UniversalFile> {
        if (songs.isEmpty()) {
            loadMetadata(context)
        }
        return songs.map { song ->
            val provider = getProvider(context, song.providerKind, song.connectionId)
            UniversalFile(
                name = song.title,
                isDirectory = false,
                lastModified = song.lastSync,
                length = 0L,
                provider = provider,
                providerId = song.filePath,
                parentId = provider.parentId(song.filePath),
                mimeType = "audio/*"
            )
        }
    }

    suspend fun sync(context: Context, musicFolders: Set<String>) {
        val newAlbums = mutableListOf<MusicAlbumMetadata>()
        val newSongs = mutableListOf<MusicSongMetadata>()
        val retriever = MediaMetadataRetriever()
        val coversFolder = getCoversFolder(context)
        val appConfigs = AppConfigurations(context)

        musicFolders.forEach { folderKey ->
            when {
                folderKey.startsWith("network:") -> {
                    val parts = folderKey.split(":", limit = 4)
                    if (parts.size == 4) {
                        val kindName = parts[1]
                        val connId = parts[2]
                        val path = parts[3]
                        val conn = appConfigs.networkConnections.find { it.id == connId }
                        if (conn != null) {
                            val provider = StorageProviders.network(conn)
                            scanRemoteFolder(context, provider, path, kindName, connId, newAlbums, newSongs, retriever, coversFolder)
                        }
                    }
                }
                folderKey.startsWith("webdav://") || folderKey.startsWith("ftp://") || 
                folderKey.startsWith("sftp://") || folderKey.startsWith("smb://") -> {
                    val scheme = folderKey.substringBefore("://")
                    val remaining = folderKey.substringAfter("://")
                    val connId = remaining.substringBefore("/")
                    val path = "/" + remaining.substringAfter("/", "")
                    
                    val kindName = when (scheme) {
                        "webdav" -> ProviderKind.NETWORK_WEBDAV.name
                        "ftp" -> ProviderKind.NETWORK_FTP.name
                        "sftp" -> ProviderKind.NETWORK_SFTP.name
                        "smb" -> ProviderKind.NETWORK_SMB.name
                        else -> ""
                    }

                    val conn = appConfigs.networkConnections.find { it.id == connId }
                    if (conn != null) {
                        val provider = StorageProviders.network(conn)
                        scanRemoteFolder(context, provider, path, kindName, connId, newAlbums, newSongs, retriever, coversFolder)
                    }
                }
                folderKey.startsWith("content://") -> {
                    scanRemoteFolder(context, SafProvider, folderKey, ProviderKind.SAF.name, null, newAlbums, newSongs, retriever, coversFolder)
                }
                else -> {
                    val rootFolder = File(folderKey)
                    if (rootFolder.exists() && rootFolder.isDirectory) {
                        scanLocalFolder(rootFolder, newAlbums, newSongs, retriever, coversFolder)
                    }
                }
            }
        }

        metadataMutex.withLock {
            albums = newAlbums
            songs = newSongs
            saveMetadata(context)
        }
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanLocalFolder(
        folder: File,
        albumList: MutableList<MusicAlbumMetadata>,
        songList: MutableList<MusicSongMetadata>,
        retriever: MediaMetadataRetriever,
        coversFolder: File
    ) {
        val files = folder.listFiles() ?: return
        val audioFiles = files.filter { it.isFile && isAudioFile(it.name) }

        if (audioFiles.isNotEmpty()) {
            val albumsInFolder = mutableMapOf<String, MutableList<File>>()

            audioFiles.forEach { file ->
                try {
                    retriever.setDataSource(file.absolutePath)
                    extractMetadataAndAddSong(file.name, file.absolutePath, ProviderKind.LOCAL.name, null, songList, retriever, albumsInFolder, folder.name)
                } catch (e: Exception) {
                    addFallbackSong(file.name, file.absolutePath, ProviderKind.LOCAL.name, null, songList, albumsInFolder, folder.name)
                }
            }

            processAlbumsInFolder(albumsInFolder, ProviderKind.LOCAL.name, null, albumList, retriever, coversFolder)
        }

        files.filter { it.isDirectory }.forEach {
            scanLocalFolder(it, albumList, songList, retriever, coversFolder)
        }
    }

    private suspend fun scanRemoteFolder(
        context: Context,
        provider: StorageProvider,
        parentId: String,
        kindName: String,
        connectionId: String?,
        albumList: MutableList<MusicAlbumMetadata>,
        songList: MutableList<MusicSongMetadata>,
        retriever: MediaMetadataRetriever,
        coversFolder: File
    ) {
        val children = try { provider.listChildren(parentId) } catch (e: Exception) { emptyList() }
        val audioFiles = children.filter { !it.isDirectory && isAudioFile(it.name) }

        if (audioFiles.isNotEmpty()) {
            val albumsInFolder = mutableMapOf<String, MutableList<String>>()

            audioFiles.forEach { file ->
                try {
                    retriever.setDataSource(UniversalMediaDataSource(provider, file.providerId, file.length))
                    
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name.substringBeforeLast('.')
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: provider.displayName(parentId)
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()

                    songList.add(MusicSongMetadata(
                        title = title, artist = artist, album = album, filePath = file.providerId,
                        duration = duration, trackNumber = trackNumber, lastSync = System.currentTimeMillis(),
                        providerKind = kindName, connectionId = connectionId
                    ))
                    albumsInFolder.getOrPut(album) { mutableListOf() }.add(file.providerId)
                } catch (e: Exception) {
                    val albumTag = provider.displayName(parentId)
                    songList.add(MusicSongMetadata(
                        title = file.name, artist = null, album = albumTag, filePath = file.providerId,
                        duration = 0, trackNumber = null, lastSync = System.currentTimeMillis(),
                        providerKind = kindName, connectionId = connectionId
                    ))
                    albumsInFolder.getOrPut(albumTag) { mutableListOf() }.add(file.providerId)
                }
            }

            albumsInFolder.forEach { (albumName, idsInAlbum) ->
                var artist: String? = null
                var coverPath: String? = null
                try {
                    val firstId = idsInAlbum.first()
                    val firstFile = audioFiles.find { it.providerId == firstId }
                    if (firstFile != null) {
                        retriever.setDataSource(UniversalMediaDataSource(provider, firstId, firstFile.length))
                        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val embeddedPicture = retriever.embeddedPicture
                        if (embeddedPicture != null) {
                            val coverFile = File(coversFolder, "${albumName.hashCode()}.jpg")
                            if (!coverFile.exists()) coverFile.writeBytes(embeddedPicture)
                            coverPath = coverFile.absolutePath
                        }
                    }
                } catch (e: Exception) {}

                albumList.add(MusicAlbumMetadata(
                    albumName = albumName, artistName = artist, folderPath = parentId,
                    trackCount = idsInAlbum.size, lastSync = System.currentTimeMillis(),
                    coverPath = coverPath, providerKind = kindName, connectionId = connectionId
                ))
            }
        }

        children.filter { it.isDirectory }.forEach {
            scanRemoteFolder(context, provider, it.providerId, kindName, connectionId, albumList, songList, retriever, coversFolder)
        }
    }

    private fun extractMetadataAndAddSong(
        fileName: String, filePath: String, kind: String, connId: String?,
        songList: MutableList<MusicSongMetadata>, retriever: MediaMetadataRetriever,
        albumsInFolder: MutableMap<String, MutableList<File>>, defaultAlbum: String
    ) {
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: fileName.substringBeforeLast('.')
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: defaultAlbum
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()

        songList.add(MusicSongMetadata(
            title = title, artist = artist, album = album, filePath = filePath,
            duration = duration, trackNumber = trackNumber, lastSync = System.currentTimeMillis(),
            providerKind = kind, connectionId = connId
        ))
        albumsInFolder.getOrPut(album) { mutableListOf() }.add(File(filePath))
    }

    private fun addFallbackSong(
        fileName: String, filePath: String, kind: String, connId: String?,
        songList: MutableList<MusicSongMetadata>, albumsInFolder: MutableMap<String, MutableList<File>>, defaultAlbum: String
    ) {
        songList.add(MusicSongMetadata(
            title = fileName, artist = null, album = defaultAlbum, filePath = filePath,
            duration = 0, trackNumber = null, lastSync = System.currentTimeMillis(),
            providerKind = kind, connectionId = connId
        ))
        albumsInFolder.getOrPut(defaultAlbum) { mutableListOf() }.add(File(filePath))
    }

    private fun processAlbumsInFolder(
        albumsInFolder: Map<String, List<File>>, kind: String, connId: String?,
        albumList: MutableList<MusicAlbumMetadata>, retriever: MediaMetadataRetriever, coversFolder: File
    ) {
        albumsInFolder.forEach { (albumName, filesInAlbum) ->
            val firstFile = filesInAlbum.first()
            var artist: String? = null
            var coverPath: String? = null
            try {
                retriever.setDataSource(firstFile.absolutePath)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val embeddedPicture = retriever.embeddedPicture
                if (embeddedPicture != null) {
                    val coverFile = File(coversFolder, "${albumName.hashCode()}.jpg")
                    if (!coverFile.exists()) coverFile.writeBytes(embeddedPicture)
                    coverPath = coverFile.absolutePath
                }
            } catch (e: Exception) { }

            albumList.add(MusicAlbumMetadata(
                albumName = albumName, artistName = artist, folderPath = firstFile.parent ?: "",
                trackCount = filesInAlbum.size, lastSync = System.currentTimeMillis(),
                coverPath = coverPath, providerKind = kind, connectionId = connId
            ))
        }
    }

    private fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "m4a", "ogg", "wav", "flac", "aac")
    }

    fun getCoverPath(albumName: String, context: Context? = null): String? {
        val album = albums.find { it.albumName == albumName }
        if (album?.coverPath != null) return album.coverPath
        
        if (context != null) {
            val coversFolder = getCoversFolder(context)
            val coverFile = File(coversFolder, "${albumName.hashCode()}.jpg")
            if (coverFile.exists()) return coverFile.absolutePath
        }
        return null
    }

    fun getCoverPathForSong(context: Context, file: UniversalFile): String? {
        val song = songs.find { it.filePath == file.providerId }
        if (song != null) {
            return song.album?.let { getCoverPath(it, context) }
        }
        return null
    }

    fun getSongsForAlbum(context: Context, albumName: String): List<UniversalFile> {
        if (songs.isEmpty()) {
            loadMetadata(context)
        }
        return songs.filter { it.album?.equals(albumName, ignoreCase = true) == true }.map { song ->
            val provider = getProvider(context, song.providerKind, song.connectionId)
            UniversalFile(
                name = song.title, isDirectory = false, lastModified = song.lastSync,
                length = 0L, provider = provider, providerId = song.filePath,
                parentId = provider.parentId(song.filePath), mimeType = "audio/*"
            )
        }
    }

    suspend fun extractAndCacheMetadata(context: Context, file: UniversalFile): MusicSongMetadata? {
        val retriever = MediaMetadataRetriever()
        val coversFolder = getCoversFolder(context)
        return try {
            retriever.setDataSource(UniversalMediaDataSource(file.provider, file.providerId, file.length))
            
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: file.provider.displayName(file.parentId ?: "")
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()

            val song = MusicSongMetadata(
                title = title, artist = artist, album = album, filePath = file.providerId,
                duration = duration, trackNumber = trackNumber, lastSync = System.currentTimeMillis(),
                providerKind = file.provider.kind.name, connectionId = file.provider.connectionId
            )

            metadataMutex.withLock {
                // Update in-memory lists
                val updatedSongs = songs.toMutableList()
                updatedSongs.removeAll { it.filePath == file.providerId }
                updatedSongs.add(song)
                songs = updatedSongs

                // Handle album and cover
                val existingAlbum = albums.find { it.albumName == album }
                if (existingAlbum == null) {
                    var coverPath: String? = null
                    val embeddedPicture = retriever.embeddedPicture
                    if (embeddedPicture != null) {
                        val coverFile = File(coversFolder, "${album.hashCode()}.jpg")
                        if (!coverFile.exists()) coverFile.writeBytes(embeddedPicture)
                        coverPath = coverFile.absolutePath
                    }

                    val newAlbum = MusicAlbumMetadata(
                        albumName = album, artistName = artist, folderPath = file.parentId ?: "",
                        trackCount = 1, lastSync = System.currentTimeMillis(),
                        coverPath = coverPath, providerKind = file.provider.kind.name,
                        connectionId = file.provider.connectionId
                    )
                    albums = albums + newAlbum
                }
                saveMetadata(context)
            }
            song
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }
}
