/*
 * Copyright (c) 2007-2011 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 */

package com.midisheetmusic;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * A custom {@link Preference} that displays a single track row in the
 * "Track Options" section of {@link SettingsActivity}.
 *
 * <p>Each row shows the track title and current instrument name, and provides
 * four compact icon/text buttons for:
 * <ul>
 *   <li>Visibility (show/hide track in sheet music)</li>
 *   <li>Mute (mute/unmute track during playback)</li>
 *   <li>Instrument (open picker dialog to change the instrument)</li>
 *   <li>Octave shift (cycle: T = none, 8va = +1 octave, 8vb = −1 octave)</li>
 * </ul>
 */
public class TrackRowPreference extends Preference {

    private final int trackIndex;
    private boolean visible;
    private boolean muted;
    private int instrumentIndex;
    private int octaveShift; // 0 = none (T), 1 = 8va, -1 = 8vb

    public TrackRowPreference(Context context, int trackIndex,
                               boolean visible, boolean muted,
                               int instrumentIndex, int octaveShift) {
        super(context);
        this.trackIndex = trackIndex;
        this.visible = visible;
        this.muted = muted;
        this.instrumentIndex = instrumentIndex;
        this.octaveShift = octaveShift;

        setLayoutResource(R.layout.preference_track_row);
        setSelectable(false);
    }

    // --- Getters for SettingsActivity.updateOptions() ---

    public boolean isTrackVisible() {
        return visible;
    }

    public boolean isTrackMuted() {
        return muted;
    }

    public int getInstrumentIndex() {
        return instrumentIndex;
    }

    public int getOctaveShift() {
        return octaveShift;
    }

    // --- Bind the view ---

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView titleView = (TextView) holder.findViewById(R.id.track_title);
        TextView instrumentView = (TextView) holder.findViewById(R.id.track_instrument);
        ImageButton btnVisibility = (ImageButton) holder.findViewById(R.id.btn_visibility);
        ImageButton btnMute = (ImageButton) holder.findViewById(R.id.btn_mute);
        ImageButton btnInstrument = (ImageButton) holder.findViewById(R.id.btn_instrument);
        Button btnOctaveShift = (Button) holder.findViewById(R.id.btn_octave_shift);

        // Track title and current instrument subtitle
        titleView.setText(getContext().getString(R.string.track_number, trackIndex));
        instrumentView.setText(MidiFile.Instruments[instrumentIndex]);

        // Visibility icon
        updateVisibilityIcon(btnVisibility);
        btnVisibility.setOnClickListener(v -> {
            visible = !visible;
            updateVisibilityIcon(btnVisibility);
        });

        // Mute icon
        updateMuteIcon(btnMute);
        btnMute.setOnClickListener(v -> {
            muted = !muted;
            updateMuteIcon(btnMute);
        });

        // Instrument picker
        btnInstrument.setOnClickListener(v -> showInstrumentPicker(instrumentView));

        // Octave shift button
        updateOctaveShiftButton(btnOctaveShift);
        btnOctaveShift.setOnClickListener(v -> {
            // Cycle: 0 (T) -> 1 (8va) -> -1 (8vb) -> 0 (T)
            if (octaveShift == 0) {
                octaveShift = 1;
            } else if (octaveShift == 1) {
                octaveShift = -1;
            } else {
                octaveShift = 0;
            }
            updateOctaveShiftButton(btnOctaveShift);
        });
    }

    private void updateVisibilityIcon(ImageButton btn) {
        btn.setImageResource(visible ? R.drawable.ic_visibility : R.drawable.ic_visibility_off);
    }

    private void updateMuteIcon(ImageButton btn) {
        btn.setImageResource(muted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
    }

    private void updateOctaveShiftButton(Button btn) {
        if (octaveShift == 1) {
            btn.setText(R.string.octave_shift_8va);
        } else if (octaveShift == -1) {
            btn.setText(R.string.octave_shift_8vb);
        } else {
            btn.setText(R.string.octave_shift_none);
        }
    }

    private void showInstrumentPicker(TextView instrumentView) {
        new AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.track_instrument_picker_title, trackIndex))
                .setSingleChoiceItems(MidiFile.Instruments, instrumentIndex, (dialog, which) -> {
                    instrumentIndex = which;
                    instrumentView.setText(MidiFile.Instruments[instrumentIndex]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
