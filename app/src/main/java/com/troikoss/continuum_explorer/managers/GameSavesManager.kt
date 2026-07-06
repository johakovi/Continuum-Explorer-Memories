package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.SafProvider
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import com.troikoss.continuum_explorer.utils.AppConfigurations
import com.troikoss.continuum_explorer.utils.IconHelper
import java.io.File

object GamesManager {
    private val games = listOf(
        GameSaveInfo("Endling", "Android/data/com.hg.endling/files/UE4Game/Endling/Endling/Saved/SaveGames"),
        GameSaveInfo("SENTINEL519", "Android/data/com.AppyBearStudio.SENTINEL519/files/"),
        GameSaveInfo("Stardew Valley", "Android/data/com.chucklefish.stardewvalley/files/Saves"),
        GameSaveInfo("PSP (savedata:PPSSPP)", "PPSSPP/PSP/SAVEDATA"),
        GameSaveInfo("PSP (savedata:PSP)", "PSP/PSP/SAVEDATA"),
        GameSaveInfo("PSP (savedata:Emulator)", "Documents/Emulator/PSP/SAVEDATA"),
        GameSaveInfo("PSP (data:Emulator)", "Documents/Emulator/PSP"),
        GameSaveInfo("PSP (data:PSP)", "PSP/PSP"),
        GameSaveInfo("PSP (data:PPSSPP)", "PPSSPP/PSP"),
        GameSaveInfo("Pocket City 2", "Android/data/com.codebrewgames.pocketcity2/files/pocketcity2"),
        GameSaveInfo("Easy Delivery Co.", "Android/data/com.doghowlgames.EasyDeliveryCo/files/"),
        GameSaveInfo("Wreckfest", "Android/data/com.hg.wreckfest/files/save"),
        GameSaveInfo("Vector", "Android/data/com.nekki.vector.paid/files"),
        GameSaveInfo("art of rally", "Android/data/com.noodlecake.artofrally/files/Save"),
        GameSaveInfo("Subnautica", "Android/data/com.unknownworlds.subnautica/files/SavedGames"),
        GameSaveInfo("Subnautica Below Zero", "Android/data/com.unknownworlds.subnauticabelowzero/files/SavedGames"),
        GameSaveInfo("Snufkin: Melody of Moominvalley", "Android/data/com.snapbreak.snufkinmelodyofmoominvalley/files/Saves"),
        GameSaveInfo("Dadish3D", "Android/data/com.ThomasK.Young.Dadish3D/files/SaveDir"),
        GameSaveInfo("Everlasting Summer", "Android/data/su.sovietgames.everlasting_summer/saves"),
        GameSaveInfo("Monument Valley 3 (EGS)", "Android/data/com.ustwogames.mv3.epicgames/files/CloudSave"),
        GameSaveInfo("KOTOR II", "Android/data/com.aspyr.swkotorii/files/saves/"),
        GameSaveInfo("KOTOR", "Android/data/com.aspyr.swkotor/files/saves/"),
        GameSaveInfo("Grand Theft Auto: San Andreas", "Android/data/com.rockstargames.gtasa/files/"),
        GameSaveInfo("The Were Cleaner", "Android/data/com.HowlinHugs.TheWereCleaner/files/SavesDir"),
        GameSaveInfo("PS2: NetherSX2", "Android/data/xyz.aethersx2.android/files"),
        GameSaveInfo("Dolphin Emulator", "Android/data/org.dolphinemu.dolphinemu/files"),
        GameSaveInfo("The Sun Origin", "Android/data/Agaming.thesun.origin/files/"),
        GameSaveInfo("PSVita (Vita3k:SaveData)", "Android/data/org.vita3k.emulator/files/vita/ux0/user/00/savedata"),
        GameSaveInfo("PSVita (Vita3k:Data)", "Android/data/org.vita3k.emulator/files"),
        GameSaveInfo("NDS (primary:EmulatorDS)", "Documents/Emulator/DS"),
        GameSaveInfo("NDS (primary:EmulatorNDS)", "Documents/Emulator/NDS"),
        GameSaveInfo("NDS (primary:DS)", "DS"),
        GameSaveInfo("NDS (primary:NDS)", "NDS"),
        GameSaveInfo("NDS (primary:MelonDS)", "MelonDS"),
        GameSaveInfo("Zalith Launcher (Minecraft)", "Android/data/com.movtery.zalithlauncher.v2/files/.minecraft"),
        GameSaveInfo("DuckStation", "Android/data/com.github.stenzek.duckstation/files"),
        GameSaveInfo("Bomb Squad", "Android/data/net.froemling.bombsquad/files/mods"),
        GameSaveInfo("Neverless to Everless (Selfie)", "Android/data/com.hottagames.nte/files/Selfie"),
        GameSaveInfo("Wytchwood", "Android/data/com.Alientrap.Wytchwood"),
        GameSaveInfo("NS (Suyu)", "Android/data/org.suyu.suyu_emu/files"),
        GameSaveInfo("The Wreck", "Android/data/com.ThePixelHunt.TheWreck"),
        GameSaveInfo("Mindustry", "Android/data/io.anuke.mindustry/files/saves"),
    )

