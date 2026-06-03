package com.troikoss.continuum_explorer.managers

import android.content.Context
import android.os.Environment
import com.troikoss.continuum_explorer.model.UniversalFile
import com.troikoss.continuum_explorer.providers.LocalProvider
import com.troikoss.continuum_explorer.providers.ShizukuProvider
import java.io.File

object GamesManager {
    private val games = listOf(
        GameSaveInfo("Endling", "Android/data/com.hg.endling/files/UE4Game/Endling/Endling/Saved/SaveGames"),
        GameSaveInfo("SENTINEL519", "Android/data/com.AppyBearStudio.SENTINEL519/files/"),
        GameSaveInfo("Stardew Valley", "Android/data/com.chucklefish.stardewvalley/files/Saves"),
        GameSaveInfo("PSP (savedata:PPSSPP)", "PPSSPP/PSP/SAVEDATA"),
        GameSaveInfo("PSP (savedata:PSP)", "PSP/PSP/SAVEDATA"),
        GameSaveInfo("PSP (savedata:Emulator/PSP)", "Documents/Emulator/PSP/SAVEDATA"),
        GameSaveInfo("PSP (data:Emulator/PSP)", "Documents/Emulator/PSP"),
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
        GameSaveInfo("PS2 (NetherSX2)", "Android/data/xyz.aethersx2.android/files"),
        GameSaveInfo("Dolphin Emulator", "Android/data/org.dolphinemu.dolphinemu/files"),
        GameSaveInfo("The Sun Origin", "Android/data/Agaming.thesun.origin/files/"),
        GameSaveInfo("PSVita (Vita3k:SaveData)", "Android/data/org.vita3k.emulator/files/vita/ux0/user/00/savedata"),
        GameSaveInfo("PSVita (Vita3k:data)", "Android/data/org.vita3k.emulator/files"),
        GameSaveInfo("NDS (primary:Emulator/DS)", "Documents/Emulator/DS"),
        GameSaveInfo("NDS (primary:Emulator/NDS)", "Documents/Emulator/NDS"),
        GameSaveInfo("NDS (primary:DS)", "DS"),
        GameSaveInfo("NDS (primary:NDS)", "NDS"),
        GameSaveInfo("NDS (primary:MelonDS)", "MelonDS"),
        GameSaveInfo("Zalith Launcher (Minecraft)", "Android/data/com.movtery.zalithlauncher.v2/files/.minecraft"),
        GameSaveInfo("DuckStation", "Android/data/com.github.stenzek.duckstation/files"),
        GameSaveInfo("Bomb Squad", "Android/data/net.froemling.bombsquad/files/mods"),
        GameSaveInfo("Neverless to Everless (Selfie)", "Android/data/com.hottagames.nte/files/Selfie"),

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

    fun getGames(context: Context): List<UniversalFile> {
        val storageRoot = Environment.getExternalStorageDirectory()
        val results = mutableListOf<UniversalFile>()

        for (game in games) {
            if (game.displayName.isEmpty()) continue
            val fullPath = File(storageRoot, game.relativePath)
            
            val isRestricted = fullPath.absolutePath.lowercase().contains("/android/data")
            
            if (isRestricted) {
                if (ShizukuManager.hasPermission()) {
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
                                parentId = "virtual://games_manager"
                            )
                        )
                    }
                } else {
                    // If restricted but no permission, we still add it so the user can see it 
                    // and clicking it will trigger the Shizuku permission prompt.
                    results.add(
                        UniversalFile(
                            name = game.displayName,
                            isDirectory = true,
                            lastModified = 0L,
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
        return results
    }

    data class GameSaveInfo(val displayName: String, val relativePath: String)
}
