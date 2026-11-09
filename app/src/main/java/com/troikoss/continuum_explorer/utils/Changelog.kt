package com.troikoss.continuum_explorer.utils

/**
 * Changelog configuration file.
 * You can easily edit or add new changes here during development.
 */
data class ChangelogCategory(
    val title: String,
    val items: List<String>
)

data class ChangelogRelease(
    val versionName: String,
    val releaseTitle: String = "",
    val note: String? = null,
    val categories: List<ChangelogCategory>
)

object ChangelogProvider {
    /**
     * The latest release changelog shown in the Welcome & Update dialog.
     * Edit this during development for new updates.
     */
    val currentRelease = ChangelogRelease(
        versionName = "Memories M10",
        releaseTitle = "Memories 8.1 update with QOF improvement",
        note = "Important note:\nAfter updating libraries for this app, the minimum requirement for the system rises to Android 9 (API 28+).",
        categories = listOf(
            ChangelogCategory(
                title = "New Additions",
                items = listOf(
                    "New games added to support (BloonsTD6 (EGS), Construction Simulator (EGS) and Polytrack)",
                    "Video manager added to homepage",
                    "PDF Viewer reworked fully",
                    "Tools menu inside three dot button"
                )
            ),
            ChangelogCategory(
                title = "Updates",
                items = listOf(
                    "Details window max size increased",
                    "Blur effect added for sidebar (excluding square tablet/desktop UI)",
                    "Blur effect added for non-active tabs",
                    "Welcome & What's New dialog added for updates and first-time setup"
                )
            ),
            ChangelogCategory(
                title = "Fixes",
                items = listOf(
                    "Music details fixed",
                    "Colour palette optimised and fixed"
                )
            )
        )
    )
}
