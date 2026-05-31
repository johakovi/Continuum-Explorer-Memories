package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.os.Environment
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import java.io.File

object GameSavesManager {
    fun getGameSaves(context: Context): List<UniversalFile> {
        val games = listOf(
            GameSaveInfo("Endling", "Android/data/com.hg.endling/files/UE4Game/Endling/Endling/Saved/SaveGames"),
            GameSaveInfo("SENTINEL519", "Android/data/com.AppyBearStudio.SENTINEL519/files/"),
            GameSaveInfo("Stardew Valley", "Android/data/com.chucklefish.stardewvalley/files/Saves"),
            GameSaveInfo("PSP (PPSSPP)", "PPSSPP/PSP/SAVEDATA"),
            GameSaveInfo("PSP (Emulator)", "Documents/Emulator/PSP/SAVEDATA"),
            GameSaveInfo("PSP (Data)", "Android/data/org.ppsspp.ppsspp/files/PSP/SAVEDATA"),
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
            GameSaveInfo("PS2 (NetherSX2)", "Android/data/xyz.aethersx2.android/files"),
            GameSaveInfo("Dolphin Emulator", "Android/data/org.dolphinemu.dolphinemu/files"),
            GameSaveInfo("The Sun Origin", "Android/data/Agaming.thesun.origin/files/"),

        )

        val storageRoot = Environment.getExternalStorageDirectory()
        val results = mutableListOf<UniversalFile>()

        for (game in games) {
            // We use a simplified check: if it's a restricted path, we assume it exists if Shizuku is available,
            // or we just return it so the user can see it and potentially click it (which will trigger Shizuku prompt).
            // However, to be more accurate, we can try to get metadata via Shizuku if available.
            if (game.displayName.isEmpty()) continue
            val fullPath = File(storageRoot, game.relativePath)
            
            val isRestricted = fullPath.absolutePath.lowercase().contains("/android/data")
            val provider = if (isRestricted) ShizukuProvider else LocalProvider
            
            if (isRestricted && ShizukuManager.hasPermission()) {
                if (ShizukuProvider.exists(fullPath.absolutePath)) {
                    val meta = ShizukuProvider.getMetadata(fullPath.absolutePath)
                    results.add(
                        UniversalFile(
                            name = game.displayName,
                            isDirectory = true,
                            lastModified = meta.lastModified,
                            length = 0L,
                            provider = ShizukuProvider,
                            providerId = fullPath.absolutePath,
                            parentId = "virtual://game_saves"
                        )
                    )
                }
            } else if (!isRestricted && fullPath.exists()) {
                 results.add(
                    UniversalFile(
                        name = game.displayName,
                        isDirectory = true,
                        lastModified = fullPath.lastModified(),
                        length = 0L,
                        provider = LocalProvider,
                        providerId = fullPath.absolutePath,
                        parentId = "virtual://game_saves"
                    )
                )
            } else {
                results.add(
                    UniversalFile(
                        name = game.displayName,
                        isDirectory = true,
                        lastModified = 0L,
                        length = 0L,
                        provider = provider,
                        providerId = fullPath.absolutePath,
                        parentId = "virtual://game_saves"
                    )
                )
            }
        }
        return results
    }

    data class GameSaveInfo(val displayName: String, val relativePath: String)
}