    fun hasAnyGame(context: Context): Boolean {
        val storageRoot = Environment.getExternalStorageDirectory()
        for (game in games) {
            val fullPath = File(storageRoot, game.relativePath)
            val isRestricted = fullPath.absolutePath.lowercase().contains("/android/data")
            
            if (isRestricted) {
                // For restricted paths, we assume they exist so the user can see the "Game Manager"
                // and potentially authorize Shizuku to see them.
                return true
            } else if (fullPath.exists()) {
                return true
            }
        }
        return false
    }

    fun getGames(context: Context, appConfigs: AppConfigurations): List<UniversalFile> {
        val storageRoot = Environment.getExternalStorageDirectory()
        val results = mutableListOf<UniversalFile>()

        // 1. Add folders from effective games path
        val effectivePath = SettingsManager.getEffectiveGamesPath(context)
        if (effectivePath.isNotEmpty()) {
            val gamesDir = File(effectivePath)
            if (gamesDir.exists() && gamesDir.isDirectory) {
                val children = gamesDir.listFiles()
                children?.filter { it.isDirectory && !it.name.startsWith(".") }?.forEach { dir ->
                    results.add(
                        UniversalFile(
                            name = dir.name,
                            isDirectory = true,
                            lastModified = dir.lastModified(),
                            length = 0L,
                            provider = LocalProvider,
                            providerId = dir.absolutePath,
                            parentId = "virtual://games_manager"
                        )
                    )
                }
            }
        }

        // 2. Add folders from hardcoded list
        for (game in games) {
            if (game.displayName.isEmpty()) continue
            val fullPath = File(storageRoot, game.relativePath)
            
            // Skip if already added from custom path
            if (results.any { it.name == game.displayName || (it.providerId == fullPath.absolutePath) }) continue
            
            val isRestricted = fullPath.absolutePath.lowercase().contains("/android/data")

            if (isRestricted) {
                if (ShizukuManager.hasPermission() && ShizukuProvider.exists(fullPath.absolutePath)) {
                    val meta = ShizukuProvider.getMetadata(fullPath.absolutePath)
                    results.add(
                        UniversalFile(
                            name = game.displayName,
                            isDirectory = true,
                            lastModified = meta.lastModified,
                            length = 0L,
                            provider = ShizukuProvider,
                            providerId = fullPath.absolutePath,
                            parentId = "virtual://games_manager"
                        )
                    )
                }
            } else if (fullPath.exists()) {
                 results.add(
                    UniversalFile(
                        name = game.displayName,
                        isDirectory = true,
                        lastModified = fullPath.lastModified(),
                        length = 0L,
                        provider = LocalProvider,
                        providerId = fullPath.absolutePath,
                        parentId = "virtual://games_manager"
                    )
                )
            }
        }

        // Add SAF shortcuts
        for (shortcut in appConfigs.gameSafShortcuts) {
            val uri = shortcut.uri
            try {
                val doc = DocumentFile.fromTreeUri(context, uri)
                if (doc != null && doc.exists()) {
                    val decodedUri = Uri.decode(uri.toString())
                    val packageName = IconHelper.getPackageNameFromPath(decodedUri, strict = false)
                    val appName = packageName?.let { IconHelper.getAppName(context, it) }
                    
                    results.add(
                        UniversalFile(
                            name = shortcut.name ?: appName ?: doc.name ?: uri.toString(),
                            isDirectory = true,
                            lastModified = doc.lastModified(),
                            length = 0L,
                            provider = SafProvider,
                            providerId = uri.toString(),
                            parentId = "virtual://games_manager"
                        )
                    )
                }
            } catch (_: Exception) {
                // Ignore
            }
        }

        return results
    }

    data class GameSaveInfo(val displayName: String, val relativePath: String)
}
