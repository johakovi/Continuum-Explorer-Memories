package com.troikoss.continuum_explorer.providers

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.troikoss.continuum_explorer.model.UniversalFile
import java.io.InputStream

@UnstableApi
class ProviderDataSource @OptIn(UnstableApi::class) constructor
    (
    private val file: UniversalFile,
) : BaseDataSource(file.provider.capabilities.isRemote) {

    private var stream: InputStream? = null
    private var bytesRemaining: Long = C.LENGTH_UNSET.toLong()
    private var uri: Uri = Uri.parse(file.providerId)

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        
        val inputStream = file.provider.openInput(file.providerId, dataSpec.position)
        stream = inputStream
        
        val totalLength = if (file.length > 0) file.length else C.LENGTH_UNSET.toLong()

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length
        else if (totalLength != C.LENGTH_UNSET.toLong()) totalLength - dataSpec.position
        else C.LENGTH_UNSET.toLong()
        
        val responseLength = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else bytesRemaining

        transferStarted(dataSpec)
        return responseLength
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) length
        else minOf(length.toLong(), bytesRemaining).toInt()
        
        val read = try {
            stream?.read(buffer, offset, toRead) ?: -1
        } catch (_: Exception) {
            -1
        }

        if (read == -1) return C.RESULT_END_OF_INPUT
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri = uri

    override fun close() {
        stream?.runCatching { close() }
        stream = null
        transferEnded()
    }

    class Factory(private val file: UniversalFile) : DataSource.Factory {
        override fun createDataSource(): DataSource = ProviderDataSource(file)
    }
}
