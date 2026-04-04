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

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * AllSongsActivity is used to display a list of
 * songs to choose from.  The list is created from the songs
 * shipped with MidiSheetMusic (in the assets directory), and
 * also by searching for midi files in the internal/external
 * device storage.
 * <br/><br/>
 * When a song is chosen, this calls the SheetMusicActivity, passing
 * the raw midi byte[] data as a parameter in the Intent.
 */
public class AllSongsActivity extends AppCompatActivity implements TextWatcher {

    private static final String TAG = AllSongsActivity.class.getSimpleName();

    private ArrayList<FileUri> songlist;
    private EditText filterText;
    private ExecutorService executor;
    private ListView listView;
    private IconArrayAdapter<FileUri> adapter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.choose_song);
        setTitle("MidiSheetMusic: Choose Song");
        executor = Executors.newSingleThreadExecutor();

        listView = findViewById(R.id.list_view);
        filterText = findViewById(R.id.name_filter);
        filterText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        filterText.addTextChangedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only load songs on first resume; preserve any extra files added via scanForSongs()
        if (songlist == null || songlist.isEmpty()) {
            loadSongs();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void loadSongs() {
        songlist = new ArrayList<>();
        loadAssetMidiFiles();
        loadMidiFilesFromProvider(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
        loadMidiFilesFromProvider(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);

        if (songlist.size() > 0) {
            Collections.sort(songlist, songlist.get(0));
        }
        // Remove duplicates
        ArrayList<FileUri> deduplicated = new ArrayList<>();
        String prevName = "";
        for (FileUri file : songlist) {
            if (!file.toString().equals(prevName)) {
                deduplicated.add(file);
            }
            prevName = file.toString();
        }
        songlist = deduplicated;

        adapter = new IconArrayAdapter<>(this, android.R.layout.simple_list_item_1, songlist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileUri file = (FileUri) adapter.getItem(position);
            ChooseSongActivity.openFile(file);
        });
    }

    /** Scan the filesystem for additional MIDI songs in the background */
    public void scanForSongs() {
        Handler handler = new Handler(Looper.getMainLooper());
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        Toast.makeText(this, "Scanning " + rootPath + " for MIDI files", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            ArrayList<FileUri> found = new ArrayList<>();
            try {
                scanDirectory(new File(rootPath), found, 1);
            } catch (Exception e) {
                Log.e(TAG, "Error scanning for MIDI files", e);
            }
            handler.post(() -> scanDone(found));
        });
    }

    private void scanDirectory(File dir, ArrayList<FileUri> found, int depth) {
        if (depth > 10 || Thread.currentThread().isInterrupted()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (Thread.currentThread().isInterrupted()) return;
            String name = file.getName().toLowerCase();
            if (name.endsWith(".mid") || name.endsWith(".midi")) {
                Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                found.add(new FileUri(uri, file.getName()));
            }
        }
        for (File file : files) {
            if (Thread.currentThread().isInterrupted()) return;
            if (file.isDirectory()) {
                scanDirectory(file, found, depth + 1);
            }
        }
    }

    private void scanDone(ArrayList<FileUri> newFiles) {
        if (songlist == null || newFiles == null) return;
        songlist.addAll(newFiles);
        Collections.sort(songlist, songlist.get(0));
        ArrayList<FileUri> deduplicated = new ArrayList<>();
        String prevName = "";
        for (FileUri file : songlist) {
            if (!file.toString().equals(prevName)) {
                deduplicated.add(file);
            }
            prevName = file.toString();
        }
        songlist = deduplicated;
        adapter = new IconArrayAdapter<>(this, android.R.layout.simple_list_item_1, songlist);
        listView.setAdapter(adapter);
        Toast.makeText(this, "Found " + newFiles.size() + " MIDI files", Toast.LENGTH_SHORT).show();
    }

    private void loadAssetMidiFiles() {
        try {
            AssetManager assets = getResources().getAssets();
            for (String path : assets.list("")) {
                if (path.endsWith(".mid")) {
                    Uri uri = Uri.parse("file:///android_asset/" + path);
                    songlist.add(new FileUri(uri, path));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading asset MIDI files", e);
        }
    }

    private void loadMidiFilesFromProvider(Uri contentUri) {
        ContentResolver resolver = getContentResolver();
        String[] columns = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.MIME_TYPE
        };
        String selection = MediaStore.Audio.Media.MIME_TYPE + " LIKE '%mid%'";
        try (Cursor cursor = resolver.query(contentUri, columns, selection, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) return;
            do {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String mime = cursor.getString(mimeCol);
                if (mime != null && (mime.endsWith("/midi") || mime.endsWith("/mid"))) {
                    Uri uri = Uri.withAppendedPath(contentUri, String.valueOf(id));
                    songlist.add(new FileUri(uri, title));
                }
            } while (cursor.moveToNext());
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore", e);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (adapter != null) adapter.getFilter().filter(s);
    }

    @Override public void afterTextChanged(Editable s) {}
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
