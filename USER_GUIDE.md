# Horizon TV Launcher — Complete User Guide & Feature Reference

**Version:** `2.5.17`  
**Package ID:** `com.horizontv.launcher`  
**Repository:** https://github.com/darkside281/HorizonTVLauncher  
**Platform:** Android TV / Nvidia Shield / Waydroid (Android 7.0+ / API 24+)

Horizon TV Launcher is a fast, ad-free, custom Android TV home screen built with Jetpack Compose. It pairs a clean cinema-style hero backdrop with customizable app rows, independent per-service live wallpapers, an idle screensaver, live weather forecasts, unified streaming & Jellyfin search, and a built-in GitHub Releases updater.

---

## 1. Screen Layout Overview

- **Fixed Top Hero Area:** Displays the hero clock, date, and live weather forecast on the left, with quick-access action buttons (**Search**, **Customize**, **Android Settings**) on the top right.
- **Full-Bleed Backdrop & ClearLogo:** Shows your chosen ambient gradient, static image, or rotating live wallpaper, with the official movie/show ClearLogo (or subtle Reddit `r/subreddit` credit) positioned cleanly above the bottom dock.
- **Single-Row Scrollable Bottom Dock:** Keeps your app rows anchored along the bottom, scrolling one row at a time vertically while each row scrolls horizontally.
- **Artwork-Only or Titled Tiles:** Toggle **Show App Titles Under Tiles** off for a sleek artwork-only dock, or keep labels visible underneath every tile.
- **Resolution-Independent Scaling:** Automatically scales to a fixed `1152dp` design width so tiles, typography, and spacing look identical across 720p, 1080p, 4K Nvidia Shields, and Waydroid.
- **Smart Startup & Home Focus:** Opening Horizon or pressing the **Home** button immediately focuses **Row 1, Tile 1** with a symmetrical accent glow.
- **Edge-Locked Rows:** Pressing Left on the first tile or Right on the last tile stays inside the row instead of jumping unexpectedly into the top bar.

---

## 2. Remote Control & Navigation

| Button / Action | Where | What It Does |
| :--- | :--- | :--- |
| **D-Pad Up / Down** | Home Screen | Move between your custom app rows, the All Apps grid (if enabled), and the top-right action bar. |
| **D-Pad Left / Right** | App Row | Scroll horizontally through tiles in the active row. |
| **OK / Select (Short Press)** | App Tile | Launch the app. |
| **OK / Select (Long Press)** or **Menu** | App Tile | Open the **App Action Menu** (reorder, rename, change icon, hide, app info, uninstall). |
| **Home Button** | Anywhere | Return to Horizon and focus Row 1, Tile 1. |
| **Back Button** | Dialogs / Overlays | Close the current menu, search overlay, screensaver, or exit Reorder Mode. |
| **D-Pad Down / Search / Enter** | Search Text Box | Dismiss the on-screen Android TV keyboard and jump straight into the filter chips and results. |
| **D-Pad Up** | Search Results / Filter Chips | Scroll back to the top result → Filter Chips → Search Box cleanly. |
| **D-Pad Right** | Rightmost Filter Chip (or empty Search Box) | Jump to the **Close (`×`)** button. |

---

## 3. Managing Rows & App Tiles

### Custom Rows
Open **Customize → Home** to manage your rows:
- **Create Row (`+ New Row`):** Add as many custom rows as you like (e.g., *Main Streaming*, *Media*, *Games*, *Utilities*).
- **Add / Remove Apps:** Select a row's apps button (or click the **`+ Add App`** tile at the end of a row) to check/uncheck installed apps.
- **Move Rows Up / Down:** Reorder rows vertically using the `↑` / `↓` buttons in **Customize → Home**.
- **Rename or Delete Rows:** Rename any row or delete extra rows you no longer need.

### Reordering Tiles (3 Ways)
1. **Tile Order Dialog (Recommended for bulk sorting):** Go to **Customize → Home** and press the **`↑↓`** button on any row with 2+ apps.
   - **Grab & Move:** Press **OK** on an app to grab it, use **Up / Down** to move it, then press **OK** or **Back** to drop it.
   - **Quick Nudge:** Use the row's inline `↑` / `↓` buttons.
   - **Alphabetical Sort:** Press **A–Z** to sort the entire row alphabetically in one click.
2. **Inline Reorder Mode:** Long-press **OK** on a tile → **Reorder in [Row] (`◄ / ►`)** → use Left/Right to slide the tile, then press **OK** or **Back** to finish.
3. **Single-Step Move:** Long-press **OK** on a tile → **Move Left** or **Move Right**.

