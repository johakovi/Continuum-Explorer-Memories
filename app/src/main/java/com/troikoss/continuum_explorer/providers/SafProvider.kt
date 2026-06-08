package com.troikoss.continuum_explorer.providers

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import com.troikoss.continuum_explorer.model.FileMetadata
import com.troikoss.continuum_explorer.model.ProviderCapabilities
import com.troikoss.continuum_explorer.model.ProviderKind
import com.troikoss.continuum_explorer.model.StorageProvider
import com.troikoss.continuum_explorer.model.UniversalFile
import java.io.InputStream
import java.io.OutputStream

object SafProvider : StorageProvider {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun appContext(): Context = appContext

    override val kind = ProviderKind.SAF
    override val capabilities = ProviderCapabilities(
        canWrite = true,
        canRename = true,
        canDelete = true,
        canRandomAccessRead = true,
        canGetShareableUri = true,
        supportsChildEnumeration = true,
        isRemote = false,
    )

    private fun docFromId(id: String): DocumentFile? =
        DocumentFile.fromTreeUri(appContext, Uri.parse(id))
            ?: DocumentFile.fromSingleUri(appContext, Uri.parse(id))

    override fun rootId() = ""

    override fun parentId(childId: String): String? =
        docFromId(childId)?.parentFile?.uri?.toString()

    override fun displayName(id: String) =
        docFromId(id)?.name ?: Uri.parse(id).lastPathSegment ?: id

    override fun exists(id: String) = docFromId(id)?.exists() == true

    override suspend fun listChildren(id: String): List<UniversalFile> {
        val doc = docFromId(id) ?: return emptyList()
        return doc.listFiles().map { it.toUniversalFile() }
    }

    override fun getMetadata(id: String): FileMetadata {
        val doc = docFromId(id)
        return FileMetadata(
            size = doc?.length() ?: 0L,
            lastModified = doc?.lastModified() ?: 0L,
            isDirectory = doc?.isDirectory == true,
            mimeType = doc?.type,
        )
    }

    override fun findChild(parentId: String, name: String): UniversalFile? {
        val doc = docFromId(parentId) ?: return null
        return doc.findFile(name)?.toUniversalFile()
    }

    override fun openInput(id: String): InputStream =
        appContext.contentResolver.openInputStream(Uri.parse(id))!!

    override fun openReadFd(id: String): ParcelFileDescriptor? =
        appContext.contentResolver.openFileDescriptor(Uri.parse(id), "r")

    override fun getShareableUri(id: String): Uri = Uri.parse(id)

    override fun createChild(parentId: String, name: String, isDirectory: Boolean): UniversalFile {
        val parent = docFromId(parentId)!!
        val doc = if (isDirectory) parent.createDirectory(name)!! else parent.createFile("application/octet-stream", name)!!
        return doc.toUniversalFile()
    }

    override fun createAndOpenOutput(parentId: String, name: String): Pair<UniversalFile, OutputStream> {
        val parent = docFromId(parentId)!!
        val doc = parent.createFile("application/octet-stream", name)!!
        val stream = appContext.contentResolver.openOutputStream(doc.uri)!!
        return doc.toUniversalFile() to stream
    }

    override fun delete(id: String): Boolean = docFromId(id)?.delete() == true

    override fun rename(id: String, newName: String): UniversalFile? {
        val doc = docFromId(id) ?: return null
        return if (doc.renameTo(newName)) docFromId(id)?.toUniversalFile() else null
    }

    override fun move(id: String, destParentId: String, destName: String): UniversalFile? {
        val sourceDoc = docFromId(id) ?: return null
        val sourceParentUri = sourceDoc.parentFile?.uri ?: Uri.parse(parentId(id) ?: return null)
        val destParentDoc = docFromId(destParentId) ?: return null

        return try {
            val resultUri = android.provider.DocumentsContract.moveDocument(
                appContext.contentResolver,
                sourceDoc.uri,
                sourceParentUri,
                destParentDoc.uri
            )
            if (resultUri != null) {
                val finalDoc = DocumentFile.fromSingleUri(appContext, resultUri)
                if (finalDoc?.name != destName) {
                    finalDoc?.renameTo(destName)
                }
                docFromId(resultUri.toString())?.toUniversalFile()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun getDiskInfo(uri: Uri): Pair<Long, Long>? {
        return try {
            val authority = uri.authority ?: return null
            val rootId = if (android.provider.DocumentsContract.isTreeUri(uri)) {
                android.provider.DocumentsContract.getTreeDocumentId(uri).split(":")[0]
            } else {
                android.provider.DocumentsContract.getRootId(uri)
            }
            val rootUri = android.provider.DocumentsContract.buildRootUri(authority, rootId)
            appContext.contentResolver.query(
                rootUri,
                arrayOf(
                    android.provider.DocumentsContract.Root.COLUMN_CAPACITY_BYTES,
                    android.provider.DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val capacityIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Root.COLUMN_CAPACITY_BYTES)
                    val availableIndex = cursor.getColumnIndex(android.provider.DocumentsContract.Root.COLUMN_AVAILABLE_BYTES)
                    if (capacityIndex != -1 && availableIndex != -1) {
                        val total = cursor.getLong(capacityIndex)
                        val available = cursor.getLong(availableIndex)
                        if (total > 0) total to available else null
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun DocumentFile.toUniversalFile(): UniversalFile = UniversalFile(
        name = this.name ?: "Unknown",
        isDirectory = this.isDirectory,
        lastModified = this.lastModified(),
        length = this.length(),
        provider = SafProvider,
        providerId = this.uri.toString(),
        parentId = this.parentFile?.uri?.toString(),
        mimeType = this.type,
    )
}
