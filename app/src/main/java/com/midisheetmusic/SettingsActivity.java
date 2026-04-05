/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */


package com.midisheetmusic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;
import androidx.core.os.BundleCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

/**
 * This activity is created by the "Settings" menu option.
 * The user can change settings such as: <ul>
 *  <li/> Which tracks to display
 *  <li/> Which tracks to mute
 *  <li/> Which instruments to use during playback
 *  <li/> Whether to scroll horizontally or vertically
 *  <li/> Whether to display the piano or not
 *  <li/> Whether to display note letters or not
 *  <li/> Transpose the notes to another key
 *  <li/> Change the key signature or time signature displayed
 *  <li/> Change how notes are combined into chords (the time interval)
 *  <li/> Change the colors for shading the left/right hands.
 *  <li/> Whether to display measure numbers
 *  <li/> Play selected measures in a loop
 * </ul>
 *
 * When created, pass an Intent parameter containing MidiOptions.
 * When destroyed, this activity passes the result MidiOptions to the Intent.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String settingsID = "settings";
    public static final String defaultSettingsID = "defaultSettings";

    private MidiOptions defaultOptions;  /** The initial option values */
    private MidiOptions options;         /** The option values */

    /** Create the Settings activity. Retrieve the initial option values
     *  (MidiOptions) from the Intent.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // Use the explicit Toolbar defined in the layout (AppTheme has no ActionBar)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        options = IntentCompat.getSerializableExtra(getIntent(), settingsID, MidiOptions.class);
        defaultOptions = IntentCompat.getSerializableExtra(getIntent(), defaultSettingsID, MidiOptions.class);

        // Pass options to the fragment
        Fragment settingsFragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(SettingsActivity.settingsID, options);
        bundle.putSerializable(SettingsActivity.defaultSettingsID, defaultOptions);
        settingsFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                SettingsFragment settingsFragment = (SettingsFragment)
                        getSupportFragmentManager().findFragmentById(R.id.settings);
                if (settingsFragment != null) {
                    settingsFragment.updateOptions();
                }
                Intent intent = new Intent();
                intent.putExtra(SettingsActivity.settingsID, options);
                setResult(Activity.RESULT_OK, intent);
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /** Handle 'Up' button press */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        private MidiOptions defaultOptions;  /** The initial option values */
        private MidiOptions options;         /** The option values */

        private Preference restoreDefaults;           /** Restore default settings */
        private TrackRowPreference[] trackOptions;    /** Combined track options rows */
        private Preference setAllToPiano;             /** Set all instruments to piano */
        private SwitchPreferenceCompat showLyrics;        /** Show the lyrics */
        private SwitchPreferenceCompat twoStaffs;         /** Combine tracks into two staffs */
        private ListPreference showNoteLetters;       /** Show the note letters */
        private ListPreference transpose;             /** Transpose notes */
        private ListPreference midiShift;             /** Control MIDI shift */
        private ListPreference key;                   /** Key Signature to use */
        private ListPreference time;                  /** Time Signature to use */
        private ListPreference combineInterval;       /** Interval (msec) to combine notes */
        private ListPreference countInMeasures;       /** Count-in measures before playing */

        private ColorPreference[] noteColors;
        private SwitchPreferenceCompat useColors;
        private SwitchPreferenceCompat colorAccidentals;    /** Use RED as color for sharps and flats */
        private SwitchPreferenceCompat useFullHeight;       /** Drawing on full height option */

        private ColorPreference shade1Color;          /** Right-hand color */
        private ColorPreference shade2Color;          /** Left-hand color */

        private Context context;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (getArguments() != null) {
                options = BundleCompat.getSerializable(
                        getArguments(), SettingsActivity.settingsID, MidiOptions.class);
                defaultOptions = BundleCompat.getSerializable(
                        getArguments(), SettingsActivity.defaultSettingsID, MidiOptions.class);
            }
            context = getPreferenceManager().getContext();
            createView();
        }

        /** Create all the preference widgets in the view */
        private void createView() {
            PreferenceScreen root = getPreferenceManager().createPreferenceScreen(context);
            createRestoreDefaultPrefs(root);
            createTrackOptionsPrefs(root);

            PreferenceCategory sheetTitle = new PreferenceCategory(context);
            sheetTitle.setTitle(R.string.sheet_prefs_title);
            root.addPreference(sheetTitle);

            createShowLyricsPrefs(root);
            if (options.tracks.length != 2) {
                createTwoStaffsPrefs(root);
            }
            createShowLetterPrefs(root);
            createTransposePrefs(root);
            createMidiShiftPrefs(root);
            createKeySignaturePrefs(root);
            createTimeSignaturePrefs(root);
            createCombineIntervalPrefs(root);
            createCountInMeasuresPrefs(root);
            createColorPrefs(root);
            applyNoIconSpace(root);
            setPreferenceScreen(root);
        }

        /** For each list dialog, we display the value selected in the "summary" text.
         *  When a new value is selected from the list dialog, update the summary
         *  to the selected entry.
         */
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ListPreference list = (ListPreference) preference;
            int index = list.findIndexOfValue((String)newValue);
            CharSequence entry = list.getEntries()[index];
            preference.setSummary(entry);
            return true;
        }

        /** When the 'restore defaults' preference is clicked, restore the default settings */
        public boolean onPreferenceClick(Preference preference) {
            if (preference == restoreDefaults) {
                options = defaultOptions.copy();
                createView();
            }
            else if (preference == setAllToPiano) {
                for (int i = 0; i < options.instruments.length; i++) {
                    options.instruments[i] = 0;
                }
                createView();
            }
            return true;
        }


        /** Create the combined "Track Options" section with one row per track. */
        private void createTrackOptionsPrefs(PreferenceScreen root) {
            PreferenceCategory trackOptionsTitle = new PreferenceCategory(context);
            trackOptionsTitle.setTitle(R.string.track_options);
            root.addPreference(trackOptionsTitle);

            trackOptions = new TrackRowPreference[options.tracks.length];
            for (int i = 0; i < options.tracks.length; i++) {
                trackOptions[i] = new TrackRowPreference(
                        context, i,
                        options.tracks[i],
                        options.mute[i],
                        options.instruments[i],
                        options.trackOctaveShift[i]);
                root.addPreference(trackOptions[i]);
            }

            /* "Set All to Piano" sits below a category separator to visually separate
             * it as a bulk action for all tracks in this section. */
            PreferenceCategory allTracksActions = new PreferenceCategory(context);
            root.addPreference(allTracksActions);

            setAllToPiano = new Preference(context);
            setAllToPiano.setTitle(R.string.set_all_to_piano);
            setAllToPiano.setOnPreferenceClickListener(this);
            root.addPreference(setAllToPiano);
        }

        /** Create the "Show Lyrics" preference */
        private void createShowLyricsPrefs(PreferenceScreen root) {
            showLyrics = new SwitchPreferenceCompat(context);
            showLyrics.setTitle(R.string.show_lyrics);
            showLyrics.setChecked(options.showLyrics);
            root.addPreference(showLyrics);

            useFullHeight = new SwitchPreferenceCompat(context);
            useFullHeight.setTitle(R.string.use_full_height);
            useFullHeight.setChecked(options.useFullHeight);

            useFullHeight.setOnPreferenceChangeListener((preference, isChecked) -> {
                options.useFullHeight = (boolean) isChecked;

                return true;
            });
            root.addPreference(useFullHeight);
        }

        /** Create the "Show Note Letters" preference */
        private void createShowLetterPrefs(PreferenceScreen root) {
            showNoteLetters = new ListPreference(context);
            showNoteLetters.setOnPreferenceChangeListener(this);
            showNoteLetters.setKey("show_note_letters");
            showNoteLetters.setTitle(R.string.show_note_letters);
            showNoteLetters.setEntries(R.array.show_note_letter_entries);
            showNoteLetters.setEntryValues(R.array.show_note_letter_values);
            showNoteLetters.setValueIndex(options.showNoteLetters);
            showNoteLetters.setSummary(showNoteLetters.getEntry());
//            DialogPreference x = new DialogPreference(context);
            root.addPreference(showNoteLetters);
        }


        /** Create the "Combine to Two Staffs" preference. */
        private void createTwoStaffsPrefs(PreferenceScreen root) {
            twoStaffs = new SwitchPreferenceCompat(context);
            if (options.tracks.length == 1) {
                twoStaffs.setTitle(R.string.split_to_two_staffs);
                twoStaffs.setSummary(R.string.split_to_two_staffs_summary);
            }
            else {
                twoStaffs.setTitle(R.string.combine_to_two_staffs);
                twoStaffs.setSummary(R.string.combine_to_two_staffs_summary);
            }
            twoStaffs.setChecked(options.twoStaffs);
            root.addPreference(twoStaffs);
        }

        /** Create the "Transpose Notes" preference.
         *  The values range from 12, 11, 10, .. -10, -11, -12
         */
        private void createTransposePrefs(PreferenceScreen root) {
            int transposeIndex = 12 - options.transpose;
            transpose = new ListPreference(context);
            transpose.setKey("transpose");
            transpose.setOnPreferenceChangeListener(this);
            transpose.setTitle(R.string.transpose);
            transpose.setEntries(R.array.transpose_entries);
            transpose.setEntryValues(R.array.transpose_values);
            transpose.setValueIndex(transposeIndex);
            transpose.setSummary(transpose.getEntry());
            root.addPreference(transpose);
        }

        /** Create the "Shift MIDI Input" preference.
         *  It shifts the input received via MIDI interface with
         *  a value in the range 12, 11, 10, .. -10, -11, -12
         */
        private void createMidiShiftPrefs(PreferenceScreen root) {
            int midiShiftIndex = 12 - options.midiShift;
            midiShift = new ListPreference(context);
            midiShift.setKey("midi_shift");
            midiShift.setOnPreferenceChangeListener(this);
            midiShift.setTitle(R.string.midiShift);
            midiShift.setEntries(R.array.transpose_entries);
            midiShift.setEntryValues(R.array.transpose_values);
            midiShift.setValueIndex(midiShiftIndex);
            midiShift.setSummary(midiShift.getEntry());
            root.addPreference(midiShift);
        }

        /** Create the "Key Signature" preference */
        private void createKeySignaturePrefs(PreferenceScreen root) {
            key = new ListPreference(context);
            key.setOnPreferenceChangeListener(this);
            key.setKey("key_signature");
            key.setTitle(R.string.key_signature);
            key.setEntries(R.array.key_signature_entries);
            key.setEntryValues(R.array.key_signature_values);
            key.setValueIndex(options.key + 1);
            key.setSummary(key.getEntry());
            root.addPreference(key);
        }

        /** Create the "Time Signature" preference */
        private void createTimeSignaturePrefs(PreferenceScreen root) {
            String[] values = { "Default", "3/4", "4/4" };
            int selected = 0;
            if (options.time != null && options.time.getNumerator() == 3)
                selected = 1;
            else if (options.time != null && options.time.getNumerator() == 4)
                selected = 2;

            time = new ListPreference(context);
            time.setKey("time_signature");
            time.setOnPreferenceChangeListener(this);
            time.setTitle(R.string.time_signature);
            time.setEntries(values);
            time.setEntryValues(values);
            time.setValueIndex(selected);
            time.setSummary(time.getEntry());
            root.addPreference(time);
        }


        /** Create the "Combine Notes Within Interval"  preference.
         *  Notes within N milliseconds are combined into a single chord,
         *  even though their start times may be slightly different.
         */
        private void createCombineIntervalPrefs(PreferenceScreen root) {
            int selected = options.combineInterval/20  - 1;
            combineInterval = new ListPreference(context);
            combineInterval.setKey("combine_interval");
            combineInterval.setOnPreferenceChangeListener(this);
            combineInterval.setTitle(R.string.combine_interval);
            combineInterval.setEntries(R.array.combine_interval_entries);
            combineInterval.setEntryValues(R.array.combine_interval_values);
            combineInterval.setValueIndex(selected);
            combineInterval.setSummary(combineInterval.getEntry() );
            root.addPreference(combineInterval);
        }

        /** Create the "Count-in measures" preference.
         *  Plays N measures of beat clicks before playback starts.
         */
        private void createCountInMeasuresPrefs(PreferenceScreen root) {
            countInMeasures = new ListPreference(context);
            countInMeasures.setKey("CountInMeasures");
            countInMeasures.setOnPreferenceChangeListener((preference, newValue) -> {
                options.countInMeasures = Integer.parseInt((String) newValue);
                countInMeasures.setValueIndex(options.countInMeasures);
                return true;
            });
            countInMeasures.setSummaryProvider((preference) -> {
                int val = Integer.parseInt(((ListPreference) preference).getValue());
                return val == 0
                        ? context.getString(R.string.count_in_measures_none)
                        : context.getResources().getQuantityString(
                                R.plurals.count_in_measures_summary, val, val);
            });
            countInMeasures.setTitle(R.string.count_in_measures);
            countInMeasures.setEntries(R.array.count_in_measures_entries);
            countInMeasures.setEntryValues(R.array.count_in_measures_values);
            countInMeasures.setPersistent(false);
            countInMeasures.setValueIndex(options.countInMeasures);
            root.addPreference(countInMeasures);
        }


        /* Create the "Left-hand color" and "Right-hand color" preferences */
        private void createColorPrefs(PreferenceScreen root) {
            PreferenceCategory localPreferenceCategory = new PreferenceCategory(context);
            localPreferenceCategory.setTitle("Select Colors");
            root.addPreference(localPreferenceCategory);

            shade1Color = new ColorPreference(context);
            shade1Color.setColor(options.shade1Color);
            shade1Color.setTitle(R.string.right_hand_color);
            root.addPreference(shade1Color);

            shade2Color = new ColorPreference(context);
            shade2Color.setColor(options.shade2Color);
            shade2Color.setTitle(R.string.left_hand_color);
            root.addPreference(shade2Color);

            colorAccidentals = new SwitchPreferenceCompat(context);
            colorAccidentals.setTitle(R.string.use_accidental_colors);
            colorAccidentals.setChecked(options.colorAccidentals);

            colorAccidentals.setOnPreferenceChangeListener((preference, isChecked) -> {
                if ((boolean)isChecked == true)
                    useColors.setChecked(false);

                boolean isuseColorChecked = useColors.isChecked();
                for (ColorPreference noteColorPref : noteColors) {
                    noteColorPref.setVisible(isuseColorChecked);
                }

                return true;
            });
            root.addPreference(colorAccidentals);

            useColors = new SwitchPreferenceCompat(context);
            useColors.setTitle(R.string.use_note_colors);
            useColors.setChecked(options.useColors);
            useColors.setOnPreferenceChangeListener((preference, isChecked) -> {
                if ((boolean)isChecked == true)
                    colorAccidentals.setChecked(false);

                for (ColorPreference noteColorPref : noteColors) {
                    noteColorPref.setVisible((boolean)isChecked);
                }
                return true;
            });
            root.addPreference(useColors);

            noteColors = new ColorPreference[options.noteColors.length];
            for (int i = 0; i < 12; i++) {
                noteColors[i] = new ColorPreference(context);
                noteColors[i].setColor(options.noteColors[i]);
                noteColors[i].setTitle(new String[]
                        {"A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"}[i]);
                noteColors[i].setVisible(options.useColors);
                root.addPreference(noteColors[i]);
            }
        }

        /**
         * Remove the icon placeholder reserved space from every preference in the
         * screen so that all item titles align at the same left margin as the custom
         * TrackRowPreference rows (which have no icon frame in their layout).
         */
        private void applyNoIconSpace(PreferenceGroup group) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference p = group.getPreference(i);
                p.setIconSpaceReserved(false);
                if (p instanceof PreferenceGroup) {
                    applyNoIconSpace((PreferenceGroup) p);
                }
            }
        }

        /** Create the "Restore Default Settings" preference */
        private void createRestoreDefaultPrefs(PreferenceScreen root) {
            restoreDefaults = new Preference(context);
            restoreDefaults.setTitle(R.string.restore_defaults);
            restoreDefaults.setOnPreferenceClickListener(this);
            root.addPreference(restoreDefaults);
        }

        /** Update the MidiOptions based on the preferences selected. */
        private void updateOptions() {
            for (int i = 0; i < trackOptions.length; i++) {
                options.tracks[i] = trackOptions[i].isTrackVisible();
                options.mute[i] = trackOptions[i].isTrackMuted();
                options.instruments[i] = trackOptions[i].getInstrumentIndex();
                options.trackOctaveShift[i] = trackOptions[i].getOctaveShift();
            }
            for (int i = 0; i < options.noteColors.length; i++) {
                options.noteColors[i] = noteColors[i].getColor();
            }
            options.showLyrics = showLyrics.isChecked();
            if (twoStaffs != null)
                options.twoStaffs = twoStaffs.isChecked();
            else
                options.twoStaffs = false;

            options.showNoteLetters = Integer.parseInt(showNoteLetters.getValue());
            options.transpose = Integer.parseInt(transpose.getValue());
            options.midiShift = Integer.parseInt(midiShift.getValue());
            options.key = Integer.parseInt(key.getValue());
            switch (time.getValue()) {
                case "Default":
                    options.time = null;
                    break;
                case "3/4":
                    options.time = new TimeSignature(3, 4, options.defaultTime.getQuarter(),
                            options.defaultTime.getTempo());
                    break;
                case "4/4":
                    options.time = new TimeSignature(4, 4, options.defaultTime.getQuarter(),
                            options.defaultTime.getTempo());
                    break;
            }
            options.combineInterval = Integer.parseInt(combineInterval.getValue());
            options.countInMeasures = Integer.parseInt(countInMeasures.getValue());
            options.shade1Color = shade1Color.getColor();
            options.shade2Color = shade2Color.getColor();
            options.useColors = useColors.isChecked();
            options.colorAccidentals = colorAccidentals.isChecked();
            options.useFullHeight = useFullHeight.isChecked();
        }

        @Override
        public void onStop() {
            updateOptions();
            super.onStop();
        }

        @Override
        public void onPause() {
            updateOptions();
            super.onPause();
        }
    }
}


