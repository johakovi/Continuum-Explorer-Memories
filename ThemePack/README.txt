Continuum Explorer Theme Pack Example

Structure:
- theme.json (Colors configuration)
- icons/ (Folder containing .png icons)

To use:
1. Customize theme.json colors.
2. Add your own 128x128 or larger PNG icons in the icons/ folder.
   Supported names match drawable resources:
   - ic_folder.png
   - ic_nav_gallery.png
   - ic_nav_recent.png
   - ic_nav_downloads.png
   - ic_nav_documents.png
   - ic_nav_game.png
   - ic_nav_trash.png
   - ic_file.png
   - ic_pdf.png
   - ic_xls.png
   - ic_docx.png
   - ic_txt.png
   - ic_zip.png
   - ic_terminal.png
   - ic_video.png
   - ic_network.png
   - ic_dcim_folder.png (Custom icon for DCIM folder at storage root)
   - ic_download_folder.png (Custom icon for Download folder at storage root)
   - ic_pictures_folder.png (Custom icon for Pictures folder at storage root)
   - ic_documents_folder.png (Custom icon for Documents folder at storage root)
   - ic_music_folder.png (Custom icon for Music folder at storage root)
   - ic_video_folder.png (Custom icon for Movies/Video folder at storage root)

theme.json Supported Colour Keys:
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

   Library/File Icons:
   - folderIcon
   - galleryIcon
   - recentIcon
   - filesIcon
   - documentsIcon
   - gameIcon
   - gameShortcutIcon
   - recycleBinIcon
   - downloadsIcon
   - zipIcon
   - pdfIcon
   - xlsIcon
   - docxIcon
   - txtIcon
   - terminalIcon
   - imageIcon
   - videoIcon
   - audioIcon
   - musicIcon

3. Zip the contents (theme.json and icons folder) into a .zip file.
4. Load the .zip file in the app settings.

Note on Custom Themes:
When a Theme Pack is loaded, the standard "Theme" and "Icon Theme" settings are disabled.
Instead, use the "Custom Theme Mode" setting to switch between Light, Dark, and System modes
for your custom theme.

    "tabActiveBackground":      //Tab Active Background (The background of the active tab)
    "textColor":                //Text colour (File names, folder names, ... etc)
    "menuBackground":           //Context Menu (When you right click on a file or folder)
    "fileViewBackground":       //Content View (Where the files and folders are)
    "background":               //Background
    "commandPanelBackground":   //Command Panel (Copy, paste, delete, ... etc below top bar)
    "folderIcon":               //Folder Icon
    "sidebarBackground":        //Sidebar Background
    "topBarBackground":         //Top Bar (Search bar, nav buttons, search... etc)
    "searchBoxBackground":      //Search box background (The background of the search box when you click on the search icon in the top bar)
    "navButtonBackground":      //Nav Buttons background (Back, Forward, Up)
    "tabBarBackground":         //Tab Bar (Where the tabs are)
    "sidebarIcons":             //Sidebar Icons (Not predefined icon colouring, but the icons that are in the sidebar)
    "primary":                  //Primary (The main colour of the theme, used for highlights, ... etc)
    "statusBarColor":           //Status bar (Battery, time, ... etc)
    "navigationBarColor":       //Three button navigation bar or gesture navigation (Back, Home, Recent)
    "imageIcon":                //File icon
    "videoIcon":                //File icon
    "audioIcon":                //File icon
    "musicIcon":                //File icon
    "galleryIcon":              //Sidebar icon
    "recentIcon":               //Sidebar icon
    "downloadsIcon":            //Sidebar icon
    "documentsIcon":            //Sidebar icon
    "gameIcon":                 //Sidebar icon
    "recycleBinIcon":           //Sidebar icon
    "zipIcon":                  //File icon
    "pdfIcon":                  //File icon
    "xlsIcon":                  //File icon
    "docxIcon":                 //File icon
    "txtIcon":                  //File icon
    "terminalIcon":             //File icon
  }
}
