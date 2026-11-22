# Horizon TV Launcher — Complete Guide & Feature Reference

**Version:** `2.4.0` (`versionCode 43`)  
**Package ID:** `com.horizontv.launcher`  
**Platform:** Android TV / Nvidia Shield / Waydroid (Android 8.0+)

Horizon TV Launcher is a fast, ad-free, custom Android TV home screen built with Jetpack Compose. It combines a clean cinema-style hero backdrop with customizable app rows, live media wallpapers, weather forecasts, and unified search across your apps, Jellyfin library, and UK streaming services.

---

## 1. Screen Layout Overview

- **Fixed Top Hero Area:** Displays the hero clock, date, and live weather forecast on the left, with quick-access action buttons (**Search**, **Customize**, **Android Settings**) on the top right.
- **Full-Bleed Backdrop & ClearLogo:** Shows your chosen ambient gradient, static image, or rotating live wallpaper, with the official movie/show ClearLogo and title caption positioned cleanly above the bottom dock.
- **Single-Row Scrollable Bottom Dock:** Keeps your app rows anchored along the bottom, scrolling one row at a time vertically while each row scrolls horizontally.
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
| **Back Button** | Dialogs / Overlays | Close the current menu, search overlay, or exit Reorder Mode. |
| **D-Pad Down / Search / Enter** | Search Text Box | Dismiss the on-screen Android TV keyboard and jump straight into search results. |

---

## 3. Managing Rows & App Tiles

### Custom Rows
Open **Customize → Home** to manage your rows:
- **Create Row (`+ New Row`):** Add as many custom rows as you like (e.g., *Favorites*, *Streaming*, *Media*, *Games*, *Utilities*).
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
- **Tile Size:** In **Customize → Home**, choose **Small** (`172×98dp`), **Medium** (`216×122dp`), or **Large** (`264×148dp`).
- **Rename App:** Long-press **OK** → **Rename App** to give any app a custom label.
- **Custom Icon / Banner:** Long-press **OK** → **Change Icon / Banner** to pick a custom PNG/JPG image from storage (and **Reset Default Icon** anytime to revert).
- **Hide / Unhide Apps:** Long-press **OK** → **Hide App** to remove clutter. Restore hidden apps anytime under **Customize → Launcher & Apps → Hidden Apps**.
- **Lock Home Layout:** Enable **Lock Home Layout** in **Customize → Home** to prevent accidental reorders, renames, hides, or uninstalls.

---

## 4. Unified Search & "Where to Watch"

Click the **Search** icon in the top-right bar to search across multiple sources at once. Use the filter tabs (**All**, **Apps**, **Jellyfin**, **Where to Watch**) to narrow results:

| Source | What It Searches | How It Works |
| :--- | :--- | :--- |
| **Apps** | All visible installed TV & sideloaded apps | Matches custom names, original names, and package IDs. |
| **Jellyfin** | Your personal Jellyfin server library | Shows posters, years, ratings, and launches directly into the Jellyfin app when configured. |
| **Where to Watch (JustWatch UK)** | Live movies & TV shows across UK streaming services | Displays posters, IMDb ratings, and provider badges (Subscription, Free/Ads, Rent, Buy). Selecting an offer deep-links directly into the streaming app on your TV. |
| **YouTube & Web** | Quick-launch external searches | Opens your query directly in YouTube or your TV browser. |
| **Installed Streaming Apps** | Auto-detected apps (Netflix, Disney+, Prime Video, BBC iPlayer, Apple TV, Crunchyroll, etc.) | One-click search handoff into installed streaming apps. |

Enable or disable individual search providers (and show/hide the top-right Search button) under **Customize → Search**.

---

## 5. Live Wallpapers & Ambient Themes

Open **Customize → Wallpapers** to turn your TV home screen into a rotating high-resolution showcase.

### Supported Live Wallpaper Providers

| Provider | Authentication Needed | Content & Features |
| :--- | :--- | :--- |
| **Jellyfin** | Server URL + API Key (or Username/Password) | Rotating fanart backdrops & official ClearLogos from your chosen Jellyfin libraries. Filter by **Both**, **Movies**, or **TV Shows**. |
| **Plex** | Server URL + Plex Token (`X-Plex-Token`) | High-res library backdrops from selected Plex libraries. |
| **Emby** | Server URL + API Key (or Username/Password) | Library backdrops & ClearLogos from selected Emby libraries. |
| **TMDB (`themoviedb.org`)** | Free TMDB **v3 API Key** or **v4 Read Access Token** | 4K-ready movie & TV backdrops with official English ClearLogos across 6 selectable categories: *Trending Now*, *Popular Movies*, *Top Rated Movies*, *Now Playing*, *Popular TV Shows*, and *Top Rated TV Shows*. |
| **Reddit** | None (works immediately) | High-res photography & wallpapers from 9 preset subreddits (`r/EarthPorn`, `r/wallpapers`, `r/wallpaper`, `r/SpacePorn`, `r/CityPorn`, `r/SkyPorn`, `r/MinimalWallpaper`, `r/ImaginaryLandscapes`, `r/WidescreenWallpaper`) plus any custom subreddit you add. Sort by **Hot**, **Top Week**, **Top Month**, or **Top All Time**. |

