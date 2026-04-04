package com.midisheetmusic;

import java.util.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import android.content.*;
import org.json.*;
import android.graphics.*;

import androidx.appcompat.app.AppCompatActivity;


/** @class RecentSongsActivity
 * The RecentSongsActivity class displays a list of songs
 * that were recently accessed.  The list comes from the
 * SharedPreferences ????
 */
public class RecentSongsActivity extends AppCompatActivity {
    private ArrayList<FileUri> filelist; /* List of recent files opened */
    private ListView listView;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_list);
        setTitle("MidiSheetMusic: Recent Songs");
        listView = findViewById(R.id.list_view);
        listView.setBackgroundColor(Color.rgb(0, 0, 0));
    }

    private void loadFileList() {
        filelist = new ArrayList<FileUri>();
        SharedPreferences settings = getSharedPreferences("midisheetmusic.recentFiles", 0);
        String recentFilesString = settings.getString("recentFiles", null);
        if (recentFilesString == null) {
            return;
        }
        try {
            JSONArray jsonArray = new JSONArray(recentFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                FileUri file = FileUri.fromJson(obj, this);
                if (file != null) {
                    filelist.add(file);
                }
            }
        }
        catch (Exception e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFileList();
        IconArrayAdapter<FileUri> adapter = new IconArrayAdapter<>(this, android.R.layout.simple_list_item_1, filelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FileUri file = (FileUri) adapter.getItem(position);
            ChooseSongActivity.openFile(file);
        });
    }
}



