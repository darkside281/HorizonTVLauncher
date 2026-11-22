<p align="center">
  <img src="docs/logo.svg" alt="Horizon TV Launcher Logo" width="112" height="112" />
</p>

<h1 align="center">Horizon TV Launcher</h1>

<p align="center">
  A clean, fast, ad-free <strong>Android TV / Nvidia Shield home launcher</strong> built with Jetpack Compose — designed for living-room full-bleed artwork, crisp D-pad navigation, and universal streaming &amp; library search.
</p>

> **Latest Release:** Download [`HorizonTVLauncher-v2.5.17.apk`](https://github.com/darkside281/HorizonTVLauncher/releases/latest) from the [Releases](https://github.com/darkside281/HorizonTVLauncher/releases) page.

---

## Screenshots

### Home Screen & Live Artwork
Full-bleed widescreen wallpapers with ClearLogos, live weather, and customizable artwork-only or titled app rows.

![Horizon TV Launcher Home Screen](docs/screenshots/home.png)

### Universal Search (Where to Watch + Jellyfin + Apps)
Search across UK streaming services (filtered to your subscriptions & Prime Video Channels), your own Jellyfin server with vertical posters, and installed apps — opening movies and TV shows directly to their **Overview / Details** screen.

![Horizon TV Launcher Universal Search](docs/screenshots/search.png)

### Customize Overlay
TV-friendly two-pane settings overlay for rows, search providers, live wallpapers, screensaver, weather, appearance, and backup/restore.

![Horizon TV Launcher Customize Overlay](docs/screenshots/customize.png)

### Full-Screen Idle Screensaver & Android DreamService
Ken Burns widescreen artwork slideshow with optional clock, weather, burn-in shift, and ClearLogos.

![Horizon TV Launcher Screensaver](docs/screenshots/screensaver.png)

---

## What It Can Do

### Home Screen & App Organization
- **Custom Home Rows**: Create, rename, reorder, delete, and lock custom app rows.
- **Flexible Tiles**: Choose **Small / Medium / Large** tile sizes, toggle **App Titles Under Tiles** on/off for a clean artwork-only dock, rename apps, and assign custom PNG/JPG app icons.
- **All Apps Grid & Filtering**: Optional All Apps section with configurable 4–6 column layout, TV/Mobile app filtering, and app hiding/restoring.
- **Resolution-Independent TV Layout**: Scales consistently across 720p, 1080p, and 4K TVs.

### Universal Search & Where to Watch
- **JustWatch UK Integration**: See which streaming services carry any movie or TV show.
- **My Streaming Services & Prime Video Channels**: Filter search results to only the services you subscribe to — including dedicated **Prime Video Channels** (*Paramount+*, *MGM+*, *Crunchyroll*, *Discovery+*, *Shudder*, *MUBI*, *BFI Player*, *Acorn TV*, *Hayu*, *Curzon*, *Studiocanal*, and more) and optional Rent & Buy filtering.
- **Overview Screen Deep-Links**: Selecting a movie or TV show opens the **Title Overview / Details** page instead of immediately auto-playing on Prime Video (and Prime Channels), Netflix, Disney+, Apple TV, ITVX, Paramount+, and Crunchyroll.
- **Jellyfin Library Search**: Live search your Jellyfin server with 2:3 vertical posters and jump straight into Jellyfin TV.
- **Quick App & Web Search**: Search matching installed apps or jump straight to in-app search for installed streaming apps, YouTube/SmartTube, or the web.

### Live Wallpapers (Per-Service Configs)
- Supports **Jellyfin**, **Plex**, **Emby**, **TMDB**, **Reddit** (curated & custom subreddits), **Custom Local Image**, and built-in **Ambient Gradient Themes**.
- **Independent Per-Service Settings**: Each wallpaper provider saves its own **Show Movie/Show Logo** (or **Subreddit Credit**), **Slideshow Speed**, **Wallpaper Count**, and **Media Type** independently.
- **Multi-Select Libraries & Categories** plus adjustable wallpaper dimming.

### Built-In Screensaver & Android System DreamService
- **3 Modes**: Live Wallpaper Artwork, Local Photos / Folder, or Ambient Theme Cycle.
- Supports smooth **Ken Burns** motion, **OLED burn-in shift**, configurable idle timeout & slide interval, and optional Clock / Weather / Title overlays.
- Registers as an Android TV **System Screensaver (`DreamService`)** too.

### Live Weather
- Powered by **Open-Meteo** (no API key required).
- Configurable 1–7 day forecast, Celsius/Fahrenheit, 4 icon styles, compact/expanded sizing, and transparent overlay mode.

### Backup, Restore, Reset & Built-In GitHub Updater
- **GitHub Self-Updater**: Automatically checks GitHub Releases for newer versions on startup (or manually under **Customize → About**), shows release notes, and lets you choose **Update Now** (downloads and installs the APK directly on your TV) or **Not Now**.
- **Portable JSON Backup & Restore** of your full launcher configuration (rows, themes, wallpapers, per-service configs, weather, search filters, and screensaver) for easy transfer across devices.
- **Reset Launcher to Defaults** with two-step confirmation under **Customize → Launcher & Apps**.

For detailed setup instructions, see [USER_GUIDE.md](USER_GUIDE.md).

---

## Compatible Devices

Horizon TV Launcher targets **Android 7.0+ (API 24)** up to **Android 16**, requires **no Google Play Services**, scales cleanly across **720p, 1080p, and 4K**, and ships as a single universal APK (**ARM64, ARM32, and x86_64**).

- **Nvidia Shield TV** (2015, 2017, 2019, Pro) — *tested & verified*
- **Google TV devices** (Chromecast with Google TV, Google TV Streamer, Onn 4K / 4K Pro)
- **Android TV boxes & sticks** (Xiaomi Mi Box / TV Stick, Mecool, Homatics, generic Android TV boxes)
- **Smart TVs with Android TV / Google TV** (Sony Bravia, TCL, Hisense, Philips)
- **Waydroid** (Linux x86_64 / ARM64) — *tested & verified*
- **Amazon Fire TV (Fire OS 7+)** — app runs, though newer Fire OS versions restrict replacing the Home button

> **Notes:**
> - **Where to Watch (JustWatch)** currently queries **UK (`GB`)** streaming availability; all other features (apps, Jellyfin/Plex/Emby/TMDB/Reddit wallpapers, weather, screensaver) work worldwide.
> - Devices without a built-in system file picker (some Google TV sticks) can still import/export backups via ADB (see [USER_GUIDE.md](USER_GUIDE.md)).

---

## Installation

1. Download the latest APK (`HorizonTVLauncher-v2.5.17.apk`) from the [Releases](https://github.com/darkside281/HorizonTVLauncher/releases) page.
2. Sideload it onto your Android TV / Google TV device (via USB, file manager, or ADB):
   ```bash
   adb install -r HorizonTVLauncher-v2.5.17.apk
   ```
3. Open **Horizon TV Launcher** → **Customize → Launcher & Apps → Set as Default Home**.
   - **Android TV (Nvidia Shield, Sony, etc.)** — optionally disable the stock launcher via ADB so the Home button always opens Horizon:
     ```bash
     adb shell pm disable-user --user 0 com.google.android.tvlauncher
     adb shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
     ```
   - **Google TV (Chromecast, Onn, Google TV Streamer, etc.)** — disable the Google TV launcher package instead:
     ```bash
     adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
     adb shell cmd package set-home-activity com.horizontv.launcher/.MainActivity
     ```

---

## Building From Source

Requires JDK 17 and the Android SDK (API 36).

```bash
git clone https://github.com/darkside281/HorizonTVLauncher.git
cd HorizonTVLauncher
./gradlew assembleDebug
```

The built APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Third-Party APIs, Attribution & Trademarks

- **TMDB:** This product uses the TMDB API but is not endorsed or certified by TMDB. Users supply their own personal TMDB API key.
- **Weather:** Weather and forecast data is provided by [Open-Meteo](https://open-meteo.com/).
- **Media Servers & Feeds:** Jellyfin, Plex, Emby, and Reddit wallpapers are fetched directly from the user's own configured servers or public read-only feeds at runtime; no third-party media artwork is bundled with this application.
- **Trademarks & Disclaimer:** Horizon TV Launcher is an independent, open-source project and is not affiliated with, endorsed by, or sponsored by Google, Android TV, Nvidia, Netflix, Amazon Prime Video, Disney+, Apple TV, Paramount+, Crunchyroll, ITVX, BBC, Channel 4, JustWatch, Jellyfin, Plex, Emby, TMDB, or Reddit. All product names, logos, and brands are the property of their respective owners.

## License

Licensed under the [MIT License](LICENSE).

