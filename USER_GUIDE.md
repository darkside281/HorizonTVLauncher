# Horizon TV Launcher — Complete User Guide & Feature Reference

**Version:** `2.5.41`  
**Package ID:** `com.horizontv.launcher`  
**Repository:** https://github.com/darkside281/HorizonTVLauncher  
**Platform:** Android TV / Nvidia Shield / Waydroid (Android 7.0+ / API 24+)

Horizon TV Launcher is a fast, ad-free, custom Android TV home screen built with Jetpack Compose. It pairs a clean cinema-style hero backdrop with customizable app rows, Minimalist Bottom-Dock mode, independent per-service live wallpapers, an idle screensaver, live weather forecasts, unified streaming & Jellyfin search with Voice Search, Parental Controls, a built-in Local Web Companion for phone/PC editing, and a GitHub Releases updater.

---

## 1. Screen Layout Overview

- **Fixed Top Hero Area:** Displays the hero clock, date, and live weather forecast on the left, with quick-access action buttons (**Search**, **Customize**, **TV Inputs**, **Android Settings**) on the top right.
- **Full-Bleed Backdrop & ClearLogo:** Shows your chosen ambient gradient, static image, or rotating live wallpaper, with the official movie/show ClearLogo positioned cleanly above the bottom dock.
- **Standard Dock or Minimalist Bottom-Dock Mode:** Use standard multi-row scrolling, or switch on **Minimalist Bottom-Dock Mode** (**Customize → Home → Rows & Tiles**) for a sleek frosted floating dock along the bottom edge.
- **Artwork-Only or Titled Tiles:** Toggle **Show App Titles Under Tiles** off for a clean artwork-only dock, and fine-tune **Tile Size**, **Tile Spacing**, and **Tile Opacity (Unfocused)**.
- **Resolution-Independent Scaling:** Automatically scales to a fixed `1152dp` design width so tiles, typography, and spacing look identical across 720p, 1080p, 4K Nvidia Shields, and Waydroid.
- **Smart Startup & Home Focus:** Opening Horizon or pressing the **Home** button immediately focuses **Row 1, Tile 1** with a symmetrical accent glow.
- **First-Run Setup Wizard:** Fresh installs automatically launch a 4-step Setup Wizard (Basics, Wallpapers, Region & Search, Favorite Apps). Existing setups skip it automatically, and you can re-run it anytime under **Customize → Launcher & Apps → Run Setup Wizard…**.

---

## 2. Remote Control & Navigation

| Button / Action | Where | What It Does |
| :--- | :--- | :--- |
| **D-Pad Up / Down** | Home Screen | Move between your custom app rows, the All Apps grid (if enabled), and the top-right action bar. |
| **D-Pad Left / Right** | App Row | Scroll horizontally through tiles in the active row. |
| **OK / Select (Short Press)** | App Tile | Launch the app (prompts for 4-digit PIN if app is locked). |
| **OK / Select (Long Press)** or **Menu** | App Tile | Open the **App Action Menu** (reorder, rename, change icon, lock with PIN, hide, app info, uninstall). |
| **Home Button** | Anywhere | Return to Horizon and focus Row 1, Tile 1. |
| **Back Button** | Dialogs / Overlays | Close the current menu, search overlay, screensaver, or exit Reorder Mode. |
| **D-Pad Down / Search / Enter** | Search Text Box | Dismiss the on-screen Android TV keyboard and jump straight into the filter chips and results. |
| **D-Pad Up** | Search Results / Filter Chips | Scroll back to the top result → Filter Chips → Search Box cleanly. |
| **D-Pad Right** | Rightmost Filter Chip (or empty Search Box) | Jump to the **Close (`×`)** button. |

---

## 3. Managing Rows & App Tiles

### Custom Rows
Open **Customize → Home** (organized into **Rows & Tiles**, **Clock & Header**, and **All Apps**) to manage your layout:
- **Create Row (`+ New Row`):** Add as many custom rows as you like (e.g., *Main Streaming*, *Media*, *Games*, *Utilities*).
- **Add / Remove Apps:** Select a row's apps button (or click the **`+ Add App`** tile at the end of a row) to check/uncheck installed apps — or manage them even faster from your phone/PC via the **Local Web Companion**.
- **Move Rows Up / Down:** Reorder rows vertically using the `↑` / `↓` buttons.
- **Rename or Delete Rows:** Rename any row or delete extra rows you no longer need.

