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

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a reference to a MIDI file.
 * The file may reside in the app's assets, internal storage, or external storage.
 */
public class FileUri implements Comparator<FileUri> {

    private final Uri uri;
    private final String displayName;

    /** Create a FileUri with the given URI and display path. */
    public FileUri(Uri uri, String path) {
        this.uri = uri;
        this.displayName = displayNameFromPath(path != null ? path : uri.getLastPathSegment());
    }

    /** Derive a human-readable display name from a file path. */
    public static String displayNameFromPath(String path) {
        if (path == null) return "";
        return path
                .replace("__", ": ")
                .replace("_", " ")
                .replace(".mid", "")
                .replace(".midi", "");
    }

    @Override
    public String toString() {
        return displayName;
    }

    /** Returns true if this URI represents a directory (path ends with '/'). */
    public boolean isDirectory() {
        String path = uri.getPath();
        return path != null && path.endsWith("/");
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public int compare(FileUri f1, FileUri f2) {
        return f1.displayName.compareToIgnoreCase(f2.displayName);
    }

    /**
     * Returns the file contents as a byte array, or {@code null} on any error.
     * Accepts a {@link Context} so this can be called from Activities or Fragments alike.
     */
    public byte[] getData(Context context) {
        try (InputStream stream = openInputStream(context)) {
            if (stream == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int len;
            while ((len = stream.read(chunk)) > 0) {
                buffer.write(chunk, 0, len);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** Opens an InputStream for this URI's content. Caller must close it. */
    private InputStream openInputStream(Context context) throws IOException {
        String uriString = uri.toString();
        if (uriString.startsWith("file:///android_asset/")) {
            AssetManager assets = context.getAssets();
            String assetPath = uriString.replace("file:///android_asset/", "");
            return assets.open(assetPath);
        }
        // For content:// URIs (MediaStore, FileProvider) and file:// URIs
        return context.getContentResolver().openInputStream(uri);
    }

    /** Serialises this FileUri to a JSON object for persistence. */
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("uri", uri.toString());
            json.put("displayName", displayName);
            return json;
        } catch (JSONException e) {
            return null;
        }
    }

    /** Deserialises a FileUri from a previously saved JSON object. */
    public static FileUri fromJson(JSONObject obj) {
        if (obj == null) return null;
        String displayName = obj.optString("displayName", null);
        String uriString = obj.optString("uri", null);
        if (displayName == null || uriString == null) return null;
        return new FileUri(Uri.parse(uriString), displayName);
    }

    /** Returns true if the two JSON objects represent the same FileUri. */
    public static boolean equalJson(JSONObject obj1, JSONObject obj2) {
        if (obj1 == null || obj2 == null) return obj1 == obj2;
        return Objects.equals(obj1.optString("uri", null), obj2.optString("uri", null))
                && Objects.equals(obj1.optString("displayName", null), obj2.optString("displayName", null));
    }
}


