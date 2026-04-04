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

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;


public class FileBrowserActivity extends AppCompatActivity {
    private static final String TAG = FileBrowserActivity.class.getSimpleName();
    private String directory;            /* Current directory being displayed */
    private TextView directoryView;      /* TextView showing directory name */
    private String rootdir;              /* The top level root directory */
    private ListView listView;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.file_browser);
        setTitle("MidiSheetMusic: Browse Files");
        listView = findViewById(R.id.list_view);
    }

    @Override
    public void onResume() {
        super.onResume();
        rootdir = Environment.getExternalStorageDirectory().getAbsolutePath();
        directoryView = findViewById(R.id.directory);
        SharedPreferences settings = getPreferences(0);
        String lastBrowsedDirectory = settings.getString("lastBrowsedDirectory", null);
        if (lastBrowsedDirectory == null) {
            lastBrowsedDirectory = rootdir;
        }
        loadDirectory(lastBrowsedDirectory);
    }

    /* Scan the files in the new directory, and store them in the filelist.
     * Update the UI by refreshing the list adapter.
     */
    private void loadDirectory(String newdirectory) {
        if (newdirectory.equals("../")) {
            try {
                directory = new File(directory).getParent();
            }
            catch (Exception e) {
                Log.e(TAG, "Error navigating to parent directory", e);
            }
        }
        else {
            directory = newdirectory;
        }
        if (directory == null) return;
        // Do not navigate to root directory
        if (directory.equals("/") || directory.equals("//")) {
            return;
        }
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putString("lastBrowsedDirectory", directory);
        editor.apply();
        directoryView.setText(directory);

        /* List of files in the directory */
        ArrayList<FileUri> filelist = new ArrayList<>();
        ArrayList<FileUri> sortedDirs = new ArrayList<>();
        ArrayList<FileUri> sortedFiles = new ArrayList<>();
        File dir = new File(directory);
        // If we're not at the root directory, add parent directory to the list
        if (dir.compareTo(new File(rootdir)) != 0) {
            String parentDirectory = new File(directory).getParent() + "/";
            Uri uri = Uri.parse("file://" + parentDirectory);
            sortedDirs.add(new FileUri(uri, "../"));
        }
        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file == null) {
                        continue;
                    }
                    String filename = file.getName();
                    if (file.isDirectory()) {
                        Uri uri = Uri.parse("file://" + file.getAbsolutePath() + "/");
                        FileUri fileuri = new FileUri(uri, file.getName());
                        sortedDirs.add(fileuri);
                    }
                    else if (filename.endsWith(".mid") || filename.endsWith(".MID") ||
                             filename.endsWith(".midi") || filename.endsWith(".MIDI")) {

                        Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                        FileUri fileuri = new FileUri(uri, uri.getLastPathSegment());
                        sortedFiles.add(fileuri);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing directory: " + directory, e);
        }

        if (sortedDirs.size() > 0) {
            Collections.sort(sortedDirs, sortedDirs.get(0));
        }
        if (sortedFiles.size() > 0) {
            Collections.sort(sortedFiles, sortedFiles.get(0));
        }
        filelist.addAll(sortedDirs);
        filelist.addAll(sortedFiles);

        IconArrayAdapter<FileUri> adapter =
                new IconArrayAdapter<>(this, android.R.layout.simple_list_item_1, filelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileUri file = (FileUri) adapter.getItem(position);
            if (file.isDirectory()) {
                String path = file.getUri().getPath();
                if (path != null) {
                    loadDirectory(path);
                }
            } else {
                ChooseSongActivity.openFile(file);
            }
        });
    }

    public void onHomeClick(View view) {
        loadDirectory(rootdir);
    }
}



