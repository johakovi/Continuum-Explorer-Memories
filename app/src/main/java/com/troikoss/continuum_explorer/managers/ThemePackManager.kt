package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class CustomThemeColors(
    val sidebarBackground: Color? = null,
    val topBarBackground: Color? = null,
    val navButtonBackground: Color? = null,
    val searchBoxBackground: Color? = null,
    val tabBarBackground: Color? = null,
    val selectionBackground: Color? = null,
    val sidebarIcons: Color? = null,
    val folderIcon: Color? = null,
    val galleryIcon: Color? = null,
    val recentIcon: Color? = null,
    val filesIcon: Color? = null,
    val documentsIcon: Color? = null,
    val gameIcon: Color? = null,
    val gameShortcutIcon: Color? = null,
    val recycleBinIcon: Color? = null,
    val downloadsIcon: Color? = null,
    val zipIcon: Color? = null,
    val pdfIcon: Color? = null,
    val xlsIcon: Color? = null,
    val docxIcon: Color? = null,
    val txtIcon: Color? = null,
    val terminalIcon: Color? = null,
    val tabActiveBackground: Color? = null,
    val textColor: Color? = null,
    val menuBackground: Color? = null,
    val fileViewBackground: Color? = null,
    val background: Color? = null,
    val commandPanelBackground: Color? = null
)

data class ThemePack(
    val name: String,
    val lightColors: CustomThemeColors,
    val darkColors: CustomThemeColors,
    val iconDir: File?
)

object ThemePackManager {
    private val _currentPack = mutableStateOf<ThemePack?>(null)
    val currentPack: State<ThemePack?> = _currentPack

    private val iconCache = mutableMapOf<String, Bitmap>()

    fun init(context: Context) {
        val packPath = context.getSharedPreferences("explorer_settings", Context.MODE_PRIVATE)
            .getString("active_theme_pack", null)
        if (packPath != null) {
            val file = File(packPath)
            if (file.exists()) {
                loadPack(context, file)
            }
        }
    }

    fun loadPack(context: Context, zipFile: File): Boolean {
        try {
            val extractDir = File(context.filesDir, "active_theme")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val outFile = File(extractDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            val themeJsonFile = findFile(extractDir, "theme.json")
            if (themeJsonFile == null) return false

            val themeRoot = themeJsonFile.parentFile ?: extractDir
            val json = JSONObject(themeJsonFile.readText())
            val name = json.optString("name", "Custom Theme")
            
            fun parseColors(obj: JSONObject?): CustomThemeColors {
                if (obj == null) return CustomThemeColors()
                fun getC(key: String): Color? = if (obj.has(key)) obj.getString(key).toColor() else null
                
                return CustomThemeColors(
                    sidebarBackground = getC("sidebarBackground"),
                    topBarBackground = getC("topBarBackground"),
                    navButtonBackground = getC("navButtonBackground"),
                    searchBoxBackground = getC("searchBoxBackground"),
                    tabBarBackground = getC("tabBarBackground"),
                    selectionBackground = getC("selectionBackground"),
                    sidebarIcons = getC("sidebarIcons"),
                    folderIcon = getC("folderIcon"),
                    galleryIcon = getC("galleryIcon"),
                    recentIcon = getC("recentIcon"),
                    filesIcon = getC("filesIcon"),
                    documentsIcon = getC("documentsIcon"),
                    gameIcon = getC("gameIcon"),
                    gameShortcutIcon = getC("gameShortcutIcon"),
                    recycleBinIcon = getC("recycleBinIcon"),
                    downloadsIcon = getC("downloadsIcon"),
                    zipIcon = getC("zipIcon"),
                    pdfIcon = getC("pdfIcon"),
                    xlsIcon = getC("xlsIcon"),
                    docxIcon = getC("docxIcon"),
                    txtIcon = getC("txtIcon"),
                    terminalIcon = getC("terminalIcon"),
                    tabActiveBackground = getC("tabActiveBackground"),
                    textColor = getC("textColor"),
                    menuBackground = getC("menuBackground"),
                    fileViewBackground = getC("fileViewBackground"),
                    background = getC("background"),
                    commandPanelBackground = getC("commandPanelBackground")
                )
            }

            val lightColors = parseColors(json.optJSONObject("light"))
            val darkColors = parseColors(json.optJSONObject("dark"))
            val iconDir = File(themeRoot, "icons").let { if (it.exists()) it else File(themeRoot, "Icons") }

            _currentPack.value = ThemePack(name, lightColors, darkColors, if (iconDir.exists()) iconDir else null)
            iconCache.clear()
            
            context.getSharedPreferences("explorer_settings", Context.MODE_PRIVATE)
                .edit().putString("active_theme_pack", zipFile.absolutePath).apply()
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun findFile(dir: File, name: String): File? {
        val rootFile = File(dir, name)
        if (rootFile.exists()) return rootFile
        
        dir.listFiles()?.forEach { 
            if (it.isDirectory) {
                val found = findFile(it, name)
                if (found != null) return found
            }
        }
        return null
    }

    fun clearPack(context: Context) {
        _currentPack.value = null
        iconCache.clear()
        context.getSharedPreferences("explorer_settings", Context.MODE_PRIVATE)
            .edit().remove("active_theme_pack").apply()
        File(context.filesDir, "active_theme").deleteRecursively()
    }

    fun getCustomIcon(name: String): Bitmap? {
        val pack = _currentPack.value ?: return null
        val iconDir = pack.iconDir ?: return null
        
        if (iconCache.containsKey(name)) return iconCache[name]

        val extensions = listOf(".png", ".PNG", ".webp", ".WEBP", ".jpg", ".JPG", ".jpeg", ".JPEG")
        for (ext in extensions) {
            val iconFile = File(iconDir, name + ext)
            if (iconFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                if (bitmap != null) {
                    iconCache[name] = bitmap
                    return bitmap
                }
            }
        }
        return null
    }

    private fun String.toColor(): Color? {
        return try {
            if (startsWith("#")) {
                Color(android.graphics.Color.parseColor(this))
            } else null
        } catch (_: Exception) { null }
    }
}