### Tile Appearance & Per-App Customization
- **Tile Size:** In **Customize → Home → Tiles**, choose **Small** (`172×98dp`), **Medium** (`216×122dp`), or **Large** (`264×148dp`).
- **Show App Titles Under Tiles:** Toggle on or off under **Customize → Home → Tiles** to switch between titled tiles and clean artwork-only banners.
- **Rename App:** Long-press **OK** → **Rename App** to give any app a custom label.
- **Custom Icon / Banner:** Long-press **OK** → **Change Icon / Banner** to pick a custom PNG/JPG image from storage (and **Reset Default Icon** anytime to revert).
- **Hide / Unhide Apps:** Long-press **OK** → **Hide App** to remove clutter. Restore hidden apps anytime under **Customize → Launcher & Apps → Hidden Apps**.
- **Lock Home Layout:** Enable **Lock Home Layout** in **Customize → Home** to hide `+ Add App` tiles and prevent accidental reorders, renames, hides, or uninstalls.

---

## 4. Universal Search & "Where to Watch"

Click the **Search** icon in the top-right bar to search across your streaming subscriptions, Jellyfin library, and installed apps at once. Use the filter tabs (**All Providers**, **Where to Watch**, **Jellyfin**, **Apps**) to narrow results:

| Source | What It Searches | How It Works |
| :--- | :--- | :--- |
| **Where to Watch (JustWatch UK)** | Live movies & TV shows across UK streaming services | Shows posters, years, IMDb scores, and streaming service pills (Subscription, Free/Ads, Rent, Buy). Selecting any movie or TV show opens its **Overview / Details screen** directly inside the streaming app instead of auto-playing. |
| **Jellyfin Movies & TV Shows** | Your personal Jellyfin server library | Shows 2:3 vertical posters, years, and ratings, and launches directly into the Jellyfin TV app. |
| **Matching Apps** | Installed TV & sideloaded apps | Only appears once you start typing (keeping the empty search screen clean) and matches custom names, original names, and package IDs. |
| **Streaming App Shortcuts, YouTube & Web** | Auto-detected installed apps (Netflix, Prime Video, Disney+, Crunchyroll, Plex, Apple TV, Max, Hulu, Spotify) + YouTube/SmartTube & Web | One-click search handoff into installed streaming apps, YouTube, or your browser. |

### Overview Screen Deep-Linking
Selecting a movie or TV show from **Where to Watch** opens the title's **Overview / Details** screen (instead of immediately starting playback) across:
- **Amazon Prime Video & Prime Video Channels** (Paramount+, MGM+, Crunchyroll, Discovery+, Shudder, MUBI, etc.)
- **Netflix**
- **Disney+**
- **Apple TV**
- **ITVX**
- **Paramount+**
- **Crunchyroll** (automatically routes directly into the installed Crunchyroll TV app)

### Organizing Search & Filtering to Your Subscriptions
Open **Customize → Search**, organized into 3 clean sub-tabs:
1. **General:** Toggle the top-right Search button and enable/disable core providers (JustWatch UK, Jellyfin, Installed Apps, YouTube, Web).
2. **My Services:**
   - **Only Show My Streaming Services:** Filter JustWatch results to only the subscriptions you own.
   - **Include Rent & Buy Offers:** Choose whether to show Rent/Buy offers or stick to Included/Free streams only.
   - **Hide Titles Not on My Services:** Hide title cards that aren't currently available on your selected services.
   - **Category Switcher (`Main` / `Prime Channels` / `Rent & Buy`):** Pick your services by category with live selected counts, including dedicated **Prime Video Channels** (*Paramount+*, *MGM+*, *Crunchyroll*, *Discovery+*, *Shudder*, *MUBI*, *BFI Player*, *Acorn TV / BritBox*, *Hayu*, *Curzon / Studiocanal / Arrow*, *Max / Lionsgate+ / Sony*, *Hidive / History Hit / Docs*, and *All Other Prime Video Channels*), plus **Select All** / **Clear All**.
3. **Streaming Apps:** Toggle individual installed streaming app search shortcuts or **Rescan Installed Streaming Apps**.

---

## 5. Live Wallpapers & Ambient Themes

Open **Customize → Wallpapers** to turn your TV home screen into a rotating high-resolution showcase.

### Supported Live Wallpaper Providers

