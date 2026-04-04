/*
 * Copyright (c) 2011-2013 Madhav Vaidyanathan
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
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ChooseSongActivity is a tabbed view for choosing a song to play.
 * There are 3 tabs:
 * <ul>
 *  <li> All    (AllSongsFragment)    : Display a list of all songs
 *  <li> Recent (RecentSongsFragment) : Display a list of recently opened songs
 *  <li> Browse (FileBrowserFragment) : Let the user browse the filesystem for songs
 */
public class ChooseSongActivity extends AppCompatActivity {

    private static final String TAG = ChooseSongActivity.class.getSimpleName();
    static ChooseSongActivity globalActivity;

    @Override
    public void onCreate(Bundle state) {
        globalActivity = this;
        super.onCreate(state);
        setContentView(R.layout.activity_choose_song);

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        ChooseSongPagerAdapter adapter = new ChooseSongPagerAdapter(this);
        viewPager.setAdapter(adapter);

        String[] tabTitles = {"All", "Recent", "Browse"};
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();
    }

    @Override
    protected void onResume() {
        super.onResume();
        globalActivity = this;
    }

    public static void openFile(FileUri file) {
        if (globalActivity != null) {
            globalActivity.doOpenFile(file);
        }
    }

    public void doOpenFile(FileUri file) {
        byte[] data = file.getData(this);
        if (data == null || data.length <= 6 || !MidiFile.hasMidiHeader(data)) {
            showErrorDialog("Error: Unable to open song: " + file, this);
            return;
        }
        updateRecentFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW, file.getUri(), this, SheetMusicActivity.class);
        intent.putExtra(SheetMusicActivity.MidiTitleID, file.toString());
        startActivity(intent);
    }

    /** Show an error dialog with the given message */
    public static void showErrorDialog(String message, Activity activity) {
        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", null)
                .show();
    }

    /** Save the given FileUri into the "recentFiles" preferences.
     *  Save a maximum of 10 recent files.
     */
    public void updateRecentFile(FileUri recentfile) {
        try {
            SharedPreferences settings = getSharedPreferences("midisheetmusic.recentFiles", 0);
            SharedPreferences.Editor editor = settings.edit();
            String recentFilesString = settings.getString("recentFiles", null);
            JSONArray prevRecentFiles = recentFilesString != null
                    ? new JSONArray(recentFilesString)
                    : new JSONArray();

            JSONObject recentFileJson = recentfile.toJson();
            JSONArray recentFiles = new JSONArray();
            recentFiles.put(recentFileJson);
            for (int i = 0; i < prevRecentFiles.length() && i < 10; i++) {
                JSONObject f = prevRecentFiles.getJSONObject(i);
                if (!FileUri.equalJson(recentFileJson, f)) {
                    recentFiles.put(f);
                }
            }
            editor.putString("recentFiles", recentFiles.toString());
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error updating recent files", e);
        }
    }
}