### Slideshow Controls
- **Rotation Interval:** Choose how often the wallpaper changes.
- **Backdrop Pool Size:** Control how many backdrops are fetched and rotated (up to 200).
- **Show Title / ClearLogo:** Toggle the movie/show logo and title caption above the bottom dock.
- **Next Wallpaper:** Skip immediately to the next backdrop from **Customize → Wallpapers**.

### Static Wallpaper & Ambient Themes
Under **Customize → Appearance**, you can also:
- Pick a **Custom Image Wallpaper** from local storage (and clear it anytime).
- Adjust **Wallpaper Dimming** (`0%`–`80%`) for maximum legibility.
- Choose from **6 Ambient Gradient Themes**: *Midnight Horizon*, *Aurora Borealis*, *Obsidian Ember*, *Nordic Frost*, *Sunset Velvet*, *Cinema Noir*.
- Choose from **6 Accent Colors**: *Sky Cyan*, *Electric Violet*, *Emerald Mint*, *Sunset Amber*, *Coral Rose*, *Pure Pearl*.

---

## 6. Weather Widget

Horizon includes a built-in weather & forecast widget powered by **Open-Meteo** (free, no API key required).

Configure it under **Customize → Weather**:
- **Location Search:** Search for any city or town worldwide and select it from the results.
- **Forecast Length:** Display **1 to 7 days** of daily highs, lows, and conditions (or hide the multi-day forecast to show only current temperature).
- **Units:** Toggle between **Celsius (`°C`)** and **Fahrenheit (`°F`)**.
- **Widget Size:** Choose **Small**, **Medium**, or **Large**.
- **Icon Styles:** Choose from **Vivid Color**, **Minimal**, **Emoji**, or **Accent** (matches your chosen accent color).
- **Backdrop Pill:** Toggle the translucent dark card behind the weather widget on or off.

---

## 7. Customize Menu Map

| Tab | What's Inside |
| :--- | :--- |
| **Home** | Manage Rows (add, rename, reorder, pick apps, open Tile Order `↑↓`), Tile Size (Small/Medium/Large), Clock & Date (Show Clock, Show Date, 24-Hour Clock), Show `+ Add App` Tile, Lock Home Layout, All Apps Grid (Show/Hide All Apps section, Grid Columns). |
| **Search** | Show/Hide Search button, toggle Apps, Jellyfin, JustWatch UK, YouTube, Web, and detected streaming apps. |
| **Wallpapers** | Choose provider (Jellyfin, Plex, Emby, TMDB, Reddit), configure server/API key, pick libraries/categories/subreddits, set rotation interval, media type filter, title/ClearLogo toggle, Next Wallpaper. |
| **Weather** | Enable/disable weather, pick city, 1–7 day forecast, forecast visibility & size, `°C`/`°F`, icon style, backdrop toggle, manual refresh. |
| **Appearance** | 6 ambient themes, 6 accent colors, pick/clear static custom wallpaper, wallpaper dim slider. |
| **Launcher & Apps** | Set Default Home Launcher, Open Google Play Store, Show/Hide Non-TV (sideloaded) apps, Hidden Apps list (Unhide), Refresh App List, Export/Restore JSON Backup, Horizon App Info. |
| **About** | Aurora Ridge branding, installed version name & version code, launcher summary. |

---

## 8. Backup, Restore & Syncing Devices

Horizon can export your entire configuration (rows, app order, custom labels, theme, accent, wallpaper provider & keys, weather location, search settings) to a single portable `.json` file.

> **Security Note:** Backup JSON files include any configured API keys or tokens (such as Jellyfin, Plex, Emby, or TMDB keys). Keep your backup files private.

### In-App Backup & Restore
1. Go to **Customize → Launcher & Apps**.
2. Choose **Export Backup** to save a `.json` file via Android's file picker.
3. On another TV/Shield, copy the `.json` file into the device's `Download` folder, open **Customize → Launcher & Apps → Restore Backup**, and select the file.

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

## 9. Setting Horizon as Your Default Launcher

### Standard Method
Open **Customize → Launcher & Apps → Set Default Home Launcher** and select **Horizon TV Launcher**.

### Nvidia Shield (Android TV 11) Method
On Nvidia Shield TV running Android TV 11, Google's stock launcher (`com.google.android.tvlauncher`) holds higher system priority. To make the **Home** button always open Horizon:

```bash
adb -s <SHIELD_IP>:5555 shell pm disable-user --user 0 com.google.android.tvlauncher
adb -s <SHIELD_IP>:5555 shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
```

To revert back to the stock Nvidia Shield launcher at any time:

```bash
adb -s <SHIELD_IP>:5555 shell pm enable com.google.android.tvlauncher
adb -s <SHIELD_IP>:5555 shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
```

---

## 10. Project Files, Building & Updating

- **Source Project:** `/home/david/.gemini/antigravity/scratch/horizon-tv-launcher`
- **Latest APK on Desktop:** `/home/david/Desktop/HorizonTVLauncher-v2.4.0.apk`

### Build & Run Unit Tests
```bash
./gradlew --no-daemon assembleDebug testDebugUnitTest
```

### Update Without Losing Settings
Always install updates with `-r` (replace) — never uninstall first — so your rows, custom icons, themes, and API keys stay intact:
```bash
adb -s <DEVICE_IP>:5555 install -r /home/david/Desktop/HorizonTVLauncher-v2.4.0.apk
```