| Provider | Authentication Needed | Content & Features |
| :--- | :--- | :--- |
| **Jellyfin** | Server URL + API Key (or Username/Password) | Rotating fanart backdrops & official ClearLogos from your multi-selected Jellyfin libraries. Filter by **Both**, **Movies**, or **TV Shows**. |
| **Plex** | Server URL + Plex Token (`X-Plex-Token`) | High-res library backdrops from multi-selected Plex libraries. |
| **Emby** | Server URL + API Key (or Username/Password) | Library backdrops & ClearLogos from multi-selected Emby libraries. |
| **TMDB (`themoviedb.org`)** | Free TMDB **v3 API Key** or **v4 Read Access Token** | 4K-ready movie & TV backdrops with official English ClearLogos across 6 selectable categories: *Trending Now*, *Popular Movies*, *Top Rated Movies*, *Now Playing*, *Popular TV Shows*, and *Top Rated TV Shows*. |
| **Reddit** | None (works immediately) | High-res photography & wallpapers from 9 preset subreddits (`r/EarthPorn`, `r/wallpapers`, `r/wallpaper`, `r/SpacePorn`, `r/CityPorn`, `r/SkyPorn`, `r/MinimalWallpaper`, `r/ImaginaryLandscapes`, `r/WidescreenWallpaper`) plus any custom subreddit you add. Sort by **Hot**, **Top Week**, **Top Month**, or **Top All Time**. |

### Independent Per-Service Settings
Each wallpaper provider (**Jellyfin**, **Plex**, **Emby**, **TMDB**, **Reddit**) saves its own independent configuration for:
- **Show Movie / Show Logo** (or **Show Subreddit Credit** on Reddit) — turning off Reddit credit will not turn off Jellyfin ClearLogos.
- **Slideshow Speed** (`15s`, `30s`, `1m`, `5m`).
- **Wallpaper Count** (`20`, `40`, `80`, `150`, `300`).
- **Media Type** (`Both`, `Movies`, `TV Shows`).

### Static Wallpaper & Ambient Themes
Under **Customize → Appearance**, you can also:
- Pick a **Custom Image Wallpaper** from local storage (and clear it anytime).
- Adjust **Wallpaper Dimming** (`20%`–`85%`) for maximum legibility.
- Choose from **6 Ambient Gradient Themes**: *Midnight Horizon*, *Aurora Borealis*, *Obsidian Ember*, *Nordic Frost*, *Sunset Velvet*, *Cinema Noir*.
- Choose from **6 Accent Colors**: *Sky Cyan*, *Electric Violet*, *Emerald Mint*, *Sunset Amber*, *Coral Rose*, *Pure Pearl*.

---

## 6. Built-In Screensaver & Android DreamService

Open **Customize → Screensaver** to configure Horizon's full-screen screensaver:
- **Idle Screensaver:** Automatically starts after a configurable idle timeout (`1m`, `3m`, `5m`, `10m`, `15m`, `30m`) on the Home screen.
- **Android System Screensaver:** Horizon also registers as a native Android TV `DreamService` — tap **Android System Screensaver** to select **Horizon Screensaver** as your system-wide TV screensaver.
- **3 Screensaver Modes:**
  1. **Wallpaper Artwork** — rotates high-res artwork from your active live wallpaper provider.
  2. **My Photos** — pick a local photo folder or individual photos from storage.
  3. **Ambient Themes** — smoothly cycles through Horizon's ambient gradient themes.
- **Display & OLED Protection Options:** Smooth **Ken Burns** pan/zoom motion, **OLED Burn-In Shift**, slide interval (`10s`–`5m`), dim level, and independent toggles for **Clock**, **Weather**, and **Movie/Show Logo / Title**.
- **Preview Screensaver Now:** Test your screensaver immediately with one click (press any button to exit).

---

## 7. Weather Widget

Horizon includes a built-in weather & forecast widget powered by **Open-Meteo** (free, no API key required).

Configure it under **Customize → Weather**:
- **Location Search:** Search for any city or town worldwide and select it from the results.
- **Forecast Length:** Display **1 to 7 days** of daily highs, lows, and conditions (or hide the multi-day forecast to show only current temperature).
- **Units:** Toggle between **Celsius (`°C`)** and **Fahrenheit (`°F`)**.
- **Widget Size:** Choose **Small**, **Medium**, or **Large**.
- **Icon Styles:** Choose from **Vivid Color**, **Minimal**, **Emoji**, or **Accent** (matches your chosen accent color).
- **Backdrop Pill:** Toggle the translucent dark card behind the weather widget on or off.

---

## 8. Customize Menu Map