### Reordering Tiles (4 Ways)
1. **Local Web Companion (Fastest):** Edit from your phone or PC browser with real app icons, one-tap **↑ / ↓**, **+ Add**, **Remove**, **Sort A–Z**, and **Clean Up Uninstalled**.
2. **Tile Order Dialog:** Go to **Customize → Home → Rows & Tiles** and press **`↑↓`** on any row with 2+ apps (supports Grab & Move, quick nudge, and **A–Z** sort).
3. **Inline Reorder Mode:** Long-press **OK** on a tile → **Reorder in [Row] (`◄ / ►`)** → use Left/Right to slide the tile, then press **OK** or **Back** to finish.
4. **Single-Step Move:** Long-press **OK** on a tile → **Move Left** or **Move Right**.

### Tile Appearance & Per-App Customization
- **Minimalist Bottom-Dock Mode:** Toggle under **Customize → Home → Rows & Tiles** for a translucent frosted dock at the bottom of the screen.
- **Tile Size, Spacing & Opacity:** Choose **Small / Medium / Large** tile sizes, **Compact / Normal / Wide** spacing, and **40%–100%** unfocused tile opacity.
- **Show App Titles Under Tiles:** Toggle on or off to switch between titled tiles and clean artwork-only banners.
- **Show TV Inputs (HDMI) Button:** Toggle on or off under **Customize → Home → Clock & Header**. On smart TVs that expose hardware inputs via `TvInputManager`, an HDMI button appears in the top-right bar (auto-hidden on streaming boxes with no hardware inputs).
- **Rename App / Custom Icon / Hide / PIN Lock:** Long-press **OK** on any app tile to rename it, upload a custom PNG/JPG icon/banner, lock it behind your Parental PIN, or hide it (restore anytime under **Customize → Launcher & Apps → Hidden Apps**).
- **Lock Home Layout:** Enable **Lock Home Layout** in **Customize → Home** to hide `+ Add App` tiles and prevent accidental remote edits.

---

## 4. Universal Search, Voice Search & "Where to Watch"

Click the **Search** icon in the top-right bar to search across your streaming subscriptions, Jellyfin library, and installed apps at once.
- **Zero-Permission Voice Search:** Click the microphone icon beside the search bar to speak a query using Android's system speech recognizer (no `RECORD_AUDIO` permission needed).
- **Recent Searches:** Your last 8 completed searches appear as quick-pick chips when Search opens (with a **Clear All** button).
- **Full Title Details Popup:** Clicking any movie or TV show opens a full details popup showing its poster, full synopsis, runtime, genres, IMDb & Rotten Tomatoes scores, age rating, directors, cast, and all streaming options (can be toggled under **Customize → Search → General**).

| Source | What It Searches | How It Works |
| :--- | :--- | :--- |
| **Where to Watch (JustWatch)** | Live movies & TV shows across 8 regions (`GB`, `US`, `CA`, `AU`, `IE`, `DE`, `FR`, `NL`) | Shows posters, plot synopses, years, IMDb & Rotten Tomatoes scores, age ratings, and streaming service pills (Subscription, Free/Ads, Rent, Buy). Selecting a service opens its **Overview / Details screen** directly inside the streaming app. |
| **Jellyfin Movies & TV Shows** | Your personal Jellyfin server library | Shows 2:3 vertical posters, years, and ratings, and launches directly into the Jellyfin TV app. |
| **Matching Apps** | Installed TV & sideloaded apps | Appears once you start typing and matches custom names, original names, and package IDs. |
| **Streaming App Shortcuts, YouTube & Web** | Auto-detected installed apps + YouTube/SmartTube & Web | One-click search handoff into installed streaming apps, YouTube, or your browser. |

### Organizing Search & Filtering to Your Subscriptions
Open **Customize → Search**, organized into 3 clean sub-tabs:
1. **General:** Toggle the Search button, pick your **Search Country / Region**, toggle **Open Full Details on Click**, toggle Rotten Tomatoes & Age Rating badges, and enable/disable core providers.
2. **My Services:**
   - **Only Show My Streaming Services**, **Include Rent & Buy Offers**, and **Hide Titles Not on My Services**.
   - **Category Switcher (`Main` / `Prime Channels` / `Rent & Buy`):** Pick your services by category with live selected counts, including dedicated **Prime Video Channels** (*Paramount+*, *ITVX via Prime*, *MGM+*, *Crunchyroll*, *Discovery+*, *Shudder*, *MUBI*, *BFI Player*, *Acorn TV / BritBox*, *Hayu*, *Curzon / Studiocanal / Arrow*, *HBO Max / Lionsgate+ / Sony*, *Hidive / History Hit / Docs*, and *All Other Prime Video Channels*).
