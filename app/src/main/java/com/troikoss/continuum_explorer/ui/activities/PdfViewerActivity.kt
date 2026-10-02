package com.troikoss.continuum_explorer.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.troikoss.continuum_explorer.ui.components.PdfViewerScreen
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme

class PdfViewerActivity : FragmentActivity() {

    private var currentUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val uri = intent.data
        if (uri == null) {
            Toast.makeText(this, "No PDF file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUri = uri

        setContent {
            FileExplorerTheme {
                val pdfUri = currentUri
                if (pdfUri != null) {
                    PdfViewerScreen(
                        uri = pdfUri,
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent.data ?: return
        currentUri = uri
    }
}