| Tab | What's Inside |
| :--- | :--- |
| **Home** | Lock Home Layout, Tile Size (Small/Medium/Large), **Show App Titles Under Tiles**, Clock & Date (Show Clock, Show Date, 24-Hour Clock), Show `+ Add App` Tile, All Apps Grid (Show/Hide All Apps section, Grid Columns), and Row Management (add, rename, reorder, pick apps, open Tile Order `↑↓`). |
| **Search** | 3 organized sub-tabs: **General** (Search button & core providers), **My Services** (JustWatch filter toggles + Main / Prime Channels / Rent & Buy service picker), and **Streaming Apps** (installed app search shortcuts + Rescan). |
| **Wallpapers** | Choose provider (Jellyfin, Plex, Emby, TMDB, Reddit), configure server/API key, pick libraries/categories/subreddits, per-service Wallpaper Count, Slideshow Speed, Media Type, Show Logo / Subreddit Credit, Wallpaper Dimming, and Next Backdrop Now. |
| **Screensaver** | Preview Screensaver Now, Android System Screensaver shortcut, Idle Screensaver toggle & timeout, Mode (Wallpaper Artwork / My Photos / Ambient), Ken Burns, OLED Burn-In Shift, Slide Interval, Dimming, and Clock/Weather/Title toggles. |
| **Weather** | Enable/disable weather, pick city, 1–7 day forecast, forecast visibility & size, `°C`/`°F`, icon style, backdrop toggle, manual refresh. |
| **Appearance** | 6 ambient themes, 6 accent colors, pick/clear static custom wallpaper, wallpaper dimming. |
| **Launcher & Apps** | Set Default Home Launcher, Open Google Play Store, Show/Hide Non-TV (sideloaded) apps, Refresh App List, **Backup Config… / Restore Config…**, **Reset Launcher to Defaults…** (two-step confirmation), and Hidden Apps list (Unhide). |
| **About** | Installed version & build number, **Software Updates (GitHub)** card (**Check for Updates** / **Update to vX.Y.Z**), What's New, Highlights, Package Details, and Open Android App Info. |

---

## 9. Built-In GitHub Self-Updater

Horizon checks https://github.com/darkside281/HorizonTVLauncher/releases for newer versions:
- **Automatic Check:** Runs silently once per session ~4 seconds after startup.
- **Manual Check:** Open **Customize → About → Check for Updates** anytime.
- **Your Choice:** When a newer release is found, an **Update Available** dialog shows the version jump, APK size, and release notes with two buttons:
  - **Update Now:** Downloads the APK with a live progress bar and launches Android's package installer. *(On the first update, Android will ask you to enable **Install unknown apps** for Horizon TV Launcher — enable it once, then tap **Update Now** again.)*
  - **Not Now:** Dismisses the dialog; you can still install the update later from **Customize → About**.

---

## 10. Backup, Restore, Reset & Syncing Devices

Horizon can export your entire configuration (rows, app order, custom labels, theme, accent, wallpaper providers & per-service configs, weather location, search settings & selected streaming services, and screensaver) to a single portable `.json` file.

> **Security Note:** Backup JSON files include any configured API keys or tokens (such as Jellyfin, Plex, Emby, or TMDB keys). Keep your backup files private.

### In-App Backup, Restore & Reset
1. Go to **Customize → Launcher & Apps**.
2. Choose **Backup Config…** to save a `.json` file via Android's file picker.
3. On another TV/Shield, copy the `.json` file into the device's `Download` folder, open **Customize → Launcher & Apps → Restore Config…**, and select the file.
4. To start fresh, choose **Reset Launcher to Defaults…** → **Confirm Reset** (clears all custom rows, themes, wallpaper connections, weather, search filters, hidden apps, custom icons, and screensaver settings).

### Quick ADB Backup & Restore
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

## 11. Setting Horizon as Your Default Launcher

### Standard Method
Open **Customize → Launcher & Apps → Set Default Home Launcher** and select **Horizon TV Launcher**.

### Nvidia Shield & Android TV (Android TV 11+) Method
On Android TV devices where Google's stock launcher (`com.google.android.tvlauncher`) holds higher system priority, disable it via ADB so the **Home** button always opens Horizon:

```bash
adb -s <DEVICE_IP>:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
adb -s <DEVICE_IP>:5555 shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
```

### Google TV (Chromecast, Onn 4K, Google TV Streamer, TCL/Sony/Hisense Google TV)
On Google TV devices, disable the Google TV launcher package (`com.google.android.apps.tv.launcherx`) instead:

```bash
adb -s <DEVICE_IP>:5555 shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb -s <DEVICE_IP>:5555 shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
```

To revert back to the stock launcher at any time, re-enable the stock package with `pm enable <package>` and set it back as the home activity.

---

## 12. Building & Updating via ADB

### Build & Run Unit Tests
```bash
./gradlew --no-daemon assembleDebug testDebugUnitTest
```

### Update via ADB Without Losing Settings
Always install updates with `-r` (replace) and launch via `KEYCODE_HOME` so Horizon stays in Android TV's Home stack and keeps all your settings intact:
```bash
adb -s <DEVICE_IP>:5555 install -r HorizonTVLauncher-v2.5.17.apk
adb -s <DEVICE_IP>:5555 shell input keyevent KEYCODE_HOME
```
