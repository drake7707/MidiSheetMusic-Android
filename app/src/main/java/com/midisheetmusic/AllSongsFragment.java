package com.midisheetmusic;

import android.content.ContentResolver;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AllSongsFragment extends Fragment implements TextWatcher {

    private static final String TAG = AllSongsFragment.class.getSimpleName();

    private ArrayList<FileUri> songlist;
    private EditText filterText;
    private ListView listView;
    private IconArrayAdapter<FileUri> adapter;
    private ExecutorService executor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.choose_song, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        executor = Executors.newSingleThreadExecutor();
        listView = view.findViewById(R.id.list_view);
        filterText = view.findViewById(R.id.name_filter);
        filterText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        filterText.addTextChangedListener(this);
        loadSongs();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) executor.shutdown();
    }

    private void loadSongs() {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            ArrayList<FileUri> loaded = new ArrayList<>();
            loadAssetMidiFiles(loaded);
            loadMidiFilesFromProvider(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, loaded);
            loadMidiFilesFromProvider(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, loaded);

            if (!loaded.isEmpty()) Collections.sort(loaded, loaded.get(0));
            ArrayList<FileUri> deduplicated = new ArrayList<>();
            String prevName = "";
            for (FileUri file : loaded) {
                if (!file.toString().equals(prevName)) deduplicated.add(file);
                prevName = file.toString();
            }
            handler.post(() -> {
                if (getView() == null) return;
                songlist = deduplicated;
                updateAdapter();
            });
        });
    }

    private void updateAdapter() {
        adapter = new IconArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, songlist);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, v, position, id) -> {
            FileUri file = adapter.getItem(position);
            ChooseSongActivity.openFile(file);
        });
    }

    private void loadAssetMidiFiles(ArrayList<FileUri> list) {
        try {
            AssetManager assets = requireContext().getResources().getAssets();
            for (String path : assets.list("")) {
                if (path.endsWith(".mid")) {
                    Uri uri = Uri.parse("file:///android_asset/" + path);
                    list.add(new FileUri(uri, path));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading asset MIDI files", e);
        }
    }

    private void loadMidiFilesFromProvider(Uri contentUri, ArrayList<FileUri> list) {
        ContentResolver resolver = requireContext().getContentResolver();
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
                    list.add(new FileUri(uri, title));
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
}
