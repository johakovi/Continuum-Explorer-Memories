package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.media.MediaMetadataRetriever
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class MusicSongMetadata(
    val title: String,
    val artist: String?,
    val album: String?,
    val filePath: String,
    val duration: Long,
    val trackNumber: Int?,
    val lastSync: Long
)

data class MusicAlbumMetadata(
    val albumName: String,
    val artistName: String?,
    val folderPath: String,
    val trackCount: Int,
    val lastSync: Long,
    val coverPath: String? = null
)

object MusicMetadataManager {
    private const val METADATA_FOLDER = "music_metadata"
    private const val METADATA_FILE = "metadata.json"
    private const val SONGS_FILE = "songs.json"
    private const val FAVOURITES_FILE = "favourites.json"
    private const val COVERS_FOLDER = "covers"

    private var albums: List<MusicAlbumMetadata> = emptyList()
    private var songs: List<MusicSongMetadata> = emptyList()
    private var favouritePaths: MutableSet<String> = mutableSetOf()

    fun init(context: Context) {
        loadMetadata(context)
        loadFavourites(context)
    }

    private fun loadFavourites(context: Context) {
        val file = File(getMetadataFolder(context), FAVOURITES_FILE)
        if (file.exists()) {
            try {
                val jsonArray = JSONArray(file.readText())
                favouritePaths.clear()
                for (i in 0 until jsonArray.length()) {
                    favouritePaths.add(jsonArray.getString(i))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveFavourites(context: Context) {
        try {
            val jsonArray = JSONArray()
            favouritePaths.forEach { jsonArray.put(it) }
            File(getMetadataFolder(context), FAVOURITES_FILE).writeText(jsonArray.toString())
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun isFavourite(path: String): Boolean = favouritePaths.contains(path)

    fun toggleFavourite(context: Context, path: String) {
        if (favouritePaths.contains(path)) {
            favouritePaths.remove(path)
        } else {
            favouritePaths.add(path)
        }
        saveFavourites(context)
    }

    fun getFavourites(context: Context): List<UniversalFile> {
        if (songs.isEmpty()) {
            loadMetadata(context)
        }
        return songs.filter { favouritePaths.contains(it.filePath) }.map { song ->
            val file = File(song.filePath)
            UniversalFile(
                name = song.title,
                isDirectory = false,
                lastModified = song.lastSync,
                length = file.length(),
                provider = LocalProvider,
                providerId = song.filePath,
                parentId = file.parentFile?.absolutePath,
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
                            artistName = if (obj.isNull("artistName")) null else obj.getString("artistName"),
                            folderPath = obj.getString("folderPath"),
                            trackCount = obj.getInt("trackCount"),
                            lastSync = obj.getLong("lastSync"),
                            coverPath = if (obj.isNull("coverPath")) null else obj.getString("coverPath")
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
                            artist = if (obj.isNull("artist")) null else obj.getString("artist"),
                            album = if (obj.isNull("album")) null else obj.getString("album"),
                            filePath = obj.getString("filePath"),
                            duration = obj.getLong("duration"),
                            trackNumber = if (obj.isNull("trackNumber")) null else obj.getInt("trackNumber"),
                            lastSync = obj.getLong("lastSync")
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
            UniversalFile(
                name = album.albumName,
                isDirectory = true,
                lastModified = album.lastSync,
                length = album.trackCount.toLong(),
                provider = LocalProvider,
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
            val file = File(song.filePath)
            UniversalFile(
                name = song.title,
                isDirectory = false,
                lastModified = song.lastSync,
                length = file.length(),
                provider = LocalProvider,
                providerId = song.filePath,
                parentId = file.parentFile?.absolutePath,
                mimeType = "audio/*"
            )
        }
    }

    fun sync(context: Context, musicFolders: Set<String>) {
        val newAlbums = mutableListOf<MusicAlbumMetadata>()
        val newSongs = mutableListOf<MusicSongMetadata>()
        val retriever = MediaMetadataRetriever()
        val coversFolder = getCoversFolder(context)

        musicFolders.forEach { folderPath ->
            val rootFolder = File(folderPath)
            if (rootFolder.exists() && rootFolder.isDirectory) {
                scanFolder(rootFolder, newAlbums, newSongs, retriever, coversFolder)
            }
        }

        albums = newAlbums
        songs = newSongs
        saveMetadata(context)
        try {
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanFolder(
        folder: File,
        albumList: MutableList<MusicAlbumMetadata>,
        songList: MutableList<MusicSongMetadata>,
        retriever: MediaMetadataRetriever,
        coversFolder: File
    ) {
        val files = folder.listFiles() ?: return
        val audioFiles = files.filter { it.isFile && isAudioFile(it) }

        if (audioFiles.isNotEmpty()) {
            val albumsInFolder = mutableMapOf<String, MutableList<File>>()

            audioFiles.forEach { file ->
                try {
                    retriever.setDataSource(file.absolutePath)
                    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
                    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: folder.name
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                    val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull()

                    songList.add(MusicSongMetadata(
                        title = title,
                        artist = artist,
                        album = album,
                        filePath = file.absolutePath,
                        duration = duration,
                        trackNumber = trackNumber,
                        lastSync = System.currentTimeMillis()
                    ))

                    albumsInFolder.getOrPut(album) { mutableListOf() }.add(file)
                } catch (e: Exception) {
                    val albumTag = folder.name
                    albumsInFolder.getOrPut(albumTag) { mutableListOf() }.add(file)
                    songList.add(MusicSongMetadata(
                        title = file.name,
                        artist = null,
                        album = albumTag,
                        filePath = file.absolutePath,
                        duration = 0,
                        trackNumber = null,
                        lastSync = System.currentTimeMillis()
                    ))
                }
            }

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
                        if (!coverFile.exists()) {
                            coverFile.writeBytes(embeddedPicture)
                        }
                        coverPath = coverFile.absolutePath
                    }
                } catch (e: Exception) { }

                albumList.add(
                    MusicAlbumMetadata(
                        albumName = albumName,
                        artistName = artist,
                        folderPath = folder.absolutePath,
                        trackCount = filesInAlbum.size,
                        lastSync = System.currentTimeMillis(),
                        coverPath = coverPath
                    )
                )
            }
        }

        files.filter { it.isDirectory }.forEach {
            scanFolder(it, albumList, songList, retriever, coversFolder)
        }
    }

    private fun isAudioFile(file: File): Boolean {
        val ext = file.extension.lowercase()
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
        val filePath = file.providerId
        val song = songs.find { it.filePath == filePath }
        if (song != null) {
            return song.album?.let { getCoverPath(it, context) }
        }

        // Fallback for files not in synced metadata: try to extract album name and check covers folder
        if (file.provider.kind == com.troikoss.continuum_explorer.model.ProviderKind.LOCAL || 
            file.provider.kind == com.troikoss.continuum_explorer.model.ProviderKind.SHIZUKU) {
            val actualPath = file.providerId
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(actualPath)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                if (album != null) {
                    val coversFolder = getCoversFolder(context)
                    val coverFile = File(coversFolder, "${album.hashCode()}.jpg")
                    if (coverFile.exists()) {
                        retriever.release()
                        return coverFile.absolutePath
                    }

                    val embeddedPicture = retriever.embeddedPicture
                    if (embeddedPicture != null) {
                        coverFile.writeBytes(embeddedPicture)
                        retriever.release()
                        return coverFile.absolutePath
                    }
                }
                retriever.release()
            } catch (e: Exception) { }
        }
        return null
    }

    fun getSongsForAlbum(context: Context, albumName: String): List<UniversalFile> {
        if (songs.isEmpty()) {
            loadMetadata(context)
        }
        return songs.filter { it.album?.equals(albumName, ignoreCase = true) == true }.map { song ->
            val file = File(song.filePath)
            UniversalFile(
                name = song.title,
                isDirectory = false,
                lastModified = song.lastSync,
                length = file.length(),
                provider = LocalProvider,
                providerId = song.filePath,
                parentId = file.parentFile?.absolutePath,
                mimeType = "audio/*"
            )
        }
    }
}
