package ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class TextEditorActivity : AppCompatActivity() {
    private lateinit var editText: EditText
    private lateinit var readOnlyTab: Button
    private lateinit var readWriteTab: Button
    private var filePath: String = ""
    private var isReadOnly: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_editor)

        // Initialize views
        editText = findViewById(R.id.editText)
        readOnlyTab = findViewById(R.id.readOnlyTab)
        readWriteTab = findViewById(R.id.readWriteTab)

        // Get file path and read-only flag from intent
        filePath = intent.getStringExtra("FILE_PATH") ?: ""
        isReadOnly = intent.getBooleanExtra("IS_READ_ONLY", true)

        // Load file content
        loadFileContent()

        // Set initial mode
        setReadOnlyMode(isReadOnly)

        // Tab click listeners
        readOnlyTab.setOnClickListener { setReadOnlyMode(true) }
        readWriteTab.setOnClickListener { setReadOnlyMode(false) }
    }

    private fun loadFileContent() {
        if (filePath.isNotEmpty()) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    editText.setText(file.readText())
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setReadOnlyMode(readOnly: Boolean) {
        isReadOnly = readOnly
        editText.isEnabled = !readOnly
        editText.isFocusable = !readOnly
        editText.isFocusableInTouchMode = !readOnly

        // Update tab selection
        readOnlyTab.isSelected = readOnly
        readWriteTab.isSelected = !readOnly
    }

    override fun onBackPressed() {
        // Save changes if in read-write mode
        if (!isReadOnly) {
            saveFileContent()
        }
        super.onBackPressed()
    }

    private fun saveFileContent() {
        if (filePath.isNotEmpty()) {
            try {
                val file = File(filePath)
                file.writeText(editText.text.toString())
                Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
