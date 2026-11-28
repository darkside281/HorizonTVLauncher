# Horizon TV Launcher — Complete User Guide & Feature Reference

**Version:** `2.5.61`  
**Package ID:** `com.horizontv.launcher`  
**Repository:** https://github.com/darkside281/HorizonTVLauncher  
**Platform:** Android TV / Nvidia Shield / Google TV / Waydroid (Android 7.0+ / API 24+)  
**APK Size:** ~2.4 MB (R8-optimized universal APK — zero extra permissions, no ads, no paywalls)

Horizon TV Launcher is a fast, ad-free, open-source Android TV home screen built with Jetpack Compose. It pairs full-bleed cinema artwork with customizable app rows, **2×2 App Folders**, a **Continue Watching / "Up Next" Row** (Jellyfin, Plex & Emby), Minimalist Bottom-Dock mode, a **Live-Preview Customize Drawer (Easy / Advanced)**, live wallpapers (including **SMB Network Shares** with LAN discovery & folder browsing, **Local Photo Albums**, and seasonal **Reddit** feeds), an idle & Android system screensaver, live weather forecasts, universal streaming & Jellyfin search with zero-permission Voice Search, Parental Controls, a 6-tab **Local Web Companion** for phone/PC editing, and a built-in GitHub updater.

---

## 1. Screen Layout Overview

- **Fixed Top Hero Area:** Displays your chosen **Clock** style, date, and live weather forecast on the left, with quick-access action buttons (**Search**, **Customize**, **TV Inputs**, **Android Settings**) on the top right.
- **Full-Bleed Backdrop & ClearLogo:** Shows your ambient gradient, custom image, or rotating live wallpaper (with optional **Ken Burns slow motion** and instant cold-start caching), with the official movie/show ClearLogo cleanly framed above the dock.
- **Standard Dock or Minimalist Bottom-Dock Mode:** Use standard multi-row scrolling, or toggle **Minimalist Bottom-Dock Mode** (available right at the top of **Customize → Easy Mode** or **Home → Tile Style**) for a sleek frosted dock that leaves maximum space for your artwork.
- **Continue Watching / "Up Next" Row:** Optional row right below your hero row showing in-progress movies and next episodes from **Jellyfin**, **Plex**, or **Emby** with progress bars, episode badges, remaining time, and one-click resume.
- **Resolution-Independent Scaling:** Automatically scales to a fixed `1152dp` design width so tiles, typography, and spacing look identical across 720p, 1080p, 4K TVs, Nvidia Shields, and Waydroid (including the Android System Screensaver).
- **First-Run Setup Wizard:** Fresh installs automatically launch a 4-step Setup Wizard (Basics, Wallpapers, Region & Search, Favorite Apps). Existing setups skip it automatically, and you can re-run it anytime under **Customize → Launcher & Apps → Run Setup Wizard…**.

---

## 2. Remote Control & Navigation

| Button / Action | Where | What It Does |
| :--- | :--- | :--- |
| **D-Pad Up / Down** | Home Screen | Move between your custom app rows, Continue Watching, the All Apps grid (if enabled), and the top-right action bar. |
| **D-Pad Left / Right** | App Row / Continue Watching | Scroll horizontally through tiles in the active row. |
| **OK / Select (Short Press)** | App Tile / Folder / Resume Card | Launch the app, open the App Folder popup, or resume playback in Jellyfin/Plex/Emby (prompts for 4-digit PIN if app is locked). |
| **OK / Select (Long Press)** or **Menu** | App Tile or App Folder | Open the **App Action Menu** (reorder, rename, change icon, lock with PIN, hide, app info, uninstall) or start **Reorder Mode** on a Folder. |
| **OK / Select** | Setup Dialog Text Fields | Click-to-edit: moving across Server URL, Username, Password, or API Key fields will **not** pop up the keyboard until you press **OK**. |
| **Home Button** | Anywhere | Return to Horizon and focus Row 1, Tile 1. |
| **Back Button** | Customize Drawer / Dialogs | Steps cleanly back from a drilled-in settings section to the Customize menu, or closes the active overlay / Reorder Mode. |
| **D-Pad Down / Search / Enter** | Search Text Box | Dismiss the on-screen keyboard and jump straight into filter chips and results. |

