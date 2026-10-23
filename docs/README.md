# Ledger (Tools for Boox fork)

A fork of [Tools for Boox](https://github.com/gaborauth/toolsboox-android) by [Gabor Auth](https://github.com/gaborauth), optimized for daily planning on Boox e-ink tablets with pen color support, full-screen drawing, pinch-zoom for smaller devices, home screen widgets, and [Ultrabridge](https://github.com/jdkruzr/ultrabridge) integration for handwriting OCR and task sync.

## What's different in this fork

### Drawing & navigation
- **Full-screen immersive mode** — no system bars, no action bar, no dashboard; the planner page fills the entire screen edge-to-edge (including display cutout area). Swipe from a screen edge to briefly reveal back/home if you need them.
- **Pinch-zoom + two-finger pan** — the 1404x1872 canvas renders at native resolution on any device. Pinch to zoom (up to 4x), two-finger drag to pan, double-tap to toggle 2x zoom / reset. Designed for the Palma 2 Pro where the page would otherwise be cramped. Strokes are stored in canvas space, so zoom never distorts handwriting or affects export/sync.
- **Two-finger swipes for navigation** by default — left/right to change days, up/down for week/notes. Single-finger gestures are ignored to prevent accidental swipes while resting your hand on the screen. The top toolbar "hand" button toggles single-finger mode for when you want easier one-handed navigation.
- **Rotate screen** — toolbar button cycles through portrait, landscape, reverse-portrait, reverse-landscape. Choose which orientations the button visits in Settings → Rotation.
- **Adaptive toolbar** — single 40dp column on tall screens; expands to a 100dp two-column layout when the screen height drops below 720dp (Palma in landscape, Mini, etc.) so every icon stays reachable.
- **Pen color picker** — tap the pen while active to choose Black / Red / Blue / Green and Fine / Med / Thick / Bold
- **Red ink = tasks** — write in red and [Ultrabridge](https://github.com/jdkruzr/ultrabridge) automatically creates a todo from your handwriting
- **Lasso, copy, paste** — select strokes, move them around, paste with per-stroke color persistence
- **Undo / Redo** — 50-step history
- **Collapsible toolbar** — grey strip when collapsed, tap to expand; keeps the drawing area clean
- **Phosphor icons throughout** — clean outline-style toolbar from [Phosphor Icons](https://github.com/phosphor-icons/core)
- **Redesigned navigator strip** — clean Atkinson Hyperlegible typography, current granularity in bold focal type, siblings in muted context. Same look across Day / Week / Month / Quarter / Year views.

### Viwoods AiPaper Mini support
The fork also runs on the **Viwoods AiPaper Mini** (a non-Boox, MediaTek e-ink tablet), built as a dedicated `viwoods` product flavor so the Boox builds are completely unaffected.

- **Native hardware fast ink** — on the `viwoods` flavor (which targets SDK 30 so the app runs in the `untrusted_app_30` SELinux domain, like Viwoods' own apps), the planner calls `ENoteSetting.initWriting()` to hand pen rendering to the device's **T1000 timing controller** — instant, full-screen, pressure-sensitive strokes, the same engine the stock Viwoods apps use. No root required. This needs the factory-default property `persist.sys.focusmonitor.config=1`, present on stock units and only wiped by a bootloader-unlock + factory-reset (restore it with root via `resetprop -p persist.sys.focusmonitor.config 1`, or the Viwoods debug tool's "enable accessibility" command).
- **FAST-waveform software fallback** — if that property isn't set (or on the standard build), the app instead switches the panel to the FAST e-ink waveform and renders strokes itself, so they still refresh far quicker than the default GL16 reading waveform. The Onyx raw-drawing path is skipped on Viwoods (its `TouchHelper` loads but is inert and swallows the pen).
- Implemented in `ViwoodsFastInk.kt`; all device-specific behavior is gated so the `standard` (Boox) build is byte-for-byte the same as before. Credit to [jdkruzr's ViwoodsAppDev](https://github.com/jdkruzr/ViwoodsAppDev) reverse-engineering for the `initWriting` + targetSdk-30 recipe.

### Gratitude page
Swiping down from the day view lands on a dedicated gratitude / journal page before the regular blank notes:
- **3 Things I'm Grateful For** — left column, 11 lines for free-form elaboration on each numbered slot
- **The Best Thing Today** — right column, 11 lines
- **Doodle area** — full-width framed scratch space at the bottom, sized so the sketch becomes its own keepsake (see Ultrabridge Sketch notes below)

Swipe down again to reach your regular note pages 0, 1, 2…

### Home screen widgets
Three widget variants render today's planner page directly on the home screen, auto-refreshing every 30 minutes and immediately whenever you save strokes:

- **Ledger Full Page** — full planner page (schedule + tasks + notes)
- **Ledger Schedule** — schedule column split horizontally: morning (5am–1pm) on the left, afternoon (1pm–10pm) on the right
- **Ledger Tasks & Notes** — right column rearranged side-by-side: tasks on the left, notes on the right

All three include your handwritten strokes and overlay device calendar events. Tap any widget to open the app to today's page.

### Sync & export
- **Google Drive sync** — bidirectional auto-sync on page load and close
- **WebDAV backup** — PDF + raw JSON upload to Ultrabridge (or any WebDAV server)
- **JSON export** — day page stroke data syncs to `ToolsForBoox/json/` for downstream processing (OCR, task extraction)

### Polish
- **Atkinson Hyperlegible font** — optimized for e-ink readability
- **No ads, no Firebase** — stripped completely
- **Lighter alternating cells** — better contrast on e-ink displays

## Ultrabridge Integration

When paired with the [Ultrabridge Ledger Processor](https://github.com/mjhfunctionalashtanga/ultrabridge-ledger), your handwritten planner pages get:

1. **Full-page OCR** via Claude Sonnet — schedule, tasks, and notes transcribed
2. **Automatic task creation** — unchecked items in the Tasks section become CalDAV todos
3. **Due date parsing** — write "due 6/15" in a task and it becomes a real due date
4. **Sync everywhere** — tasks appear in Apple Reminders, tasks.org, Thunderbird, or any CalDAV client
5. **Note page OCR** — extra note pages (including the gratitude page) get their own freeform Sonnet pass and append as "Page N" sections to the daily Ledger note on mjh.yoga
6. **Sketch notes** — strokes inside the gratitude page's doodle area are rendered as a standalone PNG and posted as a separate `Sketch` tagged note
7. **Multi-device merge** — write on any Boox throughout the day (Tab8, Palma 2 Pro, Tab Mini C, etc.); the processor walks all device folders on WebDAV, dedupes strokes by ID, and consolidates everything into one Ledger note per date

## Build

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# Boox (and everything else) — targetSdk 36
./gradlew assembleDevStandardDebug

# Viwoods AiPaper Mini — targetSdk 30, native fast ink
./gradlew assembleDevViwoodsDebug

# Install on a connected Boox
$ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/devStandard/debug/toolboox-devStandard-debug-*.apk
```

> The Viwoods Mini ships a locked-down ADB daemon (no `install`/`shell`/`push`), so sideload the `devViwoods` APK over MTP (Android File Transfer → Download → tap to install on-device).

## Setup

1. Install the APK on your Boox tablet
2. Open the app — it goes straight to today's calendar page
3. To enable sync: tap the toolbar → Settings
   - **Google Drive**: sign in for bidirectional cloud sync
   - **WebCal Backup**: enter your WebDAV URL + credentials for PDF and JSON backup

## Support this fork

If Ledger is useful to you, here are a few ways to say thanks and help fund more digital-garden projects like this one:

- **Read the [Shala Daily](https://theyoga.club/)** — a free daily Ashtanga reflection from Michael Joel Hall
- **Visit [mjh.yoga](https://mjh.yoga)** — essays, notes, and ongoing projects from the same workshop this app came out of
- **Subscribe at [ashtanga.tech](https://ashtanga.tech/yoga-club/)** — the membership directly supports continued open development of tools like this one

## Credits

This is a GPLv3 fork of [Tools for Boox](https://github.com/gaborauth/toolsboox-android) by [Gabor Auth](https://github.com/gaborauth). Please support the original project too:

- [Original repo](https://github.com/gaborauth/toolsboox-android)
- [Google Play](https://play.google.com/store/apps/details?id=com.toolsboox)
- [Patreon](https://www.patreon.com/toolsboox)
- [PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=SVJ9HDCVKAAKS)

## License

GPLv3 — same as the original. See [LICENSE](../LICENSE).
