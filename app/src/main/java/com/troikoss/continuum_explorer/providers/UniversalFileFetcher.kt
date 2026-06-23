package com.troikoss.continuum_explorer.providers

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.utils.RemoteCache
import okio.buffer
import okio.source
import okio.Path.Companion.toPath

class UniversalFileFetcher(
    private val file: UniversalFile,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val useCache = file.provider.capabilities.isRemote
        val source = if (useCache) {
            val cachedFile = RemoteCache.cache(options.context, file)
            ImageSource(
                file = cachedFile.absolutePath.toPath(),
                fileSystem = okio.FileSystem.SYSTEM
            )
        } else {
            ImageSource(
                source = file.provider.openInput(file.providerId).source().buffer(),
                context = options.context,
            )
        }

        return SourceResult(
            source = source,
            mimeType = file.mimeType,
            dataSource = if (useCache) DataSource.NETWORK else DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<UniversalFile> {
        override fun create(data: UniversalFile, options: Options, imageLoader: ImageLoader): Fetcher =
            UniversalFileFetcher(data, options)
    }
}
