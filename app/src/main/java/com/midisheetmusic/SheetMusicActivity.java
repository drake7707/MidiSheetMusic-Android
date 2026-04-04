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

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.midisheetmusic.sheets.ClefSymbol;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * SheetMusicActivity is the main activity. The main components are:
 * <ul>
 *  <li> MidiPlayer : The buttons and speed bar at the top.
 *  <li> Piano : For highlighting the piano notes during playback.
 *  <li> SheetMusic : For displaying and highlighting the sheet music notes during playback.
 * </ul>
 */
public class SheetMusicActivity extends MidiHandlingActivity {

    public static final String MidiTitleID = "MidiTitleID";
    public static final int ID_LOOP_ENABLE = 10;
    public static final int ID_LOOP_START = 11;
    public static final int ID_LOOP_END = 12;

    private MidiPlayer player;
    private Piano piano;
    private SheetMusic sheet;
    private LinearLayout layout;
    private MidiFile midifile;
    private MidiOptions options;
    private long midiCRC;
    private DrawerLayout drawerLayout;

    // Drawer views
    private SwitchCompat switchScrollVert;
    private SwitchCompat switchUseColors;
    private SwitchCompat switchColorAccidentals;
    private SwitchCompat switchLoopEnable;
    private SwitchCompat switchShowMeasures;
    private LinearLayout layoutLoopSubitems;
    private TextView txtLoopArrow;
    private TextView txtLoopStartBadge;
    private TextView txtLoopEndBadge;
    private boolean loopExpanded = false;
    private boolean updatingToggles = false;

    private ActivityResultLauncher<Intent> settingsLauncher;

