# MidiSheetMusic-Android — Codebase Guide for Copilot Agents

> **Purpose**: Give any Copilot agent an immediate, full understanding of the
> repository so that every new session starts informed rather than blind.
> Read this file before touching any code.

---

## 1. Application Overview

**MidiSheetMusic** is an Android app that:
1. Lets the user pick a MIDI (`.mid` / `.midi`) file from device storage or bundled assets.
2. Parses the file into tracks and notes.
3. Renders the notes as standard sheet music on screen.
4. Plays back the MIDI file using Android's `MediaPlayer`, highlighting the
   currently-playing notes on both the sheet and an on-screen piano keyboard.
5. Optionally receives live note input from a USB MIDI keyboard and highlights
   matching notes in real time.

The app is **landscape-first**, targets **API 30–35** (minSdk 30, targetSdk 35),
and is written entirely in **Java** with no Kotlin.

Build command (from repo root):
```
./gradlew assembleDebug
```

---

## 2. Project Structure

```
app/src/main/
  java/com/midisheetmusic/        ← main package
    sheets/                       ← sheet-music rendering sub-package
  res/
    layout/   ← XML layouts
    drawable/ ← icons & assets
    values/   ← strings, colors, styles, arrays
    xml/      ← fileprovider_paths.xml
  assets/     ← bundled .mid files + help.html
  AndroidManifest.xml
```

---

## 3. Class Catalogue (organised by concern)

### 3.1 Entry Points / Activities

| Class | File | Role |
|---|---|---|
| `SplashActivity` | `SplashActivity.java` | **Launcher activity.** Checks and requests `READ_EXTERNAL_STORAGE` (API ≤32) or `READ_MEDIA_AUDIO` (API 33+) permissions. Pre-loads clef/time-signature images. Immediately starts `ChooseSongActivity` and finishes itself. |
| `ChooseSongActivity` | `ChooseSongActivity.java` | **Song picker.** Hosts a `ViewPager2` with three tabs (All / Recent / Browse). Stores / retrieves the recently-opened file list in `SharedPreferences`. Contains the static `openFile(FileUri)` helper used by all three fragments to navigate to `SheetMusicActivity`. |
| `SheetMusicActivity` | `SheetMusicActivity.java` | **Player / main screen.** Extends `MidiHandlingActivity`. Loads the MIDI file, creates `MidiPlayer`, `Piano`, and `SheetMusic` views, wires them together. Owns a right-edge `DrawerLayout` for quick settings (scroll direction, colors, loop). Handles hardware keyboard shortcuts. Saves/restores per-song `MidiOptions` keyed by CRC-32 of the file bytes. |
| `SettingsActivity` | `SettingsActivity.java` | **Full settings screen.** `AppCompatActivity` wrapping an inner `SettingsFragment` (a `PreferenceFragmentCompat`). Receives a `MidiOptions` object via `Intent` extra and returns a modified copy via `setResult`. |
| `HelpActivity` | `HelpActivity.java` | Displays `assets/help.html` in a `WebView`. JavaScript disabled. |

### 3.2 Song Selection Fragments

| Class | File | Role |
|---|---|---|
| `ChooseSongPagerAdapter` | `ChooseSongPagerAdapter.java` | `FragmentStateAdapter` that creates the three tab fragments (positions 0→All, 1→Recent, 2→Browse). |
| `AllSongsFragment` | `AllSongsFragment.java` | Tab "All". Loads MIDI files from **bundled assets** and from **MediaStore** (internal + external). Sorts and de-duplicates the list. Provides a real-time text filter (`TextWatcher`). Uses a background `ExecutorService` for I/O. |
| `RecentSongsFragment` | `RecentSongsFragment.java` | Tab "Recent". Reads the JSON array persisted in SharedPreferences key `recentFiles` by `ChooseSongActivity.updateRecentFile()`. Refreshes on `onResume`. |
| `FileBrowserFragment` | `FileBrowserFragment.java` | Tab "Browse". Navigates the filesystem from `Environment.getExternalStorageDirectory()`. Lists sub-directories and `.mid`/`.midi` files. Remembers last-browsed directory in its own `SharedPreferences`. |
| `IconArrayAdapter<T>` | `IconArrayAdapter.java` | Custom `ArrayAdapter` that prepends a MIDI note icon or directory icon to each row. Used by all three fragment list views. |

### 3.3 MIDI I/O and USB MIDI

