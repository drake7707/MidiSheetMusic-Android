package com.midisheetmusic;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileBrowserFragment extends Fragment {

    private static final String TAG = FileBrowserFragment.class.getSimpleName();
    private static final String PREFS_NAME = "FileBrowserPrefs";

    private String directory;
    private String rootdir;
    private TextView directoryView;
    private ListView listView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.file_browser, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        directoryView = view.findViewById(R.id.directory);
        listView = view.findViewById(R.id.list_view);

        view.findViewById(R.id.home_dir).setOnClickListener(v -> loadDirectory(rootdir));

        rootdir = Environment.getExternalStorageDirectory().getAbsolutePath();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        String last = prefs.getString("lastBrowsedDirectory", null);
        loadDirectory(last != null ? last : rootdir);
    }

    private void loadDirectory(String newDirectory) {
        if ("../".equals(newDirectory)) {
            try {
                directory = new File(directory).getParent();
            } catch (Exception e) {
                Log.e(TAG, "Error navigating up", e);
            }
        } else {
            directory = newDirectory;
        }
        if (directory == null || directory.equals("/") || directory.equals("//")) return;

        SharedPreferences.Editor editor =
                requireContext().getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putString("lastBrowsedDirectory", directory);
        editor.apply();
        directoryView.setText(directory);

        ArrayList<FileUri> filelist = new ArrayList<>();
        ArrayList<FileUri> sortedDirs = new ArrayList<>();
        ArrayList<FileUri> sortedFiles = new ArrayList<>();
        File dir = new File(directory);

        if (dir.compareTo(new File(rootdir)) != 0) {
            String parent = new File(directory).getParent() + "/";
            sortedDirs.add(new FileUri(Uri.parse("file://" + parent), "../"));
        }

        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file == null) continue;
                    String filename = file.getName().toLowerCase();
                    if (file.isDirectory()) {
                        Uri uri = Uri.parse("file://" + file.getAbsolutePath() + "/");
                        sortedDirs.add(new FileUri(uri, file.getName()));
                    } else if (filename.endsWith(".mid") || filename.endsWith(".midi") || filename.endsWith(".smf")) {
                        Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                        sortedFiles.add(new FileUri(uri, uri.getLastPathSegment()));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing directory", e);
        }

        if (!sortedDirs.isEmpty()) Collections.sort(sortedDirs, sortedDirs.get(0));
        if (!sortedFiles.isEmpty()) Collections.sort(sortedFiles, sortedFiles.get(0));
        filelist.addAll(sortedDirs);
        filelist.addAll(sortedFiles);

        IconArrayAdapter<FileUri> adapter = new IconArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, filelist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            FileUri file = adapter.getItem(position);
            if (file.isDirectory()) {
                String path = file.getUri().getPath();
                if (path != null) loadDirectory(path);
            } else {
                ChooseSongActivity.openFile(file);
            }
        });
    }
}
