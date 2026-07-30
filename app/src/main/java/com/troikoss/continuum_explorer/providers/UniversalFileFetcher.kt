package com.troikoss.continuum_explorer.providers

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.troikoss.continuum_explorer.managers.MusicMetadataManager
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.utils.DiskCache
import okio.buffer
import okio.source
import okio.Path.Companion.toPath

class UniversalFileFetcher(
    private val file: UniversalFile,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        // 1. Check for Audio/Album covers first (they are already local)
        val isAudio = file.mimeType?.startsWith("audio/") == true || 
                listOf(".mp3", ".wav", ".ogg", ".m4a", ".flac").any { file.name.lowercase().endsWith(it) }

        if (file.mimeType == "album") {
            val coverPath = MusicMetadataManager.getCoverPath(file.name, options.context)
            if (coverPath != null) {
                val coverFile = java.io.File(coverPath)
                if (coverFile.exists()) {
                    return SourceResult(
                        source = ImageSource(file = coverFile.absolutePath.toPath(), fileSystem = okio.FileSystem.SYSTEM),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.DISK
                    )
                }
            }
        } else if (isAudio) {
            var coverPath = MusicMetadataManager.getCoverPathForSong(options.context, file)
            
            // Incremental caching: if remote and not in cache, try to extract it now
            if (coverPath == null && file.provider.capabilities.isRemote) {
                MusicMetadataManager.extractAndCacheMetadata(options.context, file)
                coverPath = MusicMetadataManager.getCoverPathForSong(options.context, file)
            }

            if (coverPath != null) {
                val coverFile = java.io.File(coverPath)
                if (coverFile.exists()) {
                    return SourceResult(
                        source = ImageSource(file = coverFile.absolutePath.toPath(), fileSystem = okio.FileSystem.SYSTEM),
                        mimeType = "image/jpeg",
                        dataSource = DataSource.DISK
                    )
                }
            }
        }

        // 2. Check persistent DiskCache (for remote files and complex types like PDF/APK)
        val thumbFile = DiskCache.getCacheFile(options.context, file)
        if (thumbFile.exists()) {
            return SourceResult(
                source = ImageSource(file = thumbFile.absolutePath.toPath(), fileSystem = okio.FileSystem.SYSTEM),
                mimeType = "image/png",
                dataSource = DataSource.DISK
            )
        }

        // 3. Fallback to downloading from provider
        val source = ImageSource(
            source = file.provider.openInput(file.providerId).source().buffer(),
            context = options.context,
        )

        return SourceResult(
            source = source,
            mimeType = file.mimeType,
            dataSource = if (file.provider.capabilities.isRemote) DataSource.NETWORK else DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<UniversalFile> {
        override fun create(data: UniversalFile, options: Options, imageLoader: ImageLoader): Fetcher =
            UniversalFileFetcher(data, options)
    }
}
