# Horizon TV Launcher — Complete User Guide & Feature Reference

**Version:** `2.5.61`  
**Package ID:** `com.horizontv.launcher`  
**Repository:** https://github.com/darkside281/HorizonTVLauncher  
**Platform:** Android TV / Nvidia Shield / Google TV / Waydroid (Android 7.0+ / API 24+)  
**APK Size:** ~2.4 MB (R8-optimized universal APK — 100% free, open source, no ads, no paywalls, zero extra permissions)

---

## Table of Contents
1. [Screen Layout & First-Run Setup Wizard](#1-screen-layout--first-run-setup-wizard)
2. [Remote Control & D-Pad Shortcuts](#2-remote-control--d-pad-shortcuts)
3. [Customize Drawer (Easy Mode vs. Advanced Mode)](#3-customize-drawer-easy-mode-vs-advanced-mode)
4. [Managing Home Rows, App Folders & Individual Apps](#4-managing-home-rows-app-folders--individual-apps)
5. [Continue Watching / "Up Next" Row (Jellyfin, Plex & Emby)](#5-continue-watching--up-next-row-jellyfin-plex--emby)
6. [Tile Styling, Minimalist Dock & Clock Typography](#6-tile-styling-minimalist-dock--clock-typography)
7. [Wallpapers, Live Slideshows & Ambient Themes](#7-wallpapers-live-slideshows--ambient-themes)
8. [Built-In Screensaver & Android System DreamService](#8-built-in-screensaver--android-system-dreamservice)
9. [Live Weather Widget](#9-live-weather-widget)
10. [Universal Search, Voice Search & "Where to Watch"](#10-universal-search-voice-search--where-to-watch)
11. [Local Web Companion (Phone / PC Browser)](#11-local-web-companion-phone--pc-browser)
12. [Parental Controls (4-Digit PIN Lock)](#12-parental-controls-4-digit-pin-lock)
13. [Complete Customize Menu Map](#13-complete-customize-menu-map)
14. [Built-In GitHub Self-Updater](#14-built-in-github-self-updater)
15. [Backup, Restore, Reset & Syncing Multiple Devices](#15-backup-restore-reset--syncing-multiple-devices)
16. [Setting Horizon as Your Default Launcher](#16-setting-horizon-as-your-default-launcher)
17. [Building From Source & Updating via ADB](#17-building-from-source--updating-via-adb)

---

## 1. Screen Layout & First-Run Setup Wizard

- **Top Hero Area:** Displays your **Clock**, date, and **Live Weather** on the left, and quick-access action buttons (**Search**, **Customize**, **TV Inputs**, **Android Settings**) on the top right.
- **Full-Bleed Backdrop & ClearLogo:** Shows your chosen ambient gradient, custom image, or rotating live wallpaper (with optional **Ken Burns slow motion** and instant cold-start caching), with the official movie/show ClearLogo cleanly positioned above the dock.
- **Home Dock & Rows:** Displays your custom app rows, optional **Continue Watching** row, **2×2 App Folders**, and optional **All Apps** grid below.
- **Resolution-Independent TV Scaling:** Automatically scales to a fixed `1152dp` design width so tiles, fonts, and spacing look identical across 720p, 1080p, 4K Nvidia Shields, Google TV sticks, and Waydroid.
- **Smart Home Focus:** Opening Horizon or pressing the **Home** button immediately focuses **Row 1, Tile 1**.
- **4-Step First-Run Setup Wizard:** Fresh installs automatically launch a guided setup wizard covering **Basics** (dock style, clock format, default launcher), **Wallpapers**, **Region & Search**, and **Favorite Apps**. You can re-run it anytime under **Customize → Launcher & Apps → Run Setup Wizard…**.

---

## 2. Remote Control & D-Pad Shortcuts

| Button / Action | Where | What It Does |
| :--- | :--- | :--- |
| **D-Pad Up / Down** | Home Screen | Move between your custom app rows, Continue Watching, the All Apps grid (if enabled), and the top-right action bar. |
| **D-Pad Left / Right** | App Row / Continue Watching | Scroll horizontally through tiles in the active row. |
| **OK / Select (Short Press)** | App Tile / Folder / Resume Card | Launch the app, open the App Folder popup, or resume playback in Jellyfin/Plex/Emby (prompts for 4-digit PIN if app is locked). |
| **OK / Select (Long Press)** or **Menu** | App Tile | Open the **App Action Menu** (reorder, rename, custom icon, lock with PIN, hide, app info, uninstall). |
| **OK / Select (Long Press)** | App Folder Tile | Start **Inline Reorder Mode** to slide the folder left/right in its row. |
| **OK / Select** | Setup Dialog Text Fields | Click-to-edit: moving across Server URL, Username, Password, or API Key fields will **not** pop up the keyboard until you press **OK**. |
| **Home Button** | Anywhere | Return to Horizon and focus Row 1, Tile 1. |
| **Back Button** | Customize Drawer / Dialogs | Steps cleanly back from a drilled-in settings section to the Customize menu, or closes the active overlay / Reorder Mode. |
| **D-Pad Down / Search / Enter** | Search Text Box | Dismiss the on-screen keyboard and jump straight into filter chips and results. |
| **D-Pad Up** | Search Results / Filter Chips | Jump back up to Filter Chips → Search Box cleanly. |

---

## 3. Customize Drawer (Easy Mode vs. Advanced Mode)

Click the **Customize** palette icon in the top-right bar to open Horizon's `510dp` translucent right-side settings drawer. Your **real TV Home screen stays live and visible on the left** so you can watch layout, tile size, dock mode, and accent color changes happen in real time.

- **Switching Between Easy & Advanced Mode:** Use the **`[ Easy | Advanced ]`** toggle pill at the top-right of the drawer. Your choice is **saved automatically** across sessions, reboots, and Backup/Restore.
  - **Easy Mode (Default):** Clean, spacious everyday essentials — toggle **Minimalist Bottom-Dock**, **Continue Watching Row**, pick **Tile Size (`XS / Small / Medium / Large`)**, tap **Accent Color** swatches with instant live preview, and jump directly into **Wallpaper Source**, **Home Rows & App Folders**, **Weather**, **Phone / PC Web Companion**, **Updates**, or **Launcher, Backup & Setup Wizard**.
  - **Advanced Mode:** Shows all **10 full configuration sections** with live status subtitles.
- **Navigating Sub-Pages:** Click any section (`›`) to drill into its full settings. Press **Back** on your remote (or click **`‹ Back`** at the top-left of the drawer) to return to the menu, and press **Back** again to close Customize.

---

## 4. Managing Home Rows, App Folders & Individual Apps

### How to Add, Edit, Reorder & Delete Rows
Open **Customize → Home → Rows** (in Easy Mode, tap **Home Rows & App Folders**):
1. **Add a New Row:** Click **`+ Add Extra Row`** (or **`+ New Row`**), enter a name (e.g., *Movies*, *Live TV*, *Games*, *Android Tools*), and save.
2. **Add or Remove Apps in a Row:** Click the **`+`** button next to any row (or click the **`+ Add App`** tile at the end of a row on the Home screen) and check/uncheck installed apps or App Folders.
3. **Change Row Tile Shape (`16:9` / `1:1` / `2:3`):** Click the shape pill (`16:9`, `1:1`, or `2:3`) on any row to switch that individual row between **16:9 Banner**, **1:1 Square**, or **2:3 Poster** tiles.
4. **Move Rows Up / Down:** Use the `↑` and `↓` arrows on extra rows to change their vertical order on the Home screen.
5. **Rename or Delete Rows:** Click the pencil (`✎`) icon to rename a row, or the trash icon to remove extra rows you no longer need.
6. **Lock Home Layout:** Toggle **Lock Home Layout** ON to hide `+ Add App` tiles and prevent accidental remote edits or reordering.

### How to Reorder Tiles Inside a Row (4 Ways)
1. **Local Web Companion (Fastest):** Drag and drop tiles using the **`⋮⋮`** handle (works with mouse & mobile touch), or use `↑ / ↓`, **+ Add**, **Remove**, **Sort A–Z**, and **Clean Up Uninstalled**.
2. **Tile Order Dialog:** Open **Customize → Home → Rows** and press **`↑↓`** on any row with 2+ items (supports Grab & Move, quick nudge, and **A–Z** sort).
3. **Inline Reorder Mode (TV Remote):** Long-press **OK** on any app tile → **Reorder in [Row] (`◄ / ►`)** (or long-press **OK** on an App Folder) → use Left/Right to slide it, then press **OK** or **Back** to finish.
4. **Single-Step Move:** Long-press **OK** on an app tile → **Move Left** or **Move Right**.

### How to Create & Use App Folders (2×2 Preview Tiles)
Group related apps into compact **2×2 preview tiles** inside any row:
1. Open **Customize → Home → Rows** and scroll to **App Folders (2×2 Tiles)**.
2. Click **`+ New Folder`**, type a folder name (e.g., *Games* or *Utilities*), and select the apps to place inside.
3. Add the folder to any row using that row's **`+`** button (or via **Local Web Companion**).
4. On the Home screen, short-press **OK** on the folder to open its frosted 3-column popup grid (supports unlimited apps with scrolling), or **long-press OK** to slide the folder left/right in its row.
5. Edit a folder's apps (`+`), rename it (`✎`), or delete it anytime under **Customize → Home → Rows**.

### Customizing Individual Apps (Rename, Custom Icon, Hide, PIN Lock, Uninstall)
Long-press **OK** (or press **Menu**) on any app tile on the Home screen to open the **App Action Menu**:
- **Rename App:** Give any app a cleaner display name (leave blank to restore its original name).
- **Custom Icon / Banner:** Upload any PNG or JPG image from your TV storage (or from your phone/PC via **Web Companion → Wallpapers & Apps → Customize App**). Wide images (`≥ 1.35:1`) fill 16:9 tiles edge-to-edge; square icons render cleanly centered on a tinted glass card.
- **Lock App with PIN:** Require your 4-digit Parental PIN before opening the app (shows a lock badge on the tile).
- **Hide App:** Remove clutter/system apps from the All Apps grid (restore anytime under **Customize → Launcher & Apps → Hidden Apps**).
- **App Info / Uninstall:** Jump straight to Android's App Info screen or uninstall the app.

### All Apps Grid & Non-TV (Sideloaded) Apps
- Toggle the bottom **All Apps** grid on/off and set **4, 5, or 6 columns** under **Customize → Home → All Apps**.
- Toggle **Show Non-TV / Sideloaded Apps** under **Customize → Launcher & Apps** to include standard phone/tablet APK apps alongside Leanback TV apps.

---

## 5. Continue Watching / "Up Next" Row (Jellyfin, Plex & Emby)

Surface your in-progress movies and next episodes directly below your primary row using your existing media server connection (no extra permissions or third-party trackers):
1. Connect **Jellyfin**, **Plex**, or **Emby** under **Customize → Wallpapers** (or **Web Companion → Servers & API Keys**).
2. Enable **Continue Watching Row** right from **Customize → Easy Mode** (or **Customize → Home → Rows → Show Continue Watching Row**).
3. Horizon pulls your active resume items & next episodes into 16:9 cards displaying episode/movie badges (`S9 · E2`, `Movie`), time-remaining pills (`39m left`), titles, and progress bars neatly integrated into the card overlay (works cleanly even in **Minimalist Bottom-Dock Mode**).
4. Click any card to jump straight into Jellyfin, Plex, or Emby to resume playback.

---

## 6. Tile Styling, Minimalist Dock & Clock Typography

### Tile Style (**Customize → Home → Tile Style**)
- **Minimalist Bottom-Dock Mode:** Pushes the primary row lower on screen and hides its header so full-bleed wallpapers and ClearLogos shine.
- **Tile Size:** Choose **XS** (`134×76`), **Small** (`172×98`), **Medium** (`216×122`), or **Large** (`264×148`) — scales 1:1 and 2:3 rows proportionally too.
- **Corner Roundness:** **Sharp**, **Rounded**, or **Soft**.
- **Focus Border Style:** **Accent Glow**, **Crisp Outline**, **Zoom Only**, or **Soft Shadow**.
- **Tile Spacing:** **Compact**, **Normal**, or **Wide**.
- **Tile Opacity (Unfocused):** Adjust from **40% to 100%** so unfocused tiles blend softly into your artwork while the focused tile pops at 100%.
- **Show App Titles Under Tiles:** Toggle OFF for ultra-clean artwork-only banners, or ON to show app names underneath.

### Clock & Header (**Customize → Home → Clock & Header**)
- **Clock Style:** **Classic**, **Centered Cinema**, **Analog Dial**, or **Minimal**.
- **Clock Font:** **Modern Sans**, **Editorial Serif**, or **Tech Mono**.
- **Clock Weight:** **Light**, **Regular**, or **Bold**.
- **Show Clock / Show Date / 24-Hour Clock:** Toggle each independently.
- **Show TV Inputs (HDMI) Button:** Adds a quick HDMI/AV/Tuner input switcher in the top-right bar on Android TV / Google TV smart TVs (automatically hidden on streaming boxes without hardware inputs).

---

## 7. Wallpapers, Live Slideshows & Ambient Themes

Horizon supports **6 Ambient Gradient Themes**, **Single Custom Image Wallpapers**, and **7 Live Slideshow Providers** — all configurable under **Customize → Wallpapers** & **Customize → Appearance** (or from your phone/PC in **Web Companion → Wallpapers & Apps**).

### Ambient Themes & Accent Colors (**Customize → Appearance**)
- **6 Ambient Gradient Themes:** *Midnight Horizon*, *Aurora Borealis*, *Obsidian Ember*, *Nordic Frost*, *Sunset Velvet*, and *Cinema Noir* (featuring layered sky gradients and Horizon mountain silhouettes when no photo/live wallpaper is active).
- **6 Accent Colors:** Changes your focus glow, active pills, and UI highlights live on screen.
- **Wallpaper Dimming:** Adjust readability scrim from **20% (Bright)** to **85% (Cinema)**.

### Single Custom Image Wallpaper
- **On TV:** Go to **Customize → Wallpapers → Source & Setup → Local Folder → Pick Single Image…** (uses Android's system file picker — no storage permission needed).
- **From Phone / PC (Best for sticks without a file picker):** Open **Local Web Companion → Wallpapers & Apps → Custom Home Wallpaper**, choose any 16:9 JPG/PNG, and click **Upload Wallpaper**.
- **Clear Custom Wallpaper:** Tap **Clear Custom Wallpaper** in **Customize → Wallpapers** or the Web Companion to return to your Ambient Theme or Live Slideshow.

### Live Slideshow Wallpaper Providers (**Customize → Wallpapers**)

| Provider | How to Set Up | Content & Features |
| :--- | :--- | :--- |
| **Jellyfin** | Enter Server URL (`http://192.168.0.x:8096`) + API Key *or* Username & Password | Rotating fanart backdrops & official ClearLogos from multi-selected libraries. Filter by **Both**, **Movies**, or **TV Shows**. |
| **Plex** | Enter Server URL (`http://192.168.0.x:32400`) + `X-Plex-Token` *or* Plex Account Sign-In | High-res library backdrops from multi-selected Plex libraries. |
| **Emby** | Enter Server URL (`http://192.168.0.x:8096`) + API Key *or* Username & Password | Library backdrops & ClearLogos from multi-selected Emby libraries. |
| **TMDB** | Enter a free TMDB **v3 API Key** or **v4 Read Access Token** (`themoviedb.org/settings/api`) | 4K-ready movie & TV backdrops with official English ClearLogos across 6 selectable categories (*Trending*, *Popular Movies*, *Top Rated Movies*, *Now Playing*, *Popular TV*, *Top Rated TV*). |
| **Reddit** | No account or key needed — works immediately! | High-res widescreen photography & wallpapers (including Reddit galleries & automatic random-seeded 16:9 Wallhaven top-up) across **11 curated categories**: *Earth & Nature*, *Wallpapers*, *4K Wallpaper*, *Space & Cosmos*, *Cityscapes*, *Skies & Sunsets*, *Minimalist*, *Sci-Fi & Fantasy*, *Widescreen*, plus seasonal **Christmas** and **Winter & Snow** — and you can add any custom subreddit (`+ Add Subreddit`). Sort by **Hot**, **Top Week**, **Top Month**, or **Top All Time**. |
| **Local Folder & Photo Album** | Pick a folder on TV (`Pick Folder…`) **or** batch-upload multiple photos from your phone/PC in **Web Companion → Wallpapers & Apps → Local Folder Photo Album** | Rotates your personal photos on both the Home screen and Screensaver with zero extra permissions. |
| **SMB Network Share** | Scan your LAN & browse shared folders directly (Guest or Username/Password) | Streams wallpapers directly from a Windows PC, Synology/QNAP/TrueNAS NAS, Samba server, or router USB share over Wi-Fi/LAN. |

### Step-by-Step: Setting Up an SMB Network Share (With Scanner & Folder Browser)
- **On your TV:**
  1. Go to **Customize → Wallpapers → Source & Setup → SMB Share → Configure SMB Share…**.
  2. Click **Scan LAN** — Horizon scans your local network on port `445` in ~1.5 seconds and lists discovered SMB servers.
  3. Select a server (e.g., `NAS (192.168.0.50)`). If your share requires a login, press **OK** on **Username** and **Password** to enter your credentials first, then click **Browse Shares**.
  4. Drill into `📁` shares and subfolders (each folder displays how many JPG/PNG/WebP images it holds) and click **Connect & Start**.
- **From Phone / PC Browser:**
  1. Open **Web Companion → Wallpapers & Apps → SMB Network Share**.
  2. Click **🔍 Scan Network for SMB Servers**, tap a server, browse `📁` shares & subfolders, and click **✓ Use This Folder**.

### Slideshow Display Options (**Customize → Wallpapers → Slideshow & Display**)
- **Independent Per-Service Settings:** Every provider saves its own **Slideshow Speed** (`15s / 30s / 1m / 5m`), **Wallpaper Count** (`20–300`), **Media Type**, and **Show Movie/Show Logo** toggle independently.
- **Ken Burns Slow Motion:** Smooth GPU-accelerated cinema pan & zoom on your Home wallpaper (zero background recomposition overhead).
- **Next Backdrop / Pause Slideshow:** Skip immediately to the next backdrop or pause/resume rotation anytime.
- **Instant Cold-Start Cache & Auto-Reshuffle:** Horizon caches your last shown live backdrop locally so it appears on frame 1 at boot while fresh backdrops load in the background, and automatically re-shuffles the pool after every full cycle.

---

## 8. Built-In Screensaver & Android System DreamService

Open **Customize → Screensaver** to configure Horizon's full-screen screensaver:
- **Idle Screensaver:** Toggle ON and choose an idle timeout (`1m`–`30m`). Automatically pauses while any dialog, Setup Wizard, or App Folder is open.
- **Android System Screensaver (`DreamService`):** Tap **Android System Screensaver** to choose **Horizon Screensaver** as your TV's system-wide screensaver (scales identically at `1152dp` on 720p/1080p/4K panels and refreshes weather every 30 minutes overnight).
- **3 Modes:**
  1. **Wallpaper Artwork** — rotates backdrops from your active Wallpaper provider (Jellyfin, Plex, Emby, TMDB, Reddit, Local Folder, or SMB).
  2. **My Photos** — rotates photos from a local folder or picked images.
  3. **Ambient Mode** — cycles through Horizon's 6 ambient gradient themes.
- **Visuals & Overlays:** Toggle **Ken Burns Motion**, **OLED Burn-In Shift** (periodically shifts clock/weather corner), **Slide Interval**, **Dimming**, **Clock**, **Weather**, and **Show Title / ClearLogo**, or click **Preview Screensaver Now** to test immediately.

---

## 9. Live Weather Widget

Powered by **Open-Meteo** (100% free, no API key required) under **Customize → Weather** (or **Web Companion → Screensaver & Weather**):
- **Set Location:** Search any city or town worldwide (easiest via the Web Companion keyboard or TV remote).
- **Forecast & Style:** Toggle weather on/off, choose **1–7 days** of daily high/low forecasts, switch **Celsius (`°C`) / Fahrenheit (`°F`)**, choose **Small / Medium / Large** sizing, pick from **4 Icon Styles** (*Vivid Color*, *Minimal*, *Emoji*, *Accent*), toggle the translucent card backdrop, or trigger a manual refresh.

---

## 10. Universal Search, Voice Search & "Where to Watch"

Click **Search** in the top-right bar to search streaming services, your Jellyfin library, and installed apps from one screen:
- **Zero-Permission Voice Search:** Click the microphone icon beside the search box to speak your query using Android's built-in speech recognizer (no `RECORD_AUDIO` permission needed).
- **Recent Searches:** Your last 8 searches appear as quick-pick chips when Search opens (with **Clear All**).
- **Full Title Details Popup:** Click any movie or TV show to view its poster, full synopsis, runtime, genres, IMDb & Rotten Tomatoes scores, age rating, directors, cast, and all streaming options (can be toggled under **Customize → Search → General**).
- **Overview Screen Deep-Links:** Selecting a streaming offer opens its **Title Overview / Details** screen rather than immediately auto-playing on Prime Video (and Prime Channels), Netflix, Disney+, Apple TV, ITVX, Paramount+, and Crunchyroll.

### Configuring Search (**Customize → Search**)
1. **General:** Toggle the Search button, choose your **Country / Region** (`GB`, `US`, `CA`, `AU`, `IE`, `DE`, `FR`, `NL`), toggle **Open Full Details on Click**, toggle Rotten Tomatoes & Age Rating badges, and enable/disable core providers (Apps, JustWatch, Jellyfin, YouTube, Web).
2. **My Services:** Toggle **Only Show My Streaming Services**, **Include Rent & Buy Offers**, and **Hide Titles Not on My Services**, and select your subscriptions across **Main Services**, **Prime Video Channels**, and **Rent & Buy**.
3. **Streaming Apps:** Toggle individual installed streaming app search shortcuts or click **Rescan Installed Streaming Apps**.

---

## 11. Local Web Companion (Phone / PC Browser)

The fastest way to organize tiles, paste API keys, upload photos/icons, or set up SMB shares — all over your local Wi-Fi with zero cloud accounts:
1. Open **Customize → Phone / PC Web Companion** (or **Launcher & Apps → Local Web Companion…**).
2. Scan the **QR code** with your phone (auto-signs in) or open `http://<TV-IP>:8765` in a PC browser and type the **4-digit session code**.
3. **Built-in Security:** Uses a fresh random 4-digit PIN every session with a **5-attempt / 30-second lockout** on failed guesses, and shuts down immediately when you close the dialog on your TV.
4. **6 Control-Center Tabs:**
   - **Home & Dock:** Drag-and-drop (`⋮⋮`) tile reordering, real TV app icons, add/remove apps, sort A–Z, clean up uninstalled apps, manage rows, tile sizes (`XS`–`Large`), shapes, corners, focus style, and clock style.
   - **Search & Services:** Region, subscribed streaming services, Prime Channels, and search preferences.
   - **Wallpapers & Apps:** Switch themes/accents live, configure slideshow providers, batch-upload photos to your **Local Folder Photo Album**, scan & browse **SMB Network Shares**, upload a custom Home wallpaper, and rename/hide/upload custom app icons.
   - **Screensaver & Weather:** Search cities from your keyboard and adjust all Weather & Screensaver settings.
   - **Servers & API Keys:** Paste Jellyfin, Plex, Emby, SMB, and TMDB URLs and credentials directly from your clipboard.
   - **Security & Backup:** Manage Parental PIN settings and download/restore timestamped JSON backups.

---

## 12. Parental Controls (4-Digit PIN Lock)

Open **Customize → Parental Controls** to protect your setup:
- **Set / Change / Remove 4-Digit PIN.**
- **Require PIN for Customize:** Locks the entire Customize drawer behind your 4-digit PIN.
- **Per-App PIN Lock:** Long-press **OK** on any app tile → **Lock App with PIN** (shows a lock badge on the tile and prompts for the PIN before launching).
- PIN settings and locked apps are preserved in JSON Backup & Restore.

---

## 13. Complete Customize Menu Map

| Section | What's Inside |
| :--- | :--- |
| **Home** | **Rows** (Continue Watching toggle, Lock Layout, Create/Rename/Move/Delete Rows, Per-Row `16:9 / 1:1 / 2:3` Shapes, Tile Order `↑↓`, App Folders `2×2`), **Tile Style** (Minimalist Dock, `XS/S/M/L` Size, Corner Roundness, Focus Style, Spacing, Opacity, Show Titles), **Clock & Header** (Clock Style, Font, Weight, Date, 24h, TV Inputs button), **All Apps** (Grid toggle & 4–6 columns). |
| **Search** | **General** (Region, Details Popup, Ratings, Core Providers), **My Services** (JustWatch filters + Main / Prime Channels / Rent & Buy), **Streaming Apps**. |
| **Wallpapers** | **Source & Setup** (Jellyfin, Plex, Emby, TMDB, Reddit + Christmas/Winter presets, Local Folder/Single Image, SMB Share with LAN Scanner & Browser), **Slideshow & Display** (Speed, Count, Media Type, Show Logo, Ken Burns Slow Motion, Dimming, Next Backdrop). |
| **Screensaver** | **Mode & Timing** (Preview, Android System Screensaver, Timeout, Mode, Interval), **Display & Overlays** (Ken Burns, OLED Burn-In Shift, Dimming, Clock/Weather/Title toggles). |
| **Weather** | Enable/disable, city search, 1–7 day forecast, size, `°C`/`°F`, 4 icon styles, card backdrop, manual refresh. |
| **Appearance** | 6 Ambient Gradient Themes, 6 Accent Colors, Wallpaper Dimming. |
| **Launcher & Apps** | **Local Web Companion (Phone / PC)…**, **Run Setup Wizard…**, Set Default Home Launcher, Open Play Store, Show Non-TV Apps, Refresh App List, **Backup Config… / Restore Config…**, **Reset Launcher to Defaults…**, and Hidden Apps. |
| **Parental Controls** | Set/Change/Remove 4-digit PIN, Require PIN for Customize, locked app count. |
| **Updates** | Check for Updates now, schedule (**Automatic / Every 7 Days / Manual Only**), release notes, and direct in-app APK updater. |
| **About** | Installed version & build number, What's New, Highlights, Package Details, and Android App Info. |

---

## 14. Built-In GitHub Self-Updater

Horizon checks https://github.com/darkside281/HorizonTVLauncher/releases for newer versions:
- Configure **Automatic**, **Every 7 Days**, or **Manual Only** under **Customize → Updates**.
- Automatically selects the tiny **~2.4 MB R8-optimized APK** (skipping `_full` fallback builds), downloads with a live progress bar, and opens Android's package installer.

---

## 15. Backup, Restore, Reset & Syncing Multiple Devices

Horizon exports your full configuration (rows, App Folders, app order, custom labels, theme, accent, wallpaper providers & per-service configs, local photo folders, weather location, search settings & subscriptions, parental PINs, Easy/Advanced mode, and screensaver) to a timestamped `.json` file (e.g., `horizon-tv-backup-2026-11-28_10-04.json`).

> **Security Note:** Backup JSON files include any configured API keys, tokens, or SMB passwords so restoring onto another device works seamlessly. Keep backup files private and do not share them publicly.

### Backup & Restore Methods
- **Local Web Companion (Easiest):** Open **Security & Backup** in your phone/PC browser to download or restore a `.json` backup in one click.
- **In-App File Picker:** Go to **Customize → Launcher & Apps → Backup Config… / Restore Config…**.
- **Reset to Defaults:** Two-step **Reset Launcher to Defaults…** under **Customize → Launcher & Apps**.
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

## 16. Setting Horizon as Your Default Launcher

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

## 17. Building From Source & Updating via ADB

### Build Release (~2.4 MB R8) or Debug APK
```bash
# Recommended R8-optimized release APK (~2.4 MB)
./gradlew assembleRelease

# Unminified debug build + unit tests
./gradlew assembleDebug testDebugUnitTest
```

### Update via ADB Without Losing Settings
Always install updates with `-r` (replace) and launch via `KEYCODE_HOME` so Horizon stays in Android TV's Home stack and preserves all settings:
```bash
adb -s <DEVICE_IP>:5555 install -r HorizonTVLauncher-v2.5.61.apk
adb -s <DEVICE_IP>:5555 shell input keyevent KEYCODE_HOME
```
