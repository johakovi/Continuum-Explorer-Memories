Continuum Explorer Theme Pack Example

Structure:
- theme.json (Colors configuration)
- icons/ (Folder containing .png icons)

To use:
1. Customize theme.json colors.
2. Add your own 128x128 or larger PNG icons in the icons/ folder.
   Supported names match drawable resources:
   - ic_folder.png
   - ic_folder_duo.png
   - ic_nav_gallery.png
   - ic_nav_gallery_duo.png
   - ic_nav_recent.png
   - ic_nav_recent_duo.png
   - ic_nav_downloads.png
   - ic_nav_downloads_duo.png
   - ic_nav_documents.png
   - ic_nav_documents_duo.png
   - ic_nav_game.png
   - ic_nav_game_duo.png
   - ic_nav_trash.png
   - ic_nav_trash_duo.png
   - ic_file.png
   - ic_file_duo.png
   - ic_pdf.png
   - ic_pdf_duo.png
   - ic_xls.png
   - ic_xls_duo.png
   - ic_docx.png
   - ic_docx_duo.png
   - ic_txt.png
   - ic_txt_duo.png
   - ic_zip.png
   - ic_zip_duo.png
   - ic_terminal.png
   - ic_terminal_duo.png
   - ic_video.png
   - ic_video_duo.png
   - ic_network.png
   - ic_network_duo.png
   - ic_dcim_folder.png (Custom icon for DCIM folder at storage root)
   - ic_download_folder.png (Custom icon for Download folder at storage root)
   - ic_pictures_folder.png (Custom icon for Pictures folder at storage root)
   - ic_documents_folder.png (Custom icon for Documents folder at storage root)
   - ic_music_folder.png (Custom icon for Music folder at storage root)
   - ic_video_folder.png (Custom icon for Movies/Video folder at storage root)

theme.json Supported Color Keys:
   Global:
   - sidebarBackground
   - topBarBackground
   - navButtonBackground
   - searchBoxBackground
   - tabBarBackground
   - tabActiveBackground
   - textColor
   - menuBackground
   - fileViewBackground
   - background
   - commandPanelBackground
   - selectionBackground
   - sidebarIcons
   - primary
   - onPrimary
   - primaryContainer
   - onPrimaryContainer
   - secondary
   - onSecondary
   - secondaryContainer
   - onSecondaryContainer
   - tertiary
   - onTertiary
   - tertiaryContainer
   - onTertiaryContainer
   - backgroundM3
   - onBackground
   - surface
   - onSurface
   - surfaceVariant
   - onSurfaceVariant
   - surfaceContainer
   - surfaceContainerLow
   - surfaceContainerHigh
   - surfaceContainerHighest
   - surfaceContainerLowest
   - outline
   - outlineVariant
   - statusBarColor
   - navigationBarColor

   Library/File Icons (Standard & Duo variants):
   - folderIcon, folderIconDuo
   - galleryIcon, galleryIconDuo
   - recentIcon, recentIconDuo
   - filesIcon, filesIconDuo
   - documentsIcon, documentsIconDuo
   - gameIcon, gameIconDuo
   - gameShortcutIcon, gameShortcutIconDuo
   - recycleBinIcon, recycleBinIconDuo
   - downloadsIcon, downloadsIconDuo
   - zipIcon, zipIconDuo
   - pdfIcon, pdfIconDuo
   - xlsIcon, xlsIconDuo
   - docxIcon, docxIconDuo
   - txtIcon, txtIconDuo
   - terminalIcon, terminalIconDuo
   - imageIcon, imageIconDuo
   - videoIcon, videoIconDuo
   - audioIcon, audioIconDuo
   - musicIcon, musicIconDuo

3. Zip the contents (theme.json and icons folder) into a .zip file.
4. Load the .zip file in the app settings.

Note on Custom Themes:
When a Theme Pack is loaded, the standard "Theme" and "Icon Theme" settings are disabled.
Instead, use the "Custom Theme Mode" setting to switch between Light, Dark, and System modes
for your custom theme.
