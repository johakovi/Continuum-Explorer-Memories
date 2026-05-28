package com.troikoss.continuum_explorer.ui.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme
import com.troikoss.continuum_explorer.utils.RestrictedCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class TextEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri == null) {
            Toast.makeText(this, "No file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            FileExplorerTheme {
                TextEditorScreen(uri, onExit = { finish() })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TextEditorScreen(uri: Uri, onExit: () -> Unit) {
        var text by remember { mutableStateOf("") }
        var originalText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(true) }
        var isSaving by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val originalPath = remember { intent.getStringExtra("originalPath") }
        val tempPath = remember { intent.getStringExtra("tempPath") }

        val fileName = remember(uri) {
            uri.lastPathSegment ?: "Unknown File"
        }

        // Load file content
        LaunchedEffect(uri) {
            withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            val content = reader.readText()
                            text = content
                            originalText = content
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TextEditorActivity, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isLoading = false
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(fileName, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (tempPath != null && text == originalText) {
                                // If it was a temp file and no changes, delete temp
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        File(tempPath).delete()
                                    } catch (_: Exception) {}
                                }
                            }
                            onExit()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                scope.launch {
                                    isSaving = true
                                    val success = saveFile(uri, text, originalPath, tempPath)
                                    if (success) {
                                        originalText = text
                                        Toast.makeText(this@TextEditorActivity, "File saved", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@TextEditorActivity, "Failed to save file", Toast.LENGTH_SHORT).show()
                                    }
                                    isSaving = false
                                }
                            }) {
                                Icon(Icons.Default.Save, contentDescription = "Save")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }

    private suspend fun saveFile(uri: Uri, content: String, originalPath: String? = null, tempPath: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val success = contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(content)
                        true
                    }
                } ?: false
                
                if (success && originalPath != null && tempPath != null) {
                    // If it was a restricted file, push it back using Shizuku
                    RestrictedCache.pushBack(this@TextEditorActivity, File(tempPath), originalPath)
                } else {
                    success
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
