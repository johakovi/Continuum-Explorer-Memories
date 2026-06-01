package com.troikoss.continuum_explorer.ui.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.troikoss.continuum_explorer.R

class PdfViewerActivity : AppCompatActivity() {

    private var pdfViewerFragment: PdfViewerFragment? = null
    private var isSearchActive = false
    private var pendingUri: android.net.Uri? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val searchSyncRunnable = object : Runnable {
        override fun run() {
            pdfViewerFragment?.let { fragment ->
                if (isSearchActive && !fragment.isTextSearchActive) {
                    // Search was closed via library UI (e.g. library's X button)
                    updateSearchUi(false)
                } else if (isSearchActive) {
                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val root = findViewById<ViewGroup>(R.id.main_root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.fab_search)
        val container = findViewById<ViewGroup>(R.id.pdf_container)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = systemBars.bottom + (16 * resources.displayMetrics.density).toInt()
                rightMargin = systemBars.right + (16 * resources.displayMetrics.density).toInt()
            }
            container.updatePadding(bottom = systemBars.bottom)
            windowInsets
        }

        val uri = intent.data
        if (uri == null) {
            Toast.makeText(this, "No PDF file specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        pendingUri = uri

        supportActionBar?.title = uri.lastPathSegment ?: "PDF Viewer"

        if (savedInstanceState == null) {
            val fragment = PdfViewerFragment()
            pdfViewerFragment = fragment
            
            supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                    if (f === fragment) {
                        try {
                            fragment.documentUri = uri
                            pendingUri = null
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        fm.unregisterFragmentLifecycleCallbacks(this)
                    }
                }
            }, false)

            supportFragmentManager.beginTransaction()
                .replace(R.id.pdf_container, fragment, "pdf_fragment")
                .commit()
        } else {
            pdfViewerFragment = supportFragmentManager.findFragmentByTag("pdf_fragment") as? PdfViewerFragment
            // Restore state
            isSearchActive = pdfViewerFragment?.isTextSearchActive ?: false
            if (isSearchActive) {
                updateSearchUi(true)
            }
        }

        fab.setOnClickListener {
            updateSearchUi(true)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent.data ?: return
        pendingUri = uri
        supportActionBar?.title = uri.lastPathSegment ?: "PDF Viewer"
        
        pdfViewerFragment?.let { fragment ->
            if (fragment.isAdded && fragment.context != null) {
                try {
                    fragment.documentUri = uri
                    pendingUri = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val uri = pendingUri ?: return
        val fragment = pdfViewerFragment ?: return
        
        if (fragment.isAdded && fragment.context != null) {
            try {
                fragment.documentUri = uri
                pendingUri = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pdf_viewer, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_close_search)?.isVisible = isSearchActive
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isSearchActive) {
                    updateSearchUi(false)
                } else {
                    finish()
                }
                return true
            }
            R.id.action_close_search -> {
                updateSearchUi(false)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateSearchUi(active: Boolean) {
        isSearchActive = active
        pdfViewerFragment?.let { fragment ->
            if (fragment.isTextSearchActive != active) {
                fragment.isTextSearchActive = active
            }
        }
        
        val fab = findViewById<FloatingActionButton>(R.id.fab_search)
        val appBar = findViewById<AppBarLayout>(R.id.app_bar)
        
        if (active) {
            fab.hide()
            appBar.visibility = View.GONE
            // Start polling to detect if the user closes search via library UI
            handler.removeCallbacks(searchSyncRunnable)
            handler.postDelayed(searchSyncRunnable, 500)
        } else {
            fab.show()
            appBar.visibility = View.VISIBLE
            handler.removeCallbacks(searchSyncRunnable)
        }
        
        invalidateOptionsMenu()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.isCtrlPressed == true) {
            when (keyCode) {
                KeyEvent.KEYCODE_F -> {
                    if (!isSearchActive) updateSearchUi(true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(searchSyncRunnable)
    }
}