    /** Create this SheetMusicActivity.
     * The Intent should have two parameters:
     * - data: The uri of the midi file to open.
     * - MidiTitleID: The title of the song (String)
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Register the settings result launcher before any possible finish() calls
        settingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() != android.app.Activity.RESULT_OK
                            || data == null) return;
                    MidiOptions newOptions;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        newOptions = data.getSerializableExtra(
                                SettingsActivity.settingsID, MidiOptions.class);
                    } else {
                        newOptions = (MidiOptions) data.getSerializableExtra(
                                SettingsActivity.settingsID);
                    }
                    if (newOptions != null) {
                        options = newOptions;
                    }
                    for (int i = 0; i < options.instruments.length; i++) {
                        if (options.instruments[i] !=
                                midifile.getTracks().get(i).getInstrument()) {
                            options.useDefaultInstruments = false;
                        }
                    }
                    saveOptions();
                    createSheetMusic(options);
                });

        // Handle back press: save options before finishing
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        saveOptions();
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                });

        hideSystemUI();

        setContentView(R.layout.sheet_music_layout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);

        Uri uri = this.getIntent().getData();
        if (uri == null) {
            this.finish();
            return;
        }
        String title = this.getIntent().getStringExtra(MidiTitleID);
        if (title == null) {
            title = uri.getLastPathSegment();
        }
        FileUri file = new FileUri(uri, title);
        this.setTitle("MidiSheetMusic: " + title);

        byte[] data;
        try {
            data = file.getData(this);
            midifile = new MidiFile(data, title);
        } catch (MidiFileException e) {
            this.finish();
            return;
        }

        options = new MidiOptions(midifile);
        CRC32 crc = new CRC32();
        crc.update(data);
        midiCRC = crc.getValue();
        SharedPreferences settings = getPreferences(0);
        options.scrollVert = settings.getBoolean("scrollVert", false);
        options.shade1Color = settings.getInt("shade1Color", options.shade1Color);
        options.shade2Color = settings.getInt("shade2Color", options.shade2Color);
        options.showPiano = settings.getBoolean("showPiano", true);
        String json = settings.getString("" + midiCRC, null);
        MidiOptions savedOptions = MidiOptions.fromJson(json);
        if (savedOptions != null) {
            options.merge(savedOptions);
        }

        createViews();
    }

    private void createViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        layout = findViewById(R.id.sheet_content);

        // Lock the drawer — it is opened only via the settings button
        drawerLayout.setFitsSystemWindows(false);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);

        // Bind drawer views
        switchScrollVert = drawerLayout.findViewById(R.id.switch_scroll_vert);
        switchUseColors = drawerLayout.findViewById(R.id.switch_use_colors);
        switchColorAccidentals = drawerLayout.findViewById(R.id.switch_color_accidentals);
        switchLoopEnable = drawerLayout.findViewById(R.id.switch_loop_enable);
        switchShowMeasures = drawerLayout.findViewById(R.id.switch_show_measures);
        layoutLoopSubitems = drawerLayout.findViewById(R.id.layout_loop_subitems);
        txtLoopArrow = drawerLayout.findViewById(R.id.txt_loop_arrow);
        txtLoopStartBadge = drawerLayout.findViewById(R.id.txt_loop_start_badge);
        txtLoopEndBadge = drawerLayout.findViewById(R.id.txt_loop_end_badge);

        // Initialise switch states from options
        switchScrollVert.setChecked(options.scrollVert);
        switchUseColors.setChecked(options.useColors);
        switchColorAccidentals.setChecked(options.colorAccidentals);
        switchLoopEnable.setChecked(options.playMeasuresInLoop);
        switchShowMeasures.setChecked(options.showMeasures);
        txtLoopStartBadge.setText(String.valueOf(options.playMeasuresInLoopStart + 1));
        txtLoopEndBadge.setText(String.valueOf(options.playMeasuresInLoopEnd + 1));

        switchScrollVert.setOnCheckedChangeListener((btn, checked) -> {
            options.scrollVert = checked;
            createSheetMusic(options);
        });

        switchUseColors.setOnCheckedChangeListener((btn, checked) -> {
            if (updatingToggles) return;
            updatingToggles = true;
            if (checked) {
                options.colorAccidentals = false;
                switchColorAccidentals.setChecked(false);
            }
            options.useColors = checked;
            updatingToggles = false;
            createSheetMusic(options);
        });

        switchColorAccidentals.setOnCheckedChangeListener((btn, checked) -> {
            if (updatingToggles) return;
            updatingToggles = true;
            if (checked) {
                options.useColors = false;
                switchUseColors.setChecked(false);
            }
            options.colorAccidentals = checked;
            updatingToggles = false;
            createSheetMusic(options);
        });

        switchLoopEnable.setOnCheckedChangeListener((btn, checked) ->
                options.playMeasuresInLoop = checked);

        switchShowMeasures.setOnCheckedChangeListener((btn, checked) -> {
            options.showMeasures = checked;
            createSheetMusic(options);
        });

        // Expand/collapse loop sub-items when the header row is tapped
        drawerLayout.findViewById(R.id.layout_loop_header).setOnClickListener(v -> {
            loopExpanded = !loopExpanded;
            layoutLoopSubitems.setVisibility(loopExpanded ? View.VISIBLE : View.GONE);
            txtLoopArrow.setText(loopExpanded ? "\u25BC" : "\u25B6");
        });

        drawerLayout.findViewById(R.id.layout_loop_start).setOnClickListener(v ->
                showMeasurePicker(true));

        drawerLayout.findViewById(R.id.layout_loop_end).setOnClickListener(v ->
                showMeasurePicker(false));

        drawerLayout.findViewById(R.id.btn_song_settings).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            changeSettings();
        });

        drawerLayout.findViewById(R.id.btn_save_images).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            showSaveImagesDialog();
        });

        player = new MidiPlayer(this);
        player.setDrawerLayout(drawerLayout);
        layout.addView(player);

        piano = new Piano(this);
        layout.addView(piano);
        player.SetPiano(piano);
        layout.requestLayout();

        player.setSheetUpdateRequestListener(() -> createSheetMusic(options));
        createSheetMusic(options);
    }

    private void showMeasurePicker(boolean isStart) {
        if (options.lastMeasure <= 0) return;
        String[] items = new String[options.lastMeasure + 1];
        for (int i = 0; i < items.length; i++) items[i] = String.valueOf(i + 1);
        int currentSelection = isStart ? options.playMeasuresInLoopStart : options.playMeasuresInLoopEnd;

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isStart ? R.string.play_measures_in_loop_start : R.string.play_measures_in_loop_end)
                .setItems(items, (d, i) -> {
                    if (isStart) {
                        options.playMeasuresInLoopStart = i;
                        if (options.playMeasuresInLoopStart > options.playMeasuresInLoopEnd) {
                            options.playMeasuresInLoopEnd = options.playMeasuresInLoopStart;
                            txtLoopEndBadge.setText(items[i]);
                        }
                        txtLoopStartBadge.setText(items[i]);
                    } else {
                        options.playMeasuresInLoopEnd = i;
                        if (options.playMeasuresInLoopStart > options.playMeasuresInLoopEnd) {
                            options.playMeasuresInLoopStart = options.playMeasuresInLoopEnd;
                            txtLoopStartBadge.setText(items[i]);
                        }
                        txtLoopEndBadge.setText(items[i]);
                    }
                })
                .create();
        dialog.show();
        dialog.getListView().setSelection(currentSelection);
    }

    /** Create the SheetMusic view with the given options */
    private void createSheetMusic(MidiOptions options) {
        if (sheet != null) {
            layout.removeView(sheet);
        }
        piano.setVisibility(options.showPiano ? View.VISIBLE : View.GONE);
        sheet = new SheetMusic(this);
        sheet.init(midifile, options);
        sheet.setPlayer(player);
        layout.addView(sheet);
        piano.SetMidiFile(midifile, options, player);
        piano.SetShadeColors(options.shade1Color, options.shade2Color);
        player.SetMidiFile(midifile, options, sheet);
        player.updateToolbarButtons();
        layout.requestLayout();
        sheet.draw();
    }

