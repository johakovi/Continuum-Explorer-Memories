package com.troikoss.continuum_explorer.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.troikoss.continuum_explorer.ui.components.PdfViewerScreen
import com.troikoss.continuum_explorer.ui.theme.FileExplorerTheme

class PdfViewerActivity : FragmentActivity() {

    private var currentUri by mutableStateOf<Uri?>(null)
    private var onSingleTapListener: ((Float, Float) -> Unit)? = null

    private val gestureDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTapListener?.invoke(e.x, e.y)
                return false
            }
        })
    }

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
                        onBackClick = { finish() },
                        onRegisterSingleTapListener = { listener ->
                            onSingleTapListener = listener
                        }
                    )
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        try {
            gestureDetector.onTouchEvent(ev)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent.data ?: return
        currentUri = uri
    }

    override fun onDestroy() {
        super.onDestroy()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}
