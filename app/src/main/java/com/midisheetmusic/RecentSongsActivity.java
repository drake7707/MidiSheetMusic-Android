package com.midisheetmusic;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Displays a list of recently opened MIDI songs (persisted in SharedPreferences).
 * Tapping a song opens it in SheetMusicActivity.
 */
public class RecentSongsActivity extends AppCompatActivity {

    private static final String TAG = RecentSongsActivity.class.getSimpleName();

    private ArrayList<FileUri> filelist;
    private ListView listView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_list);
        setTitle("MidiSheetMusic: Recent Songs");
        listView = findViewById(R.id.list_view);
        listView.setBackgroundColor(Color.rgb(0, 0, 0));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFileList();
        IconArrayAdapter<FileUri> adapter = new IconArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, filelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileUri file = (FileUri) adapter.getItem(position);
            ChooseSongActivity.openFile(file);
        });
    }

    private void loadFileList() {
        filelist = new ArrayList<>();
        SharedPreferences settings = getSharedPreferences("midisheetmusic.recentFiles", 0);
        String recentFilesString = settings.getString("recentFiles", null);
        if (recentFilesString == null) return;
        try {
            JSONArray jsonArray = new JSONArray(recentFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                FileUri file = FileUri.fromJson(obj);
                if (file != null) filelist.add(file);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading recent files", e);
        }
    }
}