---

## 3. Live-Preview Customize Drawer (Easy & Advanced Modes)

Clicking the **Customize** palette icon opens a translucent `510dp` right-side drawer so your **real TV Home screen stays live and visible on the left** while you tweak settings:

- **`[ Easy | Advanced ]` Toggle Pill (Top-Right):**
  - **Easy Mode (Default):** Clean, scrollable essentials right up front — toggle **Minimalist Bottom-Dock**, **Continue Watching Row**, pick **Tile Size (`XS / Small / Medium / Large`)**, tap **Accent Color** swatches with instant live preview, or jump straight to **Wallpaper Source**, **Home Rows & App Folders**, **Weather**, **Phone / PC Web Companion**, **Updates**, or **Backup & Setup Wizard**.
  - **Advanced Mode:** Shows all 10 full configuration sections with live status subtitles.
  - **Persistent Choice:** Whichever mode (**Easy** or **Advanced**) you select is automatically remembered across sessions, reboots, and Backup/Restore.
- **Smooth Drill-Down Navigation:** Selecting any section opens its controls inside the same steady-width drawer with a **`‹ Back`** header button (and pressing **Back** on your remote steps back to the menu before closing).

---

## 4. Managing Rows, App Folders & Continue Watching

### Custom Rows & Per-Row Tile Shapes
Open **Customize → Home → Rows** to organize your apps:
- **Create, Rename, Reorder & Delete Rows:** Add as many custom rows as you want (e.g., *Main Streaming*, *Media*, *Games*, *Utilities*), move them up/down, or rename them.
- **Per-Row Tile Aspect Ratios (`16:9` / `1:1` / `2:3`):** Tap the shape button on any row to switch that individual row between **16:9 Banner**, **1:1 Square**, or **2:3 Poster** tiles.

