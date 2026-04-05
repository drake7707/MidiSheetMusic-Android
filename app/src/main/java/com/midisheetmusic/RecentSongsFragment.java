package com.midisheetmusic;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class RecentSongsFragment extends Fragment {

    private static final String TAG = RecentSongsFragment.class.getSimpleName();

    private ArrayList<FileUri> filelist;
    private ListView listView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.list_view);
        listView.setBackgroundColor(Color.rgb(0, 0, 0));
        loadFileList();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFileList();
    }

    private void loadFileList() {
        filelist = new ArrayList<>();
        android.content.SharedPreferences settings =
                requireContext().getSharedPreferences("midisheetmusic.recentFiles", 0);
        String recentFilesString = settings.getString("recentFiles", null);
        if (recentFilesString != null) {
            try {
                JSONArray jsonArray = new JSONArray(recentFilesString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    FileUri file = FileUri.fromJson(obj);
                    if (file != null) filelist.add(file);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing recent files JSON", e);
            }
        }
        IconArrayAdapter<FileUri> adapter = new IconArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, filelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            FileUri file = adapter.getItem(position);
            ChooseSongActivity.openFile(file);
        });
    }
}
