# Changelog

All notable changes to this fork of MidiSheetMusic are documented here.
This project inherits from the original [MichaelBell/MidiSheetMusic-Android](https://github.com/MichaelBell/MidiSheetMusic-Android)
and ultimately from [Madhav Vaidyanathan's MidiSheetMusic](https://sourceforge.net/projects/midisheetmusic/).

---

## [Unreleased / Current fork changes]

### Android Modernization
- Upgraded `compileSdk` / `targetSdk` to **35** (Android 15) and `minSdk` to **30**.
- Replaced all legacy/removed Gradle APIs with their current equivalents; eliminated all Java compiler warnings.
- Fixed dark-theme rendering of the Settings screen (controls were invisible on Android 12+).
- Fixed the `SettingsActivity` action bar rendering behind the status bar.
- Fixed a `SurfaceView` surface disconnect crash that occurred when the sheet music was recreated mid-session.
- Edge-to-edge support: root layouts use `fitsSystemWindows` to avoid content hiding behind the status bar on Android 15.

### Portrait Mode Support
- Removed the forced landscape lock; the app now works correctly in **both portrait and landscape**.
- The sheet music, piano, and toolbar all redraw and resize correctly on orientation change.
- Fixed the speed `SeekBar` resizing and the toolbar not fitting in portrait.

### Compact Track Options UI
- Merged the previously scattered track preferences into a **single compact row per track**: visibility, mute, instrument, octave shift, and volume are all inline on the same row.
- This makes configuring multi-track MIDI files much faster.
- Aligned all preference items consistently by removing stray icon-reservation padding.

### Per-track Octave Shift (8va / 8vb)
- Each track can be shifted **one octave up (8va)** or **one octave down (8vb)** independently.
- This changes how the notes are drawn on the staff without altering the playback pitch, useful for simplifying a difficult range for practice.

### Per-track Volume Sliders
- A **volume slider** is now available for every track in the track options row.
- Volume is applied as MIDI velocity scaling in the playback file, so backing tracks can be quieted without muting them entirely.
- The **hide** and **mute** functions are now clearly separated: the clef-filter buttons hide a track from view; the mute button silences it from playback. Previously these were conflated.

### Count-in Measures
- Replaced the old fixed "delay before start" option with a configurable **count-in** (0, 1, 2, or 4 measures).
- An **audible click on every beat** is played during the count-in using `ToneGenerator`, giving you the rhythm before the piece begins.
- Fixed a cursor-offset bug when resuming from start after a Rewind.
- Fixed the count-in setting sometimes displaying a stale value when switching songs.

### BPM Display and Toolbar Icons
- The speed label now shows both **the percentage and the resulting tempo in beats per minute (BPM)**, updating live as the slider moves.
- The generic "L" / "R" hand filter labels have been replaced with proper **bass-clef** and **treble-clef** icons (SVG vectors).
- Consistent toolbar height in both landscape and portrait.

### Keyboard Shortcuts
- A full set of **keyboard shortcuts** works on the playback screen with any connected keyboard (USB, Bluetooth, etc.):

  | Key | Action |
  |---|---|
  | Space | Play / Pause |
  | Left Arrow | Previous note |
  | Right Arrow | Next note |
  | Page Up | Rewind (back one measure) |
  | Page Down | Fast Forward (forward one measure) |
  | R | Reset to start |
  | + or = | Increase playback speed |
  | - | Decrease playback speed |
  | S | Set loop start at current position |
  | E | Set loop end at current position |
  | L | Toggle loop on / off |

- Fixed the first arrow-key press after tapping the sheet music being swallowed by the `SurfaceView` focus steal.

### Track Labels
- Added an optional **track labels** display: the track number and a short instrument abbreviation (e.g. "T1 Pno") appear above each staff.
- Labels use abbreviations to keep them compact and avoid overlap.
- Labels are suppressed when all tracks are merged into two staves.
- Labels update instantly when the instrument is changed in settings.

### Loop Region Improvements
- The active loop section is highlighted with a **semi-transparent red overlay** on the sheet music.
- Loop restart is now **zero-delay** — playback jumps immediately to the loop start measure.
- Fixed the loop tint being erased when notes were un-shaded.
- Fixed the loop tint height exceeding the staff bar bounds.
- Fixed loop calculation and measure-boundary snapping for **custom time signatures** (non-4/4).
- Fixed S/E/L keyboard shortcuts not restoring the loop highlight correctly after `invalBuffer`.

### Playback & MIDI Engine Fixes
- **Rebuilt playback MIDI tracks from scratch** using a whitelist approach: only the events that are needed are included in the temporary MIDI file used for `MediaPlayer`. This fixed numerous note-highlighting and timing issues when starting playback from the beginning after previously seeking.
- Fixed MIDI **tempo calculation** for non-standard tempos (tempos stored as µs-per-quarter-note were not always applied correctly).
- Fixed instrument **restoration after seeking** to a mid-song position (the General MIDI program change was sometimes lost).
- Fixed note highlighting for various edge cases during playback.
- Fixed measure navigation when notes don't align exactly to measure boundaries.

### Song Navigation & Selection
- Fixed navigation between measures when notes don't align exactly to the measure start pulse.
- The `FileBrowserFragment` now navigates from both `ListView` rows and taps on sub-directories.
- Android manifest intent filters corrected; added **SMF MIME type** (`audio/midi`, `audio/x-midi`) support so the app can be opened directly from any file manager.
- **Percussion tracks are no longer hidden by default** — they show up in the track list and on the sheet.

### Settings — Instrument Selection
- Track instrument rows in the settings screen are now **tappable** to open the instrument picker, removing the need for a separate dedicated "change instrument" button.

### Automated CI / CD
- The GitHub Actions workflow now builds a **signed release APK** on every push to `master`.
- A versioned **GitHub Release** is created automatically with the APK attached (using the `versionName` from `build.gradle` as the tag; the tag is only created once and is immutable).
- The **fastlane changelog** (`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`) is generated automatically from `git log` commits since the previous tag, and the same content is used as the GitHub release notes — keeping both sources in sync.

### Developer Experience
- Added a comprehensive `.github/copilot-instructions.md` codebase guide for AI coding agents (Copilot, etc.), covering class catalogue, data flow, and common bug-location tips.

---

## Prior History

This fork is based on [MichaelBell/MidiSheetMusic-Android](https://github.com/MichaelBell/MidiSheetMusic-Android), which added USB MIDI keyboard support, and on [Madhav Vaidyanathan's original](https://sourceforge.net/projects/midisheetmusic/).