### App Folders (2×2 Preview Tiles)
Group related apps into tidy **2×2 preview tiles** right inside any home row:
1. Open **Customize → Home → Rows** and scroll to **App Folders (2×2 Tiles)**.
2. Tap **`+ New Folder`**, give it a name (e.g., *Games* or *Utilities*), and pick the apps to include.
3. Add the folder to any Home row just like a normal app (via the row's **`+`** button or **Local Web Companion**).
4. On the Home screen, click a folder tile to open its frosted 3-column popup grid, or **long-press OK** on a folder tile to slide it left/right in the row.

### Continue Watching / "Up Next" Row (Jellyfin, Plex & Emby)
Resume movies and next episodes directly from your Home screen using your existing media server connection (zero extra setup):
1. Connect **Jellyfin**, **Plex**, or **Emby** under **Customize → Wallpapers** (or **Web Companion → Servers & API Keys**).
2. Turn on **Continue Watching Row** in **Customize → Easy Mode** (or **Customize → Home → Rows → Show Continue Watching Row**).
3. Your in-progress movies and next episodes appear as 16:9 resume cards with episode/movie badges, remaining time pills, titles, and progress bars — click any card to jump straight into playback.

### Reordering Tiles (4 Ways)
1. **Local Web Companion (Fastest):** Drag and drop tiles using the **`⋮⋮`** handle (supports mouse & mobile touch), or use `↑ / ↓`, **+ Add**, **Remove**, **Sort A–Z**, and **Clean Up Uninstalled**.
2. **Tile Order Dialog:** Go to **Customize → Home → Rows** and press **`↑↓`** on any row with 2+ apps.
3. **Inline Reorder Mode:** Long-press **OK** on an app tile or App Folder → use Left/Right to slide it, then press **OK** or **Back** to finish.
4. **Single-Step Move:** Long-press **OK** on an app tile → **Move Left** or **Move Right**.

### Tile Styling & Custom Clock Typography
- **Customize → Home → Tile Style:**
  - **Minimalist Bottom-Dock Mode**
  - **Tile Size:** Choose **XS**, **Small**, **Medium**, or **Large**
  - **Corner Roundness:** **Sharp**, **Rounded**, or **Soft**
  - **Focus Border Style:** **Accent Glow**, **Crisp Outline**, **Zoom Only**, or **Soft Shadow**
  - **Tile Spacing:** **Compact**, **Normal**, or **Wide**
  - **Tile Opacity (Unfocused):** `40%`–`100%`
  - **Show App Titles Under Tiles**
- **Customize → Home → Clock & Header:**
  - **Clock Layout:** **Classic**, **Centered Cinema**, **Analog Dial**, or **Minimal**
  - **Clock Font:** **Modern Sans**, **Editorial Serif**, or **Tech Mono**
  - **Font Weight:** **Light**, **Regular**, or **Bold**
  - **Clock / Date / 24-Hour** toggles and **Show TV Inputs (HDMI) Button** (auto-hidden on streaming boxes without hardware inputs).

---

## 5. Live Wallpapers, Local Photo Albums & SMB Network Shares

Open **Customize → Wallpapers** (or **Web Companion → Wallpapers & Apps**) to pick from **7 live wallpaper providers** plus **6 Ambient Themes**:

| Provider | How to Set Up | Content & Features |
| :--- | :--- | :--- |
| **Jellyfin** | Server URL + API Key (or Username/Password) | Rotating fanart backdrops & official ClearLogos from multi-selected libraries. Filter by **Both**, **Movies**, or **TV Shows**. |
| **Plex** | Server URL + Plex Token (`X-Plex-Token`) | High-res library backdrops from multi-selected Plex libraries. |
| **Emby** | Server URL + API Key (or Username/Password) | Library backdrops & ClearLogos from multi-selected Emby libraries. |
| **TMDB** | Free TMDB **v3 API Key** or **v4 Read Access Token** | 4K-ready movie & TV backdrops with official English ClearLogos across 6 selectable categories. |
| **Reddit** | None (works immediately) | High-res landscape photography & wallpapers (including Reddit galleries & automatic 16:9 Wallhaven top-up) across **11 curated categories** — including seasonal **Christmas** and **Winter & Snow** — plus custom subreddits. Sort by **Hot**, **Top Week**, **Top Month**, or **Top All Time**. |
| **Local Folder & Photo Album** | Pick a folder on TV (SAF) or batch-upload photos from your phone/PC via Web Companion | Rotates your personal photos on the Home screen and Screensaver, or pick a single static custom image. |
| **SMB Network Share (`smb://...`)** | Scan your LAN & browse shared folders directly (Guest or Username/Password) | Streams wallpapers directly from your NAS, Windows PC, Samba server, or router USB share over Wi-Fi/LAN with zero extra permissions. |

### How to Set Up an SMB Network Share (With Built-In Scanner & Folder Browser)
- **On your TV:**
  1. Go to **Customize → Wallpapers → Source & Setup → SMB Share → Configure SMB Share…**.
  2. Click **Scan LAN** to automatically discover SMB servers on your network (finishes in ~1.5 seconds).
  3. Select a discovered server (e.g., `NAS (192.168.0.50)`). If your share is private, press **OK** on **Username** and **Password** to enter your credentials first, then click **Browse Shares**.
  4. Click through `📁` shares and subfolders (each folder shows how many images it contains) and press **Connect & Start**.
- **From your Phone / PC Browser (Even Easier):**
  1. Open **Local Web Companion → Wallpapers & Apps → SMB Network Share**.
  2. Click **🔍 Scan Network for SMB Servers**, tap a server, browse `📁` shares/folders, and click **✓ Use This Folder**.

### Independent Per-Service Settings & Motion
- Each wallpaper provider remembers its own **Show Movie/Show Logo**, **Slideshow Speed**, **Wallpaper Count**, and **Media Type** independently.
- Enable **Ken Burns Slow Motion** under **Customize → Wallpapers → Slideshow & Display** for smooth GPU-accelerated cinema pan & zoom.
- Horizon caches your last shown live backdrop locally so your wallpaper appears **instantly on cold boot** while fresh backdrops load in the background, and automatically re-shuffles after each full cycle.

---

## 6. Local Web Companion (Phone / PC Browser)

Edit everything comfortably from your phone or computer browser on the same Wi-Fi/LAN:
1. Open **Customize → Phone / PC Web Companion** (or **Launcher & Apps → Local Web Companion…**).
2. Scan the **QR code** from your phone (auto-signs in) or open the shown `http://<TV-IP>:8765` URL in any PC browser and enter the **4-digit session code**.
3. Protected by a fresh 4-digit session PIN with a **5-attempt / 30-second lockout** against brute-force guessing.
4. **6 Built-In Tabs:**
   - **Home & Dock:** Drag-and-drop (`⋮⋮`) tile reordering, real TV app icons, add/remove apps, sort A–Z, clean up uninstalled apps, manage rows, tile size (`XS`–`Large`), shapes, corners, focus style, and clock style.
   - **Search & Services:** Set your JustWatch region, subscribed streaming services, Prime Video Channels, and search options.
   - **Wallpapers & Apps:** Switch themes/accents live, configure live slideshow providers, batch-upload photos to your **Local Folder Photo Album**, scan & browse **SMB Network Shares**, upload custom Home wallpapers, and rename/hide/upload custom app icons.
   - **Screensaver & Weather:** Search any city worldwide and tweak all Weather & Screensaver settings.
   - **Servers & API Keys:** Paste Jellyfin, Plex, Emby, SMB, and TMDB URLs and credentials directly from your clipboard.
   - **Security & Backup:** Manage Parental PIN settings and download/restore timestamped JSON backups.
5. Press **Stop Server & Close** on your TV when finished to shut down the local server immediately.

---

## 7. Universal Search, Voice Search & "Where to Watch"

Click the **Search** icon in the top-right bar to search across streaming services, your Jellyfin library, and installed apps at once:
- **Zero-Permission Voice Search:** Click the microphone icon beside the search box to speak your query via Android's system speech recognizer (no `RECORD_AUDIO` permission required).
- **Recent Searches:** Your last 8 completed searches appear as quick-pick chips (with **Clear All**).
- **Full Title Details Popup:** Clicking any movie or TV show opens a details popup with full synopsis, runtime, genres, IMDb & Rotten Tomatoes scores, age rating, cast/directors, and streaming options — selecting a streaming service opens its **Overview / Details** screen rather than immediately auto-playing.
- **Multi-Region JustWatch (`GB`, `US`, `CA`, `AU`, `IE`, `DE`, `FR`, `NL`):** Configure your region, subscribed services, and **Prime Video Channels** under **Customize → Search**.

---

## 8. Built-In Screensaver & Android DreamService

Open **Customize → Screensaver** to configure Horizon's full-screen screensaver:
- **Idle Screensaver:** Starts automatically after a configurable idle timeout (`1m`–`30m`) on the Home screen (and pauses cleanly whenever any dialog or folder popup is open).
- **Android System Screensaver (`DreamService`):** Tap **Android System Screensaver** to select **Horizon Screensaver** system-wide — matches Horizon's `1152dp` resolution scaling on 720p/1080p/4K TVs and refreshes weather every 30 minutes overnight.
- **3 Modes:** **Wallpaper Artwork**, **My Photos**, or **Ambient Themes** — with smooth **Ken Burns** motion, **OLED Burn-In Shift**, dim control, and independent Clock / Weather / Title overlays.

---

## 9. Weather Widget & Parental Controls

- **Live Weather (Open-Meteo, no API key needed):** Configure under **Customize → Weather** — worldwide city search, 1–7 day forecast, `°C`/`°F`, Small/Medium/Large sizing, 4 icon styles, and optional card backdrop.
- **Parental Controls (4-Digit PIN):** Configure under **Customize → Parental Controls** to lock the **Customize** menu and/or individual apps (long-press **OK** on any tile → **Lock App with PIN**).

---

## 10. Built-In GitHub Self-Updater

Horizon checks https://github.com/darkside281/HorizonTVLauncher/releases for newer versions:
- Configure **Automatic**, **Every 7 Days**, or **Manual Only** under **Customize → Updates**.
- Automatically downloads the tiny **~2.4 MB R8-optimized APK** (skipping `_full` fallback APKs) with a live progress bar and opens Android's installer.

---

## 11. Backup, Restore, Reset & Syncing Devices

Horizon exports your entire configuration (rows, folders, app order, custom labels, theme, accent, wallpaper providers & per-service configs, local photo folders, weather location, search settings & subscriptions, parental controls, Easy/Advanced mode, and screensaver) to a timestamped `.json` file (e.g., `horizon-tv-backup-2026-11-28_09-59.json`).

> **Security Note:** Backup JSON files include any configured API keys, tokens, or SMB passwords so restoring onto another device works automatically. Keep your backup files private and do not share them publicly.

### Backup & Restore Methods
- **Local Web Companion (Easiest):** Open **Security & Backup** in your phone/PC browser to download or restore a `.json` backup in one click.
- **In-App File Picker:** Go to **Customize → Launcher & Apps → Backup Config… / Restore Config…**.
- **Quick ADB Backup & Restore:**
```bash
# Export current config on a device
adb -s <DEVICE_IP>:5555 shell am start -n com.horizontv.launcher/.MainActivity --ez horizon_export_backup true

# Pull the exported file to your PC
adb -s <DEVICE_IP>:5555 pull /sdcard/Android/data/com.horizontv.launcher/files/horizon-tv-backup.json ./horizon-tv-backup.json

# Push and auto-import onto another device
adb -s <TARGET_IP>:5555 push ./horizon-tv-backup.json /sdcard/Android/data/com.horizontv.launcher/files/horizon-tv-backup.json
adb -s <TARGET_IP>:5555 shell am start -n com.horizontv.launcher/.MainActivity --ez horizon_import_backup true
```

---

## 12. Setting Horizon as Your Default Launcher

### Standard Method
Open **Customize → Launcher & Apps → Set Default Home Launcher** and select **Horizon TV Launcher**.

### Nvidia Shield & Android TV (Android TV 11+) Method
```bash
adb -s <DEVICE_IP>:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
adb -s <DEVICE_IP>:5555 shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
```

### Google TV (Chromecast, Onn 4K, Google TV Streamer, TCL/Sony/Hisense Google TV)
```bash
adb -s <DEVICE_IP>:5555 shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb -s <DEVICE_IP>:5555 shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
```

To revert to the stock launcher at any time, re-enable the stock package with `pm enable <package>` and set it back as the home activity.

---

## 13. Building & Updating via ADB

### Build Release (~2.4 MB R8) or Debug APK
```bash
# Recommended R8-optimized release APK (~2.4 MB)
./gradlew assembleRelease

# Unminified debug build + unit tests
./gradlew assembleDebug testDebugUnitTest
```

### Update via ADB Without Losing Settings
Always install updates with `-r` (replace) and launch via `KEYCODE_HOME` so Horizon stays in Android TV's Home stack and keeps all your settings intact:
```bash
adb -s <DEVICE_IP>:5555 install -r HorizonTVLauncher-v2.5.61.apk
adb -s <DEVICE_IP>:5555 shell input keyevent KEYCODE_HOME
```