    /** Always display this activity in landscape mode. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /** To change the sheet music options, start the SettingsActivity. */
    private void changeSettings() {
        MidiOptions defaultOptions = new MidiOptions(midifile);
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.settingsID, options);
        intent.putExtra(SettingsActivity.defaultSettingsID, defaultOptions);
        settingsLauncher.launch(intent);
    }

    /** Show the "Save As Images" dialog */
    private void showSaveImagesDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.save_images_dialog, layout, false);
        final EditText filenameView = dialogView.findViewById(R.id.save_images_filename);
        filenameView.setText(midifile.getFileName().replace("_", " "));
        new AlertDialog.Builder(this)
                .setTitle(R.string.save_images_str)
                .setView(dialogView)
                .setPositiveButton("OK",
                        (d, w) -> saveAsImages(filenameView.getText().toString()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Save the current sheet music as PNG images */
    private void saveAsImages(String name) {
        String filename = URLEncoder.encode(name, StandardCharsets.UTF_8);

        if (!options.scrollVert) {
            options.scrollVert = true;
            createSheetMusic(options);
        }
        try {
            int numpages = sheet.GetTotalPages();
            for (int page = 1; page <= numpages; page++) {
                Bitmap image = Bitmap.createBitmap(
                        SheetMusic.PageWidth + 40, SheetMusic.PageHeight + 40,
                        Bitmap.Config.ARGB_8888);
                Canvas imageCanvas = new Canvas(image);
                sheet.DrawPage(imageCanvas, page);
                saveBitmapToFile(image, filename + page);
                image.recycle();
            }
            Toast.makeText(this, "Images saved to Pictures/MidiSheetMusic", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            new AlertDialog.Builder(this)
                    .setMessage("Error saving image: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (OutOfMemoryError e) {
            new AlertDialog.Builder(this)
                    .setMessage("Ran out of memory while saving images.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void saveBitmapToFile(Bitmap bitmap, String baseFilename) throws IOException {
        String fileName = baseFilename + ".png";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/MidiSheetMusic");
            Uri imageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri == null) throw new IOException("Could not create MediaStore entry");
            try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                if (out == null) throw new IOException("Could not open output stream");
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } else {
            java.io.File dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES + "/MidiSheetMusic");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("Could not create directory " + dir);
            }
            java.io.File file = new java.io.File(dir, fileName);
            try (OutputStream out = new java.io.FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            android.media.MediaScannerConnection.scanFile(this,
                    new String[]{file.getAbsolutePath()}, null, null);
        }
    }

    /** Save the options in the SharedPreferences */
    private void saveOptions() {
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putBoolean("scrollVert", options.scrollVert);
        editor.putInt("shade1Color", options.shade1Color);
        editor.putInt("shade2Color", options.shade2Color);
        editor.putBoolean("showPiano", options.showPiano);
        for (int i = 0; i < options.noteColors.length; i++) {
            editor.putInt("noteColor" + i, options.noteColors[i]);
        }
        String json = options.toJson();
        if (json != null) {
            editor.putString("" + midiCRC, json);
        }
        editor.apply();
    }

    /** When this activity resumes, redraw all the views */
    @Override
    protected void onResume() {
        super.onResume();
        if (layout != null) {
            layout.requestLayout();
            player.invalidate();
            piano.invalidate();
            if (sheet != null) sheet.invalidate();
        }
    }

    /** When this activity pauses, stop the music */
    @Override
    protected void onPause() {
        if (player != null) {
            player.Pause();
        }
        super.onPause();
    }

    @Override
    void OnMidiDeviceStatus(boolean connected) {
        player.OnMidiDeviceStatus(connected);
    }

    @Override
    void OnMidiNote(int note, boolean pressed) {
        player.OnMidiNote(note, pressed);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), decorView);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}