| Class | File | Role |
|---|---|---|
| `MidiHandlingActivity` | `MidiHandlingActivity.java` | Abstract base class for `SheetMusicActivity`. Extends `AbstractSingleMidiActivity` (from `jp.kshoji:midi-driver:0.1.5`). Bridges the USB MIDI driver callbacks (`onMidiNoteOn`, `onMidiInputDeviceAttached`, …) into the two abstract hooks `OnMidiDeviceStatus(boolean)` and `OnMidiNote(int note, boolean pressed)`. **Important**: because the base class extends `android.app.Activity` (not `AppCompatActivity`), `SheetMusicActivity` cannot use `registerForActivityResult` or be cast to `AppCompatActivity`. |

### 3.4 MIDI Data Model (Domain / Parsing)

These classes live in the root package and represent the parsed content of a MIDI file.

| Class | File | Role |
|---|---|---|
| `MidiFileException` | `MidiFileException.java` | `RuntimeException` sub-class thrown when MIDI parsing fails. Includes the byte offset. |
| `MidiFileReader` | `MidiFileReader.java` | Low-level binary reader for a `byte[]`. Provides `ReadByte()`, `ReadShort()`, `ReadInt()`, `ReadVarlen()`, `ReadAscii()`, `Skip()`, `Peek()`. Used exclusively by `MidiFile.parse()`. |
| `MidiEvent` | `MidiEvent.java` | A single raw MIDI event with all possible fields: `DeltaTime`, `StartTime`, `EventFlag`, `Channel`, `Notenumber`, `Velocity`, `Instrument`, `Tempo`, `Numerator`/`Denominator` (time sig), `Metaevent`, raw `Value` bytes, etc. Implements `Comparator<MidiEvent>` by start time then event flag. |
| `MidiNote` | `MidiNote.java` | A parsed note: `starttime`, `channel`, `notenumber` (0–127, middle C = 60), `duration` (all in pulses). Created on NoteOn; duration set by `NoteOff()`. Implements `Comparator<MidiNote>` by start time. |
| `MidiTrack` | `MidiTrack.java` | One track extracted from the MIDI file. Holds an `ArrayList<MidiNote>`, the `instrument` number (0–128, 128 = Percussion), optional `ArrayList<MidiEvent> lyrics`. Constructed from a raw `ArrayList<MidiEvent>`. |
| `MidiFile` | `MidiFile.java` | **Central domain class.** Parses a `byte[]` into tracks + time/key metadata. Contains MIDI format constants (`EventNoteOn`, `MetaEventTempo`, …), the 128-entry `Instruments[]` name array, and static track-manipulation utilities. Key methods: `parse()`, `ReadTrack()`, `ChangeMidiNotes(MidiOptions)` (returns modified tracks for rendering), `ChangeSound()` / `Write()` (writes a modified MIDI file to disk for playback), `SplitTrack()`, `CombineToTwoTracks()`, `RoundStartTimes()`, `RoundDurations()`, `Transpose()`. |
| `TimeSignature` | `TimeSignature.java` | Holds numerator, denominator, pulses-per-quarter-note, tempo (µs/quarter). Methods: `GetNoteDuration(int pulses)` → `NoteDuration` enum, `DurationToTime(NoteDuration)`, `GetMeasure(int pulseTime)`, `getMeasure()` (pulses per measure). Implements `Serializable`. |
| `NoteDuration` | `NoteDuration.java` | Enum: `ThirtySecond`, `Sixteenth`, `Triplet`, `Eighth`, `DottedEighth`, `Quarter`, `DottedQuarter`, `Half`, `DottedHalf`, `Whole`. |
| `NoteScale` | `NoteScale.java` | Constants for chromatic scale (A=0 … Aflat=11). Static helpers `ToNumber(notescale, octave)` and `FromNumber(midiNumber)`, `IsBlackKey(notescale)`. |
| `FileUri` | `FileUri.java` | Reference to a MIDI file (assets, content://, file://). Stores a `Uri` and a human-readable `displayName`. Key methods: `getData(Context)` → `byte[]`, `toJson()` / `fromJson()` for persistence, `isDirectory()`, `equalJson()`. Implements `Comparator<FileUri>` for alphabetic sorting. |
| `ListInt` | `ListInt.java` | Lightweight `int[]`-backed list (avoids `ArrayList<Integer>` boxing). Used in key-signature guessing. |
| `NoteData` | `NoteData.java` | Plain data struct used by `ChordSymbol`: MIDI note number, `WhiteNote` position, `NoteDuration`, `leftside` flag (note placement relative to stem), `Accid`. |
| `PairInt` | `MidiFile.java` (inner) | Simple struct with `low` and `high` int fields. Used internally during track splitting. |

### 3.5 Sheet Music Rendering (sheets sub-package)

These classes turn the parsed `MidiTrack`/`MidiNote` data into drawable symbols laid out on a staff. All `MusicSymbol` implementations carry a start time and know how to draw themselves onto a `Canvas`.

#### Interfaces / Enums

| Class | Role |
|---|---|
| `MusicSymbol` *(interface)* | Contract for every drawable symbol: `getStartTime()`, `getMinWidth()`, `getWidth()` / `setWidth()`, `getAboveStaff()`, `getBelowStaff()`, `Draw(Canvas, Paint, int ytop)`. |
| `Clef` *(enum)* | `Treble`, `Bass`. |
| `Accid` *(enum)* | `Sharp`, `Flat`, `Natural`. |

#### Symbols (implement MusicSymbol)

| Class | Role |
|---|---|
| `ClefSymbol` | Draws the treble or bass clef bitmap. Loads bitmaps via `LoadImages(Context)` (called at startup). |
| `AccidSymbol` | Draws a sharp, flat, or natural sign next to a note. `DrawSharp()`, `DrawFlat()`, `DrawNatural()`. |
| `ChordSymbol` | Draws one or more simultaneous notes as a chord (note heads, accidentals, stems, ledger lines, note letters). Contains `NoteData[]` and up to two `Stem` objects. Has `getNotedata()` used by `MidiPlayer.OnMidiNote()` to match a live MIDI note to what is currently highlighted. |
| `RestSymbol` | Draws a rest (whole, half, quarter, eighth). `DrawWhole()`, `DrawHalf()`, `DrawQuarter()`, `DrawEighth()`. |
| `BarSymbol` | Draws the vertical bar line at a measure boundary. |
| `BlankSymbol` | Zero-height invisible placeholder used to horizontally align symbols across tracks. |
| `TimeSigSymbol` | Draws the time signature (numerator/denominator) using pre-loaded digit bitmaps. |
| `LyricSymbol` | Stores a lyric text and its x-position within a staff. Not a `MusicSymbol` but rendered by `Staff`. |

#### Layout / Support

| Class | Role |
|---|---|
| `Staff` | One horizontal row of music (one track, one screen-width). Holds `ArrayList<MusicSymbol>` plus lyrics. Draws itself including the 5-line staff, clef, key signature, measure numbers, optional track label, and optional loop-region tint. Computed by `SheetMusic.CreateStaffs()`. |
| `Stem` | Draws the stem of a chord; handles up/down direction, horizontal beam lines between adjacent chords (pair stems). Direction can be changed post-creation by `SheetMusic`. |
| `KeySignature` | Tracks the current key (sharps/flats). `GetAccidental(noteNumber, measure)` decides whether a note needs a sharp/flat/natural symbol in context. `Guess(ListInt notes)` infers the key from a set of note numbers. `GetWhiteNote(noteNumber)` maps a MIDI number to its `WhiteNote` position on the staff. |
| `WhiteNote` | A note on the white keys (letter A–G, octave 0–10). Comparison, distance, and bound helpers (e.g., `WhiteNote.Top(clef)`, `WhiteNote.Bottom(clef)`). |
| `ClefMeasures` | For each measure of a track, determines whether the clef should be Treble or Bass. Used when rendering two-staff layouts. |
| `SymbolWidths` | Used by `SheetMusic.AlignSymbols()` to compute how much extra width each symbol needs so that notes at the same pulse time line up vertically across all tracks. |
| `DictInt` | Sorted int→int map backed by a plain `int[]`. High-performance alternative to `HashMap<Integer,Integer>`. Used by `SymbolWidths`. |

### 3.6 Sheet Music View

| Class | File | Role |
|---|---|---|
| `SheetMusic` | `SheetMusic.java` | **Core rendering view.** Extends `SurfaceView`, implements `SurfaceHolder.Callback` and `ScrollAnimationListener`. Converts `MidiFile` + `MidiOptions` into an array of `Staff` objects and draws them to a double-buffered `Bitmap`. Key methods: `init(MidiFile, MidiOptions)` — full rebuild; `ShadeNotes(currentPulse, prevPulse, scrollType)` — highlights currently playing notes and scrolls; `PulseTimeForPoint(Point)` — hit-test for tap-to-seek; `getCurrentNote(int pulse)` / `getPrevNote(int pulse)` — note navigation; `DrawPage(Canvas, pageNumber)` — used for saving images; `invalBuffer()` — forces redraw. The three scroll constants `ImmediateScroll`, `GradualScroll`, `DontScroll` control animation. |

### 3.7 Piano View

| Class | File | Role |
|---|---|---|
| `Piano` | `Piano.java` | **On-screen piano keyboard view.** Extends `SurfaceView`. Draws 6 octaves of white + black keys. `SetMidiFile(MidiFile, MidiOptions, MidiPlayer)` loads the note list. `ShadeNotes(currentPulse, prevPulse)` highlights the keys being played; uses two colors to distinguish right-hand (shade1) vs left-hand (shade2). `ShadeOneNote(noteNumber, color)` is used by the USB MIDI path. Touch on a key calls `MidiPlayer.MoveToClicked()`. |

### 3.8 Playback Engine

| Class | File | Role |
|---|---|---|
| `MidiPlayer` | `MidiPlayer.java` | **Playback controller widget** (`LinearLayout`). Owns play/pause, rewind, fast-forward, back, reset buttons plus a speed `SeekBar`. Internal states: `stopped`, `playing`, `paused`, `initStop`, `initPause`, `midi`. Key responsibilities: `CreateMidiFile()` — calls `MidiFile.ChangeSound()` to write `playing.mid` to the cache dir; `PlaySound(filename)` — uses Android `MediaPlayer`; `DoPlay()` — background `Thread` that advances `currentPulseTime`, shades notes in sheet and piano, handles loop measures, count-in beats; `Rewind()`, `FastForward()`, `PlayPause()`, `NextNote()`, `PrevNote()`, `SetLoopStart()`, `SetLoopEnd()`, `ToggleLoop()`, `SpeedUp()`, `SpeedDown()`, `MoveToClicked(x, y)`. |

### 3.9 Options / State

| Class | File | Role |
|---|---|---|
| `MidiOptions` | `MidiOptions.java` | **All user-configurable settings for a song.** Implements `Serializable`. Fields include: `tracks[]` (which tracks visible), `mute[]`, `volume[]` (per-track), `instruments[]`, `trackOctaveShift[]`, `scrollVert`, `largeNoteSize`, `twoStaffs`, `showNoteLetters` (None / Letter / FixedDoReMi / MovableDoReMi / FixedNumber / MovableNumber), `showLyrics`, `showMeasures`, `shifttime`, `transpose`, `key`, `time`, `combineInterval`, `shade1Color`, `shade2Color`, `noteColors[]` (per chromatic note), `useColors`, `colorAccidentals`, `useFullHeight`, `countInMeasures`, `playMeasuresInLoop`, `playMeasuresInLoopStart/End`, `tempo`, `pauseTime`, `midiShift`, `showTrackLabels`. Methods: `toJson()` / `fromJson()` for per-song persistence; `merge(saved)` to apply persisted settings without overwriting array-size-sensitive fields; `copy()` for deep clone. |

### 3.10 Settings UI Widgets

| Class | File | Role |
|---|---|---|
| `TrackRowPreference` | `TrackRowPreference.java` | Custom `Preference` that renders one row per MIDI track in `SettingsActivity`. Shows visibility toggle, mute toggle, instrument picker, octave-shift button (8va / 8vb / none), and volume `SeekBar`. |
| `ColorPreference` | `ColorPreference.java` | Custom `Preference` showing a colored square. Clicking opens a `ColorDialog`. Implements `ColorChangedListener`. |
| `ColorDialog` | `ColorDialog.java` | `Dialog` containing a `ColorView`. `ColorView` is a circular color wheel drawn with `Canvas`. Touch picks a color; calls back via `ColorChangedListener`. |
| `ColorChangedListener` | `ColorChangedListener.java` | One-method interface: `colorChanged(int color)`. |

### 3.11 Animation / Scrolling

| Class | File | Role |
|---|---|---|
| `ScrollAnimation` | `ScrollAnimation.java` | Handles touch events for dragging and fling-scrolling the `SheetMusic` view. Calls `ScrollAnimationListener.scrollUpdate(dx, dy)` during moves and flings. Calls `scrollTapped(x, y)` for short taps. Uses a `Handler` timer for exponentially-decaying fling animation. |
| `ScrollAnimationListener` | `ScrollAnimationListener.java` | Interface: `scrollUpdate(int dx, int dy)`, `scrollTapped(int x, int y)`. Implemented by `SheetMusic`. |
| `SheetUpdateRequestListener` | `SheetUpdateRequestListener.java` | Interface: `onSheetUpdateRequest()`. Allows `MidiPlayer` to tell `SheetMusicActivity` to rebuild the sheet (e.g., after an option change during settings). |

### 3.12 Misc / Utilities

| Class | File | Role |
|---|---|---|
| `TimeSigSymbol` | `TimeSigSymbol.java` | Implements `MusicSymbol`. Draws the time signature (e.g., 4 over 4) using digit bitmaps loaded by `LoadImages(Context)`. |
| `BoxedInt` | `SheetMusic.java` (inner) | Simple mutable int holder used as an output parameter in `SheetMusic` helper methods. |

---

## 4. Data Flow / Feature Walkthrough

### 4.1 Opening a Song

```
SplashActivity
  → ChooseSongActivity (tab: AllSongsFragment | RecentSongsFragment | FileBrowserFragment)
    → user taps file
    → ChooseSongActivity.openFile(FileUri)
      → FileUri.getData(Context) → byte[]
      → MidiFile.hasMidiHeader(data) validation
      → ChooseSongActivity.updateRecentFile(...)   [SharedPreferences]
      → Intent ACTION_VIEW → SheetMusicActivity
```

### 4.2 Parsing a MIDI File

```
SheetMusicActivity.onCreate()
  → new MidiFile(byte[], filename)
    → MidiFile.parse()
      → MidiFileReader reads header: trackmode, quarternote, numTracks
      → for each track: MidiFile.ReadTrack() → ArrayList<MidiEvent>
        → ReadMetaEvent() for 0xFF events (tempo, time sig, key sig, lyrics)
      → new MidiTrack(events, tracknum)  [per track]
        → extracts MidiNote objects from NoteOn/NoteOff pairs
      → SplitTrack() / HasMultipleChannels() to split channel-per-track if needed
      → TimeSignature created from MetaEventTimeSignature / MetaEventTempo
```

### 4.3 Rendering Sheet Music

```
SheetMusicActivity.createSheetMusic(MidiOptions)
  → new SheetMusic(context)
  → sheet.init(midifile, options)
    → MidiFile.ChangeMidiNotes(options)     [returns modified MidiTrack list]
        applies: SplitTrack, CombineToTwoTracks, ShiftTime, Transpose,
                 RoundStartTimes, RoundDurations
    → KeySignature.Guess(allNotes)          [or use options.key]
    → for each track:
        ClefMeasures.GetClef(starttime)     [Treble vs Bass per measure]
        CreateChords() → ArrayList<ChordSymbol>
        AddBars() → inserts BarSymbols
        AddRests() → inserts RestSymbols
    → AlignSymbols() using SymbolWidths     [vertical alignment across tracks]
    → CreateStaffs() → ArrayList<Staff>     [breaks symbol list into screen-width rows]
    → AddLyrics()
    → calculateSize() → sheetwidth, sheetheight
```

### 4.4 Playback

```
MidiPlayer.PlayPause()
  → CreateMidiFile()
      MidiFile.ChangeSound(destfile, options)  [writes modified .mid to cache]
  → PlaySound("playing.mid")
      new MediaPlayer(); prepareAsync(); setOnPreparedListener → start()
  → DoPlay() thread:
      while playing:
        advance currentPulseTime proportional to elapsed time × tempo × speed%
        sheet.ShadeNotes(currentPulse, prevPulse, scrollType)
        piano.ShadeNotes(currentPulse, prevPulse)
        handle loop / end-of-song
```

### 4.5 Settings Round-Trip

```
SheetMusicActivity.changeSettings()
  → Intent with MidiOptions → startActivityForResult → SettingsActivity
      SettingsFragment shows preferences; user changes values
      back press → updateOptions() → setResult(RESULT_OK, intent with modified MidiOptions)
  → SheetMusicActivity.onActivityResult()
      options.merge(newOptions)
      createSheetMusic(options)   [full re-render]
      saveOptions()               [persist per-song JSON to SharedPreferences]
```

### 4.6 Live USB MIDI Input

```
USB MIDI device attached
  → MidiHandlingActivity.onMidiInputDeviceAttached()
      → SheetMusicActivity.OnMidiDeviceStatus(true)   [UI feedback]
  → user presses key on hardware keyboard
  → MidiHandlingActivity.onMidiNoteOn()
      → SheetMusicActivity.OnMidiNote(note, true)
          → MidiPlayer.OnMidiNote(note, true)
              checks note against sheet.getCurrentNote(currentPulse)
              calls ShadeNotes / piano.ShadeNotes
```

---

## 5. Key Technical Constraints & Gotchas

| Constraint | Detail |
|---|---|
| **`SheetMusicActivity` is NOT `AppCompatActivity`** | It extends `MidiHandlingActivity` → `AbstractSingleMidiActivity` → `android.app.Activity`. Cannot use `registerForActivityResult`, `getOnBackPressedDispatcher` via AppCompat, or be cast to `AppCompatActivity`. Uses deprecated `startActivityForResult` / `onActivityResult`. |
| **Edge-to-edge (API 35)** | targetSdk 35 enforces edge-to-edge. `SheetMusicActivity` calls `hideSystemUI()` (immersive fullscreen). `ChooseSongActivity` and `HelpActivity` layouts use `android:fitsSystemWindows="true"`. |
| **Temp MIDI file for playback** | `MidiPlayer` always writes a modified copy of the MIDI file to `"playing.mid"` in the cache dir before starting `MediaPlayer`. This is how playback speed, transpose, mute, etc. are applied. |
| **Pulse-based timing** | All time values in the domain layer are in MIDI "pulses". `TimeSignature.getMeasure()` gives pulses per measure. `TimeSignature.GetNoteDuration(pulses)` maps a duration in pulses to a `NoteDuration` enum. |
| **Per-song settings persistence** | `SheetMusicActivity` saves `MidiOptions` as a JSON string in `SharedPreferences` using the CRC-32 of the raw MIDI bytes as the key. This means each song has its own saved settings. |
| **Recent files persistence** | Stored as a JSON array under key `"recentFiles"` in the `"midisheetmusic.recentFiles"` SharedPreferences file. Maximum 10 entries. |
| **Asset MIDI files** | Bundled as `.mid` files directly inside `app/src/main/assets/`. Accessed via `file:///android_asset/` URIs. |
| **FileProvider** | Configured in `AndroidManifest.xml` with authority `${applicationId}.fileprovider`. Paths defined in `res/xml/fileprovider_paths.xml`. Used to share the temp playback file. |
| **Library: jp.kshoji:midi-driver:0.1.5** | From a custom Maven repo at the JoyTunes GitHub. Provides USB MIDI host support. The AAR's base activity (`AbstractSingleMidiActivity`) extends plain `Activity`, not `AppCompatActivity`. |
| **No Kotlin, no Jetpack Compose, no Room** | The codebase is pure Java with traditional Views/Fragments/Preferences. |

---

## 6. Layout & Resource Files

| Layout | Used By | Description |
|---|---|---|
| `activity_choose_song.xml` | `ChooseSongActivity` | Root layout with `TabLayout` + `ViewPager2`. Needs `fitsSystemWindows=true` for edge-to-edge. |
| `activity_list.xml` | `RecentSongsFragment` | Simple `ListView` for the recent files tab. |
| `choose_song.xml` | `AllSongsFragment` | `EditText` filter bar + `ListView`. |
| `choose_song_item.xml` | `IconArrayAdapter` | Single row: icon + text. |
| `file_browser.xml` | `FileBrowserFragment` | Directory path `TextView` + "home" button + `ListView`. |
| `sheet_music_layout.xml` | `SheetMusicActivity` | `DrawerLayout` with the main content (`MidiPlayer` + `Piano` + `SheetMusic`) and a right-side settings drawer. |
| `player_toolbar.xml` | `MidiPlayer` | Transport controls: back, rewind, reset, play/pause, fast-forward, speed seekbar, MIDI button, left/right hand toggles, piano toggle, settings. |
| `settings_drawer.xml` | Inflated into `DrawerLayout` in `SheetMusicActivity` | Quick-access switches: scroll direction, colors, color accidentals, loop enable/configure, show measures. |
| `settings_activity.xml` | `SettingsActivity` | Toolbar + fragment container for `SettingsFragment`. |
| `help.xml` | `HelpActivity` | `WebView` with `fitsSystemWindows=true`. |
| `color_preference.xml` | `ColorPreference` | Colored square view inside a preference row. |
| `preference_track_row.xml` | `TrackRowPreference` | Track visibility + mute buttons + instrument spinner + octave-shift button + volume seekbar. |
| `save_images_dialog.xml` | `SheetMusicActivity` | Dialog for entering filename when saving sheet music as images. |

---

## 7. Permissions & Manifest

| Permission | Condition | Purpose |
|---|---|---|
| `READ_EXTERNAL_STORAGE` | maxSdkVersion 32 | Read MIDI files on Android 11–12 |
| `READ_MEDIA_AUDIO` | API 33+ | Read audio/MIDI on Android 13+ |
| `android.hardware.usb.host` | optional feature | USB MIDI keyboard support |

`SheetMusicActivity` is also exported with intent filters for `audio/midi` and `audio/x-midi` MIME types, so the app can be opened directly from a file manager.

---

## 8. Quick Bug-Location Guide

| Symptom | Likely Classes |
|---|---|
| MIDI file not parsing / wrong notes | `MidiFile`, `MidiFileReader`, `MidiTrack`, `MidiEvent`, `MidiNote` |
| Sheet music layout wrong (note positions, spacing) | `SheetMusic`, `Staff`, `ChordSymbol`, `SymbolWidths`, `KeySignature`, `WhiteNote` |
| Wrong accidentals / key signature | `KeySignature`, `AccidSymbol`, `ChordSymbol.NoteName()` |
| Clef wrong (treble shown for bass notes) | `ClefMeasures`, `SheetMusic` (two-staff logic), `MidiOptions.twoStaffs` |
| Rest not shown / wrong duration | `RestSymbol`, `SheetMusic.AddRests()`, `TimeSignature.GetNoteDuration()` |
| Bar lines at wrong position | `BarSymbol`, `SheetMusic.AddBars()`, `TimeSignature.getMeasure()` |
| Stems / beams wrong direction | `Stem`, `ChordSymbol` (stem direction logic) |
| Lyrics not shown | `LyricSymbol`, `MidiTrack.getLyrics()`, `SheetMusic.AddLyrics()`, `Staff` |
| Playback out of sync / wrong tempo | `MidiPlayer` (DoPlay thread), `TimeSignature.getTempo()`, `MidiOptions.tempo` |
| Playback doesn't start / MediaPlayer error | `MidiPlayer.CreateMidiFile()`, `MidiFile.ChangeSound()` / `Write()` |
| Loop not working | `MidiPlayer.SetLoopStart/End/ToggleLoop`, `MidiOptions.playMeasuresInLoop*` |
| Speed control wrong | `MidiPlayer.SpeedUp/Down()`, speed % calculation in `DoPlay` |
| Note not highlighted during playback | `SheetMusic.ShadeNotes()`, `Piano.ShadeNotes()`, pulse-time math in `DoPlay` |
| Piano keyboard rendering wrong | `Piano`, `NoteScale.IsBlackKey()` |
| USB MIDI not connecting | `MidiHandlingActivity`, `jp.kshoji` driver callbacks |
| Settings not saving / loading | `MidiOptions.toJson/fromJson/merge()`, `SheetMusicActivity.saveOptions()` |
| Recent files not persisting | `ChooseSongActivity.updateRecentFile()`, `RecentSongsFragment.loadFileList()` |
| File browser shows wrong files | `FileBrowserFragment`, `FileUri.isDirectory()` |
| Permission denied / crash on open | `SplashActivity.requiredPermissions()`, `AllSongsFragment.loadMidiFilesFromProvider()` |
| Colors / color picker broken | `ColorDialog`, `ColorView`, `ColorPreference`, `MidiOptions.noteColors` / `shade1Color` |
| Track options not applied | `SettingsActivity.SettingsFragment`, `TrackRowPreference`, `MidiOptions.tracks[]` / `mute[]` / `instruments[]` |
| Transpose / key signature change ignored | `MidiFile.Transpose()`, `MidiOptions.transpose` / `key`, `KeySignature` |
| Scroll / swipe not working | `ScrollAnimation`, `SheetMusic.onTouchEvent()` |
| App crashes on orientation change | `SheetMusicActivity.onConfigurationChanged()` (handles it manually, does not recreate) |
| Images not saving to gallery | `SheetMusicActivity.saveAsImages()`, `saveBitmapToFile()` |