3. **Streaming Apps:** Toggle individual installed streaming app search shortcuts or **Rescan Installed Streaming Apps**.

---

## 5. Local Web Companion (Phone / PC Browser)

Tired of typing long URLs, API keys, or city names on a TV remote? Open **Customize → Launcher & Apps → Local Web Companion (Phone / PC)…** to start a tiny, temporary local web server right on your TV:
- Scan the **QR code** from your phone (auto-signs in) or open the displayed `http://<TV-IP>:8765` URL on your PC browser and enter the **4-digit session code**.
- All changes save and apply **live on your TV immediately** (no extra save step needed except on the **Servers & API Keys** tab).
- **5 Built-In Tabs:**
  1. **Home & Dock:** Rearrange dock/row tiles with real TV app icons and friendly names, add any installed Android app, sort A–Z, clean up uninstalled backup apps, manage rows, and toggle Minimalist Dock / clock / opacity / spacing / search region.
  2. **Wallpapers & Icons:** Switch Ambient Themes & Accent colors, configure Live Slideshow Wallpaper provider/interval/dimming/titles, upload or clear a custom Home wallpaper, and upload custom app icons.
  3. **Screensaver & Weather:** Search any city worldwide and set your Weather location right from your phone/PC keyboard, toggle Weather display options, and configure all Screensaver settings.
  4. **Servers & API Keys:** Paste Jellyfin, Plex, Emby, and TMDB URLs and API keys directly from your clipboard.
  5. **Backup & Restore:** Download your full Horizon JSON backup or upload one to restore immediately.
- When finished, press **Stop Server & Close** on the TV to shut down the local server immediately.

---

## 6. Live Wallpapers & Ambient Themes

Open **Customize → Wallpapers** to turn your TV home screen into a rotating high-resolution showcase:

| Provider | Authentication Needed | Content & Features |
| :--- | :--- | :--- |
| **Jellyfin** | Server URL + API Key (or Username/Password) | Rotating fanart backdrops & official ClearLogos from your multi-selected Jellyfin libraries. Filter by **Both**, **Movies**, or **TV Shows**. |
| **Plex** | Server URL + Plex Token (`X-Plex-Token`) | High-res library backdrops from multi-selected Plex libraries. |
| **Emby** | Server URL + API Key (or Username/Password) | Library backdrops & ClearLogos from multi-selected Emby libraries. |
| **TMDB (`themoviedb.org`)** | Free TMDB **v3 API Key** or **v4 Read Access Token** | 4K-ready movie & TV backdrops with official English ClearLogos across 6 selectable categories. |
| **Reddit** | None (works immediately) | High-res photography & wallpapers from 9 family-friendly curated categories (*Earth & Nature*, *Wallpapers*, *Widescreen Wallpapers*, *Space & Cosmos*, *Cityscapes*, *Skies & Sunsets*, *Minimal Wallpapers*, *Imaginary Landscapes*, *Desktop Wallpapers*) plus custom feeds. Sort by **Hot**, **Top Week**, **Top Month**, or **Top All Time**. |

### Independent Per-Service Settings & Appearance
- Each wallpaper provider saves its own **Show Movie/Show Logo**, **Slideshow Speed**, **Wallpaper Count**, and **Media Type** independently.
- Under **Customize → Appearance**, choose from **6 Ambient Gradient Themes** (*Midnight Horizon*, *Aurora Borealis*, *Obsidian Ember*, *Nordic Frost*, *Sunset Velvet*, *Cinema Noir*), **6 Accent Colors**, a **Custom Static Image Wallpaper**, and **Wallpaper Dimming** (`20%`–`85%`).

---

## 7. Built-In Screensaver & Android DreamService

Open **Customize → Screensaver** to configure Horizon's full-screen screensaver:
- **Idle Screensaver:** Automatically starts after a configurable idle timeout (`1m`–`30m`) on the Home screen.
- **Android System Screensaver:** Registers as a native Android TV `DreamService` — tap **Android System Screensaver** to choose **Horizon Screensaver** system-wide.
- **3 Modes:** **Wallpaper Artwork**, **My Photos** (local folder/images), or **Ambient Themes**.
- **Display & OLED Protection:** Smooth **Ken Burns** motion, **OLED Burn-In Shift**, slide interval, dim level, independent Clock / Weather / Title toggles, and **Preview Screensaver Now**.

---

## 8. Weather Widget

