package com.midisheetmusic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.midisheetmusic.sheets.ClefSymbol;

/**
 * An activity to be shown when starting the app.
 * It handles checking for the required permissions and preloading the images.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE_EXT_STORAGE_ = 724;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadImages();
        startActivity();
    }

    /** Check for required permissions and start ChooseSongActivity */
    private void startActivity() {
        String[] required = requiredPermissions();
        if (required.length == 0) {
            goToChooseSong();
            return;
        }

        boolean allGranted = true;
        for (String perm : required) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            goToChooseSong();
        } else {
            ActivityCompat.requestPermissions(this, required, PERMISSION_REQUEST_CODE_EXT_STORAGE_);
        }
    }

    /** Returns the set of permissions needed for the running Android version. */
    private String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+): granular media permission
            return new String[]{Manifest.permission.READ_MEDIA_AUDIO};
        }
        // Android 11–12 (API 30–32): legacy storage read
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_EXT_STORAGE_) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                goToChooseSong();
            } else {
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.msg_permission_denied, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.msg_permission_denied_retry, view -> startActivity())
                        .show();
            }
        }
    }

    private void goToChooseSong() {
        Intent intent = new Intent(this, ChooseSongActivity.class);
        startActivity(intent);
        finish();
    }

    /** Load all the resource images */
    private void loadImages() {
        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);
    }

    /** Always use landscape mode for this activity. */
}
