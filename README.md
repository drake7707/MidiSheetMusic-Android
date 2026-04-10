# Extended MidiSheetMusic

[![Build](https://github.com/drake7707/MidiSheetMusic-Android/actions/workflows/build.yml/badge.svg)](https://github.com/drake7707/MidiSheetMusic-Android/actions/workflows/build.yml)

MidiSheetMusic is a free and open-source Android app that helps you learn and practice piano pieces. Load any MIDI file, and the app will play the music while simultaneously highlighting the notes on both the sheet music and an on-screen piano keyboard. Settings are saved per song, so you can pick up exactly where you left off.

<div style="text-align: center">

<img width="512" height="1080" alt="image" src="https://github.com/user-attachments/assets/5f4ac9d9-677f-44ef-9fec-205f7e15b557" />
<br>
<img width="512" height="1080" alt="image" src="https://github.com/user-attachments/assets/4abc4cae-026e-4d08-9eb1-269993f8e15e" />
<br>
<img width="512" height="1080" alt="image" src="https://github.com/user-attachments/assets/8617fece-7074-4226-b112-e09f575215eb" />
<br>
<img width="512" height="1080" alt="image" src="https://github.com/user-attachments/assets/c3b13767-cc7c-4062-9aa3-5044a4954097" />
<br>
<img width="512" height="1080" alt="image" src="https://github.com/user-attachments/assets/3aac4036-c3fe-417d-a2a1-dbe305281273" />



</div>

---

## Table of Contents

1. [What is a MIDI File?](#what-is-a-midi-file)
2. [Getting Started](#getting-started)
3. [Choosing a Song](#choosing-a-song)
4. [The Playback Screen](#the-playback-screen)
5. [Playback Controls](#playback-controls)
6. [Keyboard Shortcuts](#keyboard-shortcuts)
7. [Song Settings](#song-settings)
   - [Track Options](#track-options)
   - [Display Options](#display-options)
   - [Notation Options](#notation-options)
   - [Color Options](#color-options)
   - [Loop Options](#loop-options)
   - [Count-in Measures](#count-in-measures)
8. [Saving Sheet Music as Images](#saving-sheet-music-as-images)
9. [External MIDI Keyboard Support](#external-midi-keyboard-support)
10. [Bundled Songs](#bundled-songs)
11. [License](#license)
12. [Contributors](#contributors)
13. [Changelog](CHANGELOG.md)

---

## What is a MIDI File?

A MIDI file (file extension `.mid` or `.midi`) is a type of music file used widely with electronic instruments. Unlike an audio file (such as MP3 or WAV), a MIDI file does not record actual sound. Instead, it stores the notes, timing, tempo, and instrument information needed to reproduce a musical performance. This makes MIDI files ideal for displaying sheet music and for learning to play a piece note by note.

MidiSheetMusic comes with over 60 sample classical piano pieces. You can also download additional MIDI files from the internet and open them in the app.

---

## Getting Started

When you first open MidiSheetMusic, you will see the song chooser screen. You can browse the built-in sample songs, open a MIDI file from your device storage, or pick up a recently opened song. Tap any song in the list to open it.

---

## Choosing a Song

The song chooser has three tabs:

- **All** - Lists all the classical piano pieces included with the app.
- **Recent** - Shows the last songs you opened, for quick access.
- **Browse** - Lets you navigate your device's storage to find and open your own MIDI files.

To open a MIDI file from outside the app, you can also use any file manager app on your device and tap the `.mid` file. MidiSheetMusic will open it directly.

---

## The Playback Screen

Once a song is open, you will see:

- A **toolbar** at the top with playback controls and a speed slider.
- A **piano keyboard** in the middle, where keys light up as the music plays.
- The **sheet music** below (or filling the screen if the piano is hidden), which scrolls in time with the music and highlights the current notes.

To return to the song list at any time, tap the back button on your device.

---

## Playback Controls

The toolbar across the top of the playback screen contains the following controls, from left to right:

| Control | Description |
|---|---|
| Rewind | Moves back one measure. |
| Stop / Reset | Stops playback and returns to the beginning of the song. |
| Play / Pause | Starts or pauses playback. Tap anywhere on the sheet music to pause as well. |
| Fast Forward | Moves forward one measure. |
| Previous Note | Steps back to the previous note. Useful for slow, careful practice. |
| Next Note | Steps forward to the next note. |
| Speed Slider | Adjusts playback speed from 1% to 100% of the original tempo. |
| Speed Display | Shows the current speed as a percentage and the resulting tempo in beats per minute (BPM). The BPM updates live as you move the slider. |
| Bass Clef button | Filters the view to show or highlight the left-hand (bass clef) staff. |
| Treble Clef button | Filters the view to show or highlight the right-hand (treble clef) staff. |
| Show/Hide Piano | Toggles the on-screen piano keyboard. Hiding it gives more space to the sheet music. |
| Settings | Opens the song settings panel. |

When the music is playing, tapping anywhere on the sheet music or piano will pause playback.

---

## Keyboard Shortcuts

If you use a physical keyboard connected to your Android device (for example, via USB or Bluetooth), or if your device has hardware keys, the following shortcuts work on the playback screen:

| Key | Action |
|---|---|
| Space | Play / Pause |
| Left Arrow | Previous note |
| Right Arrow | Next note |
| Page Up | Rewind (back one measure) |
| Page Down | Fast Forward (forward one measure) |
| R | Reset (return to start) |
| + or = | Increase playback speed |
| - | Decrease playback speed |
| S | Set loop start at the current position |
| E | Set loop end at the current position |
| L | Toggle loop on / off |

These shortcuts also work reliably after tapping the sheet music, which would normally steal keyboard focus.

---

## Song Settings

Tap the settings icon in the toolbar to open the song settings for the current piece. All settings are saved individually for each song, so changing settings for one song does not affect others.

### Track Options

MIDI files are divided into tracks, where each track typically represents a separate instrument or hand part. The Track Options section shows one row for each track in the file. Each row includes:

| Control | Description |
|---|---|
| Track name and current instrument | Shown as a label on the row. |
| Visibility toggle | Show or hide the track in the sheet music. Hidden tracks are not displayed but may still play. |
| Mute toggle | Mute or unmute the track during playback. A muted track is still shown in the sheet music but plays no sound. Useful for practicing one hand while reading both. |
| Instrument | Tap to open a picker and choose a different General MIDI instrument for that track during playback (for example, switch from piano to strings). |
| Octave shift | Cycles through three options: no shift (T), 8va (display the notes one octave higher on the staff), and 8vb (display the notes one octave lower on the staff). This adjusts how the notes are drawn on the staff without changing the pitch of the sound. |

A **Set All Instruments to Piano** button is also available to quickly reset every track to a piano sound.

The **Combine / Split to Two Staffs** toggle merges all tracks into two staves (treble and bass clef) or splits a single track into two staves. This is helpful when a MIDI file uses many separate tracks but you want to read it as a standard piano score.

### Display Options

| Setting | Description |
|---|---|
| Scroll Vertically | Choose whether the sheet music scrolls top-to-bottom or left-to-right during playback. |
| Show the Piano | Show or hide the on-screen piano keyboard. |
| Show Lyrics | If the MIDI file contains embedded lyrics, display them under the notes. |
| Show Note Letters | Display the note name next to each note head. You can choose English letters (C, C#), Fixed Do-Re-Mi (Do is always C), Movable Do-Re-Mi (Do follows the key), Fixed Numbers, or Movable Numbers. |
| Show Measure Numbers | Display the measure number at the start of each staff. |

### Notation Options

| Setting | Description |
|---|---|
| Transpose Notes | Shift all notes up or down by a number of semitones. This changes both the sheet music display and the playback pitch. Useful for adjusting a piece to your vocal range or a different key. |
| Key Signature | Override the key signature displayed on the sheet music. MIDI files do not always include key signature information, so this lets you set it manually. Does not affect playback pitch. |
| Time Signature | Override the time signature shown on the sheet music. MIDI files do not always include a time signature, so this lets you set it manually. |
| Combine Interval | Notes played within this number of milliseconds of each other are grouped into the same chord on the sheet music. Increase this value if notes that should be chords are shown as separate notes; decrease it if notes are being incorrectly grouped. |

### Color Options

| Setting | Description |
|---|---|
| Use Note Colors | Assign a distinct color to each note (C, D, E, etc.) to make reading easier. |
| Use Accidental Colors | Highlight sharp and flat notes in red so they stand out at a glance. |
| Right Hand Color | Choose the highlight color used for right-hand notes on the sheet music and piano. |
| Left Hand Color | Choose the highlight color used for left-hand notes on the sheet music and piano. |

### Loop Options

The loop feature lets you practice a specific section of a piece repeatedly:

1. Enable **Play Measures in a Loop**.
2. Set the **Start Measure** and **End Measure** to define the section you want to repeat.
3. Press Play. The app will play from the start measure to the end measure, then jump back and repeat continuously until you stop.

This is particularly useful for working on a difficult passage.

### Count-in Measures

Before playback begins, the app can play a number of silent count-in measures so you have time to prepare. Each count-in measure produces an audible click on every beat, giving you the rhythm before the music starts.

You can set the number of count-in measures (0, 1, 2, or 4) in the settings. Setting it to 0 disables the count-in and starts playback immediately.

---

## Saving Sheet Music as Images

From the playback screen, open the side menu (swipe from the left edge or tap the menu icon) and tap **Save As Images**. The app will render the full sheet music and save each page as a PNG image file to the **Pictures/MidiSheetMusic** folder on your device. You can then share or print these images.

---

## External MIDI Keyboard Support

MidiSheetMusic supports connecting a USB MIDI keyboard or controller to your Android device using a USB OTG adapter. When a compatible device is connected, the app detects it automatically and displays a connection indicator. The sheet music will highlight the notes you play in real time, giving you visual feedback as you practice.

---

## Bundled Songs

The app includes over 60 classical piano pieces ready to play:

Bach, Beethoven, Bizet, Borodin, Brahms, Chopin, Clementi, Field, Grieg, Handel, Liadov, MacDowell, Massenet, Mendelssohn, Mozart, Offenbach, Pachelbel, Prokofiev, Puccini, Rebikov, Saint-Saens, Satie, Schubert, Schumann, Strauss, Tchaikovsky, and Verdi.

A selection of easy beginner pieces is also included (Brahms Lullaby, Greensleeves, Jingle Bells, Silent Night, Twinkle Twinkle Little Star).

---

## License

MidiSheetMusic for Android is free software, released under the [GNU General Public License version 2](https://www.gnu.org/licenses/old-licenses/gpl-2.0.html) (GPL-2.0-only). You are free to use, study, modify, and distribute it under the terms of that license. No proprietary or non-free components are included.

Source code is available at: https://github.com/drake7707/MidiSheetMusic-Android

---

## Contributors

- **Madhav Vaidyanathan** - Original creator of MidiSheetMusic. The original SourceForge project can be found at https://sourceforge.net/projects/midisheetmusic and the original Android fork at https://github.com/MichaelBell/MidiSheetMusic-Android.
- **drake7707** - Updated the app to support modern Android versions after it had not been updated for years, fixed numerous bugs, and added new features.
- **[ankineri](https://github.com/ankineri)** - Implemented USB MIDI keyboard support.

Improvements introduced in this fork include:

- **Android modernization** — updated to compile and run on Android 10–15 (API 30–35); removed legacy dependencies; fixed all compiler warnings.
- **Portrait mode support** — the app previously locked the screen to landscape; it now works in both orientations, with correct resize and redraw on rotation.
- **Compact Track Options panel** — each track's visibility, mute, instrument, octave shift, and volume are all accessible from a single inline row, making multi-track configuration much faster.
- **Per-track octave shift (8va / 8vb)** — adjust how notes are drawn on the staff without changing the playback pitch.
- **Per-track volume sliders** — lower the volume of a backing track while keeping your practice track at full volume; hide and mute are now separate controls.
- **Count-in measures** — configurable 0/1/2/4 count-in measures with an audible beat click before playback begins, so you have time to prepare.
- **BPM display** — the speed label shows both the percentage and the resulting tempo in beats per minute, updating live as you adjust the slider.
- **Bass clef and treble clef filter buttons** — replacing the generic L/R labels for clearer left/right hand selection.
- **Keyboard shortcuts** — full keyboard control for playback (Space, arrows, Page Up/Down, R, +/-, S, E, L); all shortcuts work reliably after tapping the sheet music.
- **Loop region highlight** — the active loop section is shaded on the sheet music; loop restart is zero-delay; S/E/L keys set the loop start, end, and toggle.
- **Track labels** — shows track number and instrument abbreviation above each staff; suppressed when tracks are merged; updates when the instrument changes.
- **Playback engine fixes** — rebuilt playback MIDI track generation to fix note-highlighting sync issues; fixed tempo calculation, instrument restoration after seeking, and measure navigation edge cases.
- **Instrument selection from track row** — tap any track row in settings to open the instrument picker directly; no separate button needed.
- **File type support** — fixed Android manifest intent filters and added SMF MIME type support so the app opens from any file manager; percussion tracks are shown by default.
- **Automated CI/CD** — signed release APK and versioned GitHub release created automatically on every version bump; fastlane changelogs generated from git log.

For a detailed per-feature history see [CHANGELOG.md](CHANGELOG.md).

---

Please feel free to open an issue to report a bug or request a feature:
https://github.com/drake7707/MidiSheetMusic-Android/issues