Powered by **Open-Meteo** (free, no API key required) and configurable under **Customize → Weather** (or from the **Local Web Companion**):
- **Location Search:** Search any city or town worldwide.
- **Forecast & Style:** Display **1–7 days** of daily highs/lows, toggle **Celsius (`°C`) / Fahrenheit (`°F`)**, **Small / Medium / Large** sizing, **4 Icon Styles** (*Vivid Color*, *Minimal*, *Emoji*, *Accent*), and optional translucent card backdrop.

---

## 9. Parental Controls (4-Digit PIN Lock)

Open **Customize → Parental Controls** to set a 4-digit PIN:
- **Require PIN for Customize:** Locks the Customize settings menu behind your 4-digit PIN.
- **Per-App PIN Lock:** Long-press **OK** on any app tile → **Lock App with PIN** (locked tiles display a lock badge and require the PIN before launching).
- PIN settings and locked app lists are preserved across JSON Backup & Restore.

---

## 10. Customize Menu Map

| Tab | What's Inside |
| :--- | :--- |
| **Home** | Sub-tabs for **Rows & Tiles** (Minimalist Dock, Lock Layout, Tile Size/Spacing/Opacity, Show Titles, Row Management, Tile Order `↑↓`), **Clock & Header** (Clock, Date, 24h, TV Inputs button), and **All Apps** (Grid visibility & columns). |
| **Search** | Sub-tabs for **General** (Region, Details Popup, Ratings, Core Providers), **My Services** (JustWatch filters + Main / Prime Channels / Rent & Buy), and **Streaming Apps**. |
| **Wallpapers** | Sub-tabs for **Source & Setup** (Provider, Server/API key, Libraries/Categories) and **Slideshow & Display** (Speed, Count, Media Type, Show Logo, Dimming, Next Backdrop). |
| **Screensaver** | Sub-tabs for **Mode & Timing** (Preview, System Screensaver, Timeout, Mode, Interval) and **Visuals & Overlays** (Ken Burns, OLED Burn-In Shift, Dimming, Clock/Weather/Title toggles). |
| **Weather** | Enable/disable weather, city search, 1–7 day forecast, size, `°C`/`°F`, icon style, backdrop toggle, manual refresh. |
| **Appearance** | 6 ambient themes, 6 accent colors, pick/clear static custom wallpaper, wallpaper dimming. |
| **Launcher & Apps** | **Local Web Companion (Phone / PC)…**, **Run Setup Wizard…**, Set Default Home Launcher, Open Play Store, Show/Hide Non-TV apps, Refresh App List, **Backup Config… / Restore Config…**, **Reset Launcher to Defaults…**, and Hidden Apps. |
| **Parental Controls** | Set/Change/Remove 4-digit PIN, Require PIN for Customize toggle, and locked app count. |
| **Updates** | Check for Updates now, configurable schedule (**Automatic / Every 7 Days / Manual Only**), and direct in-app APK updater. |
| **About** | Installed version & build number, What's New, Highlights, Package Details, and Android App Info. |

---

## 11. Built-In GitHub Self-Updater

Horizon checks https://github.com/darkside281/HorizonTVLauncher/releases for newer versions:
- Configure **Automatic**, **Every 7 Days**, or **Manual Only** under **Customize → Updates**.
- When a newer release is found, an **Update Available** dialog shows the version jump, APK size, and release notes with **Update Now** (downloads with live progress bar and opens Android's installer) or **Not Now**.

---

## 12. Backup, Restore, Reset & Syncing Devices

Horizon exports your entire configuration (rows, app order, custom labels, theme, accent, wallpaper providers & per-service configs, weather location, search settings & selected streaming services, parental controls, and screensaver) to a single portable `.json` file.

> **Security Note:** Backup JSON files include any configured API keys or tokens (such as Jellyfin, Plex, Emby, or TMDB keys). Keep your backup files private.

### Backup & Restore Methods
- **Local Web Companion (Easiest):** Open **Backup & Restore** in your phone/PC browser to download or upload/restore a `.json` backup in one click.
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

## 13. Setting Horizon as Your Default Launcher

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

To revert back to the stock launcher at any time, re-enable the stock package with `pm enable <package>` and set it back as the home activity.

---

## 14. Building & Updating via ADB

### Build & Run Unit Tests
```bash
./gradlew --no-daemon assembleDebug testDebugUnitTest
```

### Update via ADB Without Losing Settings
Always install updates with `-r` (replace) and launch via `KEYCODE_HOME` so Horizon stays in Android TV's Home stack and keeps all your settings intact:
```bash
adb -s <DEVICE_IP>:5555 install -r HorizonTVLauncher-v2.5.41.apk
adb -s <DEVICE_IP>:5555 shell input keyevent KEYCODE_HOME
```
