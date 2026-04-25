/*
 * Copyright (c) 2007-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package com.midisheetmusic.sheets;

import java.util.*;
import android.graphics.*;

import com.midisheetmusic.KeySignature;
import com.midisheetmusic.MidiOptions;
import com.midisheetmusic.SheetMusic;
import com.midisheetmusic.TimeSignature;

import android.util.Log;

/* @class Staff
 * The Staff is used to draw a single Staff (a row of measures) in the 
 * SheetMusic Control. A Staff needs to draw
 * - The Clef
 * - The key signature
 * - The horizontal lines
 * - A list of MusicSymbols
 * - The left and right vertical lines
 *
 * The height of the Staff is determined by the number of pixels each
 * MusicSymbol extends above and below the staff.
 *
 * The vertical lines (left and right sides) of the staff are joined
 * with the staffs above and below it, with one exception.  
 * The last track is not joined with the first track.
 */

public class Staff {
    /** Semi-transparent red used to tint the active loop region (~20% opacity). */
    private static final int LOOP_TINT_COLOR = Color.argb(51, 255, 0, 0);

    private ArrayList<MusicSymbol> symbols;  /** The music symbols in this staff */
    private ArrayList<LyricSymbol> lyrics;   /** The lyrics to display (can be null) */
    private int ytop;                   /** The y pixel of the top of the staff */
    private ClefSymbol clefsym;         /** The left-side Clef symbol */
    private AccidSymbol[] keys;         /** The key signature symbols */
    private boolean showMeasures;       /** If true, show the measure numbers */
    private boolean showBeatMarkers;    /** If true, show small beat ticks above the staff */
    private boolean showTrackLabels;    /** If true, show track number and instrument label */
    private String trackLabel;          /** The track label text (e.g. "0: Violin") */
    private int keysigWidth;            /** The width of the clef and key signature */
    private int width;                  /** The width of the staff in pixels */
    private int height;                 /** The height of the staff in pixels */
    private int tracknum;               /** The track this staff represents */
    private int totaltracks;            /** The total number of tracks */
    private int starttime;              /** The time (in pulses) of first symbol */
    private int endtime;                /** The time (in pulses) of last symbol */
    private int measureLength;          /** The time (in pulses) of a measure */
    private int beatInterval;           /** The time (in pulses) between beats */
    private MidiOptions options;        /** The midi options (used for loop highlighting) */
    private String swingLabel;          /** Swing marker text shown above the first staff, or null */

    /** Create a new staff with the given list of music symbols,
     * and the given key signature.  The clef is determined by
     * the clef of the first chord symbol. The track number is used
     * to determine whether to join this left/right vertical sides
     * with the staffs above and below. The MidiOptions are used
     * to check whether to display measure numbers or not.
     * The originalTrackNum is the index into the original (unfiltered) MIDI
     * track list, used to look up the correct instrument name for the label.
     */
    public Staff(ArrayList<MusicSymbol> symbols, KeySignature key,
                 MidiOptions options, int tracknum, int totaltracks, int originalTrackNum)  {

        keysigWidth = SheetMusic.KeySignatureWidth(key);
        this.tracknum = tracknum;
        this.totaltracks = totaltracks;
        showMeasures = (options.showMeasures && tracknum == 0);
        showBeatMarkers = (options.showBeatMarkers && tracknum == 0);
        showTrackLabels = options.showTrackLabels;
        if (showTrackLabels && options.trackInstrumentNames != null &&
                originalTrackNum >= 0 && originalTrackNum < options.trackInstrumentNames.length) {
            String instrAbbrev = options.trackInstrumentNames[originalTrackNum];
            trackLabel = originalTrackNum + ": " + (instrAbbrev != null ? instrAbbrev : "");
        } else {
            trackLabel = null;
        }
        if (options.time != null) {
            measureLength = options.time.getMeasure();
            beatInterval = options.time.getNumerator() > 0
                    ? measureLength / options.time.getNumerator() : measureLength;
        }
        else {
            measureLength = options.defaultTime.getMeasure();
            beatInterval = options.defaultTime.getNumerator() > 0
                    ? measureLength / options.defaultTime.getNumerator() : measureLength;
        }
        Clef clef = FindClef(symbols);

        clefsym = new ClefSymbol(clef, 0, false);
        keys = key.GetSymbols(clef);
        this.symbols = symbols;
        this.options = options;
        CalculateWidth(options.scrollVert);
        CalculateHeight();
        CalculateStartEndTime();
        FullJustify();
    }

    /** Return the width of the staff */
    public int getWidth() { return width; }

    /** Return the height of the staff */
    public int getHeight() { return height; }

    /** Return the track number of this staff (starting from 0 */
    public int getTrack() { return tracknum; }

    /** Return the starting time of the staff, the start time of
     *  the first symbol.  This is used during playback, to 
     *  automatically scroll the music while playing.
     */
    public int getStartTime() { return starttime; }

    /** Set the swing-feel label shown above this staff (e.g. "Swing" or "Swing (16ths)").
     *  Pass null to clear. CalculateHeight() must be called afterwards to update spacing.
     */
    public void setSwingLabel(String label) { swingLabel = label; }

    /** Return the ending time of the staff, the endtime of
     *  the last symbol.  This is used during playback, to 
     *  automatically scroll the music while playing.
     */
    public int getEndTime() { return endtime; }
    public void setEndTime(int value) { endtime = value; }

    /** Find the initial clef to use for this staff.  Use the clef of
     * the first ChordSymbol.
     */
    private Clef FindClef(ArrayList<MusicSymbol> list) {
        for (MusicSymbol m : list) {
            if (m instanceof ChordSymbol) {
                ChordSymbol c = (ChordSymbol) m;
                return c.getClef();
            }
        }
        return Clef.Treble;
    }

    /** Calculate the height of this staff.  Each MusicSymbol contains the
     * number of pixels it needs above and below the staff.  Get the maximum
     * values above and below the staff.
     */
    public void CalculateHeight() {
        int above = 0;
        int below = 0;

        for (MusicSymbol s : symbols) {
            above = Math.max(above, s.getAboveStaff());
            below = Math.max(below, s.getBelowStaff());
        }
        above = Math.max(above, clefsym.getAboveStaff());
        below = Math.max(below, clefsym.getBelowStaff());
        if (showMeasures || swingLabel != null) {
            above = Math.max(above, SheetMusic.NoteHeight * 3);
        }
        if (showBeatMarkers && !showMeasures) {
            above = Math.max(above, SheetMusic.NoteHeight * 2);
        }
        if (showTrackLabels) {
            above = Math.max(above, SheetMusic.NoteHeight * 2);
        }
        /* When both a swing label and track labels are active they would overlap
         * at NoteHeight*2 and NoteHeight respectively.  Reserve an extra row so
         * the swing label can sit one row higher than the track label. */
        if (swingLabel != null && showTrackLabels) {
            above = Math.max(above, SheetMusic.NoteHeight * 4);
        }
        ytop = above + SheetMusic.NoteHeight;
        height = SheetMusic.NoteHeight*5 + ytop + below;
        if (lyrics != null) {
            height += SheetMusic.NoteHeight * 3/2;
        }

        /* Add some extra vertical space between the last track
         * and first track.
         */
        if (tracknum == totaltracks-1)
            height += SheetMusic.NoteHeight * 3;
    }

    /** Calculate the width of this staff */
    private void CalculateWidth(boolean scrollVert) {
        if (scrollVert) {
            width = SheetMusic.PageWidth;
            return;
        }
        width = keysigWidth;
        for (MusicSymbol s : symbols) {
            width += s.getWidth();
        }
    }

    /** Calculate the start and end time of this staff. */
    private void CalculateStartEndTime() {
        starttime = endtime = 0;
        if (symbols.size() == 0) {
            return;
        }
        starttime = symbols.get(0).getStartTime();
        for (MusicSymbol m : symbols) {
            if (endtime < m.getStartTime()) {
                endtime = m.getStartTime();
            }
            if (m instanceof ChordSymbol) {
                ChordSymbol c = (ChordSymbol) m;
                if (endtime < c.getEndTime()) {
                    endtime = c.getEndTime();
                }
            }
        }
    }


    /** Full-Justify the symbols, so that they expand to fill the whole staff. */
    private void FullJustify() {
        if (width != SheetMusic.PageWidth)
            return;

        int totalwidth = keysigWidth;
        int totalsymbols = 0;
        int i = 0;

        while (i < symbols.size()) {
            int start = symbols.get(i).getStartTime();
            totalsymbols++;
            totalwidth += symbols.get(i).getWidth();
            i++;
            while (i < symbols.size() && symbols.get(i).getStartTime() == start) {
                totalwidth += symbols.get(i).getWidth();
                i++;
            }
        }

        int extrawidth = (SheetMusic.PageWidth - totalwidth - 1) / totalsymbols;
        if (extrawidth > SheetMusic.NoteHeight*2) {
            extrawidth = SheetMusic.NoteHeight*2;
        }
        i = 0;
        while (i < symbols.size()) {
            int start = symbols.get(i).getStartTime();
            int newwidth = symbols.get(i).getWidth() + extrawidth;
            symbols.get(i).setWidth(newwidth);
            i++;
            while (i < symbols.size() && symbols.get(i).getStartTime() == start) {
                i++;
            }
        }
    }


    /** Add the lyric symbols that occur within this staff.
     *  Set the x-position of the lyric symbol.
     */
    public void AddLyrics(ArrayList<LyricSymbol> tracklyrics) {
        if (tracklyrics == null || tracklyrics.size() == 0) {
            return;
        }
        lyrics = new ArrayList<LyricSymbol>();
        int xpos = 0;
        int symbolindex = 0;
        for (LyricSymbol lyric : tracklyrics) {
            if (lyric.getStartTime() < starttime) {
                continue;
            }
            if (lyric.getStartTime() > endtime) {
                break;
            }
            /* Get the x-position of this lyric */
            while (symbolindex < symbols.size() &&
                   symbols.get(symbolindex).getStartTime() < lyric.getStartTime()) {
                xpos += symbols.get(symbolindex).getWidth();
                symbolindex++;
            }
            lyric.setX(xpos);
            if (symbolindex < symbols.size() &&
                (symbols.get(symbolindex) instanceof BarSymbol)) {
                lyric.setX(lyric.getX() + SheetMusic.NoteWidth);
            }
            lyrics.add(lyric);
        }
        if (lyrics.size() == 0) {
            lyrics = null;
        }
    }

    /** Draw the lyrics */
    private void DrawLyrics(Canvas canvas, Paint paint) {
        /* Skip the left side Clef symbol and key signature */
        int xpos = keysigWidth;
        int ypos = height - SheetMusic.NoteHeight * 3/2;

        for (LyricSymbol lyric : lyrics) {
            canvas.drawText(lyric.getText(),
                            xpos + lyric.getX(),
                            ypos,
                            paint);
        }
    }


    /** Draw the track label (track number and instrument name) above the clef */
    private void DrawTrackLabel(Canvas canvas, Paint paint) {
        if (trackLabel == null) {
            return;
        }
        float savedTextSize = paint.getTextSize();
        paint.setTextSize(savedTextSize * 0.75f);
        int ypos = ytop - SheetMusic.NoteHeight;
        canvas.drawText(trackLabel, SheetMusic.LeftMargin + 2 /* small inset from margin */, ypos, paint);
        paint.setTextSize(savedTextSize);
    }


    /** Draw the measure numbers for each measure */
    private void DrawMeasureNumbers(Canvas canvas, Paint paint) {
        /* Skip the left side Clef symbol and key signature */
        int xpos = keysigWidth;
        int ypos = ytop - SheetMusic.NoteHeight * 3;

        for (MusicSymbol s : symbols) {
            if (s instanceof BarSymbol) {
                int measure = 1 + s.getStartTime() / measureLength;
                canvas.drawText("" + measure,
                                xpos + SheetMusic.NoteWidth/2,
                                ypos,
                                paint);
            }
            xpos += s.getWidth();
        }
    }


    /** Draw small beat-marker ticks above the staff for every beat in each measure,
     *  including beat 1 (the downbeat).  The tick for each beat is positioned at the
     *  x-coordinate of the note/rest that starts at that beat time; if no symbol starts
     *  exactly at that time (e.g. a half note spans beats 1–2), the position is
     *  interpolated between the surrounding symbols so the tick still falls in the
     *  right horizontal region.  Ticks are drawn in gray to keep visual clutter low.
     *  When measure numbers are also shown they occupy ytop - NoteHeight*3; the beat
     *  ticks sit one row lower at ytop - NoteHeight*2 so the two annotations never
     *  overlap.
     *
     *  This method must be called BEFORE symbols (notes/rests) are drawn so that
     *  the ticks are rendered underneath the notation.
     */
    private void DrawBeatMarkers(Canvas canvas, Paint paint) {
        if (beatInterval <= 0 || measureLength <= 0) return;

        /* Build lookup arrays from note/rest symbols only — BarSymbols are skipped
         * because a BarSymbol shares its startTime with the first note of the next
         * measure but sits physically to the left of it; including bars would cause
         * beat-1 ticks to land on (or before) the bar line instead of on the note.
         * We store the horizontal centre of each note/rest head so ticks are visually
         * centred on the note rather than at the left edge of its slot. */
        int noteCount = 0;
        for (MusicSymbol s : symbols) {
            if (s instanceof ChordSymbol || s instanceof RestSymbol) noteCount++;
        }
        if (noteCount == 0) return;

        int[] noteTimes   = new int[noteCount];
        int[] noteCenterX = new int[noteCount];
        int xpos = keysigWidth;
        int idx = 0;
        for (MusicSymbol s : symbols) {
            if (s instanceof ChordSymbol) {
                ChordSymbol chord = (ChordSymbol) s;
                noteTimes[idx]   = s.getStartTime();
                noteCenterX[idx] = xpos + chord.getNoteXLeft() + SheetMusic.NoteWidth / 2;
                idx++;
            } else if (s instanceof RestSymbol) {
                noteTimes[idx]   = s.getStartTime();
                noteCenterX[idx] = xpos + s.getWidth() / 2;
                idx++;
            }
            xpos += s.getWidth();
        }
        int staffEndX = xpos;

        int savedColor = paint.getColor();
        float savedStroke = paint.getStrokeWidth();

        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(1);

        /* Beat ticks sit one NoteHeight above the staff top (below measure numbers) */
        int tickTop    = ytop - SheetMusic.NoteHeight * 2;
        int tickBottom = tickTop + SheetMusic.NoteHeight * 3 / 4;

        /* Iterate over every beat whose time falls within this staff. */
        int firstBeat = (starttime / beatInterval) * beatInterval;
        if (firstBeat < starttime) firstBeat += beatInterval;

        for (int beatTime = firstBeat; beatTime <= endtime; beatTime += beatInterval) {
            int beatX = xposForTime(noteTimes, noteCenterX, noteCount, staffEndX, beatTime);
            canvas.drawLine(beatX, tickTop, beatX, tickBottom, paint);
        }

        paint.setColor(savedColor);
        paint.setStrokeWidth(savedStroke);
    }

    /** Return the x-pixel position (centre of note head) corresponding to the given pulse time.
     *  If a note/rest starts exactly at {@code time} its centre is returned.
     *  Otherwise the position is linearly interpolated between the centres of the two
     *  surrounding notes, giving a proportional position when a beat falls inside a long note.
     */
    private int xposForTime(int[] times, int[] xpos, int n, int endX, int time) {
        for (int i = 0; i < n; i++) {
            if (times[i] == time) {
                return xpos[i];
            }
            if (times[i] > time) {
                if (i == 0) return xpos[0];
                int t0 = times[i - 1], x0 = xpos[i - 1];
                int t1 = times[i],     x1 = xpos[i];
                if (t1 == t0) return x0;
                return x0 + Math.round((float)(time - t0) / (t1 - t0) * (x1 - x0));
            }
        }
        return endX;
    }


    /** Draw a semi-transparent red background rectangle over the measures that fall
     *  within the configured loop range, when loop is enabled.  Drawing this first
     *  ensures all notes and staff lines are rendered on top of the tint.
     */
    private void DrawLoopHighlight(Canvas canvas, Paint paint) {
        if (!options.playMeasuresInLoop) return;

        int loopStartTime = options.playMeasuresInLoopStart * measureLength;
        int loopEndTime   = (options.playMeasuresInLoopEnd + 1) * measureLength;

        /* Quick check: does the loop range overlap this staff at all? */
        if (loopEndTime <= starttime || loopStartTime > endtime) return;

        /* Scan symbols to find the pixel x-positions of the loop start and end. */
        int xpos = keysigWidth;
        int loopStartX = -1;
        int loopEndX   = -1;
        for (MusicSymbol s : symbols) {
            if (s instanceof BarSymbol) {
                int t = s.getStartTime();
                if (t == loopStartTime) loopStartX = xpos + SheetMusic.NoteWidth / 2;
                if (t == loopEndTime)   loopEndX   = xpos + SheetMusic.NoteWidth / 2;
            }
            xpos += s.getWidth();
        }

        /* Fallback: loop starts before (or at) this staff → use the staff's left edge */
        if (loopStartX < 0 && loopStartTime <= starttime) {
            loopStartX = SheetMusic.LeftMargin;
        }
        /* Fallback: loop ends beyond this staff → use the staff's right edge */
        if (loopEndX < 0 && loopEndTime >= endtime) {
            loopEndX = width - 1;
        }

        if (loopStartX < 0 || loopEndX < 0) return;

        int line = 1;



        /* Vertical extent matches DrawEndLines */
        int ystart = ytop - SheetMusic.LineWidth; //(tracknum == 0) ? ytop - SheetMusic.LineWidth : 0;
        int yend = ystart + SheetMusic.StaffHeight;
       // int yend   = (tracknum == totaltracks - 1) ? ytop + 4 * SheetMusic.NoteHeight : height;

        Paint.Style savedStyle = paint.getStyle();
        int savedColor = paint.getColor();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(LOOP_TINT_COLOR);  /* red at ~20% opacity */
        canvas.drawRect(loopStartX, ystart, loopEndX, yend, paint);

        paint.setStyle(savedStyle);
        paint.setColor(savedColor);
    }

    /** Return true if the given pulse time falls within the active loop region. */
    private boolean isWithinLoopRegion(int pulseTime) {
        if (!options.playMeasuresInLoop) return false;
        int loopStartTime = options.playMeasuresInLoopStart * measureLength;
        int loopEndTime   = (options.playMeasuresInLoopEnd + 1) * measureLength;
        return pulseTime >= loopStartTime && pulseTime < loopEndTime;
    }

    /** Draw the five horizontal lines of the staff */
    private void DrawHorizLines(Canvas canvas, Paint paint) {
        int line = 1;
        int y = ytop - SheetMusic.LineWidth;
        paint.setStrokeWidth(1);
        for (line = 1; line <= 5; line++) {
            canvas.drawLine(SheetMusic.LeftMargin, y, width-1, y, paint);
            y += SheetMusic.LineWidth + SheetMusic.LineSpace;
        }

    }

    /** Draw the vertical lines at the far left and far right sides. */
    private void DrawEndLines(Canvas canvas, Paint paint) {
        paint.setStrokeWidth(1);

        /* Draw the vertical lines from 0 to the height of this staff,
         * including the space above and below the staff, with two exceptions:
         * - If this is the first track, don't start above the staff.
         *   Start exactly at the top of the staff (ytop - LineWidth)
         * - If this is the last track, don't end below the staff.
         *   End exactly at the bottom of the staff.
         */
        int ystart, yend;
        if (tracknum == 0)
            ystart = ytop - SheetMusic.LineWidth;
        else
            ystart = 0;

        if (tracknum == (totaltracks-1))
            yend = ytop + 4 * SheetMusic.NoteHeight;
        else
            yend = height;

        canvas.drawLine(SheetMusic.LeftMargin, ystart, SheetMusic.LeftMargin, yend, paint);

        canvas.drawLine(width-1, ystart, width-1, yend, paint);

    }

    /** Draw this staff. Only draw the symbols inside the clip area */
    public void Draw(Canvas canvas, Rect clip, Paint paint) {
        paint.setColor(Color.BLACK);

        /* Draw the semi-transparent loop region tint first so everything renders on top */
        DrawLoopHighlight(canvas, paint);

        /* Draw beat markers before any notation so notes/rests paint over the ticks */
        if (showBeatMarkers) {
            DrawBeatMarkers(canvas, paint);
        }

        int xpos = SheetMusic.LeftMargin + 5;

        /* Draw the left side Clef symbol */
        canvas.translate(xpos, 0);
        clefsym.Draw(canvas, paint, ytop);
        canvas.translate(-xpos, 0);
        xpos += clefsym.getWidth();

        /* Draw the key signature */
        for (AccidSymbol a : keys) {
            canvas.translate(xpos, 0);
            a.Draw(canvas, paint, ytop);
            canvas.translate(-xpos, 0);
            xpos += a.getWidth();
        }
       
        /* Draw the actual notes, rests, bars.  Draw the symbols one 
         * after another, using the symbol width to determine the
         * x position of the next symbol.
         *
         * For fast performance, only draw symbols that are in the clip area.
         */
        for (MusicSymbol s : symbols) {
            if ((xpos <= clip.left + clip.width() + 50) && (xpos + s.getWidth() + 50 >= clip.left)) {
                canvas.translate(xpos, 0);
                s.Draw(canvas, paint, ytop);
                canvas.translate(-xpos, 0);
            }
            xpos += s.getWidth();
        }
        paint.setColor(Color.BLACK);
        DrawHorizLines(canvas, paint);
        DrawEndLines(canvas, paint);

        if (showMeasures) {
            DrawMeasureNumbers(canvas, paint);
        }
        if (showTrackLabels) {
            DrawTrackLabel(canvas, paint);
        }
        if (swingLabel != null) {
            DrawSwingMarker(canvas, paint);
        }
        if (lyrics != null) {
            DrawLyrics(canvas, paint);
        }
        DrawTieArcs(canvas, paint);
    }

    /** Draw the swing-feel marker ("Swing" or "Swing (16ths)") above the top-left
     *  of the staff, at the same vertical level as measure numbers.
     *  When track labels are also shown it moves one row higher to avoid overlap.
     */
    private void DrawSwingMarker(Canvas canvas, Paint paint) {
        float savedSize = paint.getTextSize();
        paint.setTextSize(savedSize * 1.1f);
        paint.setStyle(Paint.Style.FILL);
        /* If track labels are visible they occupy ytop - NoteHeight.  Push the
         * swing marker one row further up so the two texts don't collide. */
        int ySwing = showTrackLabels
                ? ytop - SheetMusic.NoteHeight * 3
                : ytop - SheetMusic.NoteHeight * 2;
        canvas.drawText(swingLabel,
                        SheetMusic.LeftMargin,
                        ySwing,
                        paint);
        paint.setTextSize(savedSize);
    }

    /** Draw tie arcs for all tied chords in this staff.
     *
     *  A tie is a small curved arc that connects a note head in one chord to the
     *  same-pitch note head in the immediately following chord.  It curves away from
     *  the stem: below the note head when the stem points up, above when down.
     *
     *  Two kinds of arcs are handled:
     *
     *  (a) Full arc: both the source chord (hasTie) and its partner (isTiedToPrev) are
     *      in this same staff row.  A symmetric cubic Bézier is drawn between the
     *      right edge of the source note head and the left edge of the partner note head.
     *
     *  (b) Outgoing half-arc: the source chord is in this staff but the partner is on
     *      the next staff row.  A tapering quadratic arc is drawn from the note head to
     *      the right margin.
     *
     *  (c) Incoming half-arc: the source chord was on the *previous* staff row and the
     *      continuation chord (isTiedToPrev) is in this staff.  A mirrored tapering
     *      quadratic arc is drawn from the left margin to the note head.
     */
    private void DrawTieArcs(Canvas canvas, Paint paint) {
        /* Collect the x-position of every symbol so we can look ahead without
         * re-traversing the list. */
        int[] xpos = new int[symbols.size()];
        int x = keysigWidth;
        for (int i = 0; i < symbols.size(); i++) {
            xpos[i] = x;
            x += symbols.get(i).getWidth();
        }

        Paint.Style savedStyle = paint.getStyle();
        float savedStroke = paint.getStrokeWidth();
        int savedColor = paint.getColor();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.BLACK);

        for (int i = 0; i < symbols.size(); i++) {
            if (!(symbols.get(i) instanceof ChordSymbol)) continue;
            ChordSymbol chord = (ChordSymbol) symbols.get(i);

            /* ---- (a)/(b) Outgoing: this chord has notes tied forward ---- */
            if (chord.hasTie()) {
                /* Find the paired (tiedToPrev) chord in this same staff. */
                int partnerIdx = -1;
                for (int j = i + 1; j < symbols.size(); j++) {
                    if (symbols.get(j) instanceof ChordSymbol) {
                        ChordSymbol next = (ChordSymbol) symbols.get(j);
                        if (next.isTiedToPrev()) {
                            partnerIdx = j;
                        }
                        break; /* stop at the first ChordSymbol regardless */
                    }
                }

                WhiteNote topstaff = WhiteNote.Top(chord.getClef());
                boolean tieBelow = (chord.getStem() == null ||
                                    chord.getStem().getDirection() == com.midisheetmusic.sheets.Stem.Up);

                /* x offset to the right edge of this chord's note head */
                int x1 = xpos[i] + chord.getNoteXRight();

                for (WhiteNote wn : chord.getTiedNotes()) {
                    /* Centre of note head y.  DrawNotes translates the canvas to:
                     *   (xnote + NoteWidth/2 + 1,  ynote - LineWidth + NoteHeight/2)
                     * where ynote = ytop + topstaff.Dist(wn) * NoteHeight/2.
                     * So the visual centre y = ytop + Dist * NoteHeight/2
                     *                                  - LineWidth + NoteHeight/2. */
                    int ynote = ytop + topstaff.Dist(wn) * SheetMusic.NoteHeight / 2
                                - SheetMusic.LineWidth + SheetMusic.NoteHeight / 2;

                    int x2;
                    boolean halfArc;
                    if (partnerIdx >= 0) {
                        /* Partner in same staff: full symmetric arc. */
                        ChordSymbol partner = (ChordSymbol) symbols.get(partnerIdx);
                        x2 = xpos[partnerIdx] + partner.getNoteXLeft();
                        halfArc = false;
                    } else {
                        /* Partner is on the next staff row — outgoing half-arc to right margin. */
                        x2 = width - SheetMusic.NoteWidth;
                        halfArc = true;
                    }

                    drawTieArc(canvas, paint, x1, x2, ynote, tieBelow, halfArc);
                }
            }

            /* ---- (c) Incoming: this chord is a continuation whose source is on
             *         the previous staff row ---- */
            if (chord.isTiedToPrev() && chord.getTiedFromPrevNotes() != null) {
                /* For each incoming tied note, check whether any earlier chord in THIS
                 * staff is the source (i.e., has that white note in its tiedNotes list).
                 * If no same-staff source exists the source chord is on the previous row
                 * and we draw an incoming half-arc from the left margin.
                 * Note: getTiedFromPrevNotes() returns at most one entry per pitch (1–4
                 * typically), so copying the list here is negligible. */
                ArrayList<WhiteNote> unresolved = new ArrayList<>(chord.getTiedFromPrevNotes());
                for (int j = 0; j < i && !unresolved.isEmpty(); j++) {
                    if (symbols.get(j) instanceof ChordSymbol) {
                        ChordSymbol prev = (ChordSymbol) symbols.get(j);
                        if (prev.hasTie() && prev.getTiedNotes() != null) {
                            /* WhiteNote does not override equals(), so removeAll() would
                             * use reference equality and never match.  Use Dist()==0 to
                             * compare by value (same letter and octave). */
                            for (WhiteNote prevWn : prev.getTiedNotes()) {
                                unresolved.removeIf(wn -> wn.Dist(prevWn) == 0);
                            }
                        }
                    }
                }

                if (!unresolved.isEmpty()) {
                    WhiteNote topstaff = WhiteNote.Top(chord.getClef());
                    boolean tieBelow = (chord.getStem() == null ||
                                        chord.getStem().getDirection() == com.midisheetmusic.sheets.Stem.Up);
                    /* x offset to the left edge of this chord's note head */
                    int x2 = xpos[i] + chord.getNoteXLeft();

                    for (WhiteNote wn : unresolved) {
                        int ynote = ytop + topstaff.Dist(wn) * SheetMusic.NoteHeight / 2
                                    - SheetMusic.LineWidth + SheetMusic.NoteHeight / 2;
                        drawIncomingHalfArc(canvas, paint, keysigWidth, x2, ynote, tieBelow);
                    }
                }
            }
        }

        paint.setStyle(savedStyle);
        paint.setStrokeWidth(savedStroke);
        paint.setColor(savedColor);
    }

    /** Draw a single outgoing bezier tie arc from (x1, y) to (x2, y).
     *  @param tieBelow  True → arc bows downward; false → arc bows upward.
     *  @param halfArc   True → taper the right end (outgoing cross-row tie).
     */
    private static void drawTieArc(Canvas canvas, Paint paint,
                                    int x1, int x2, int y,
                                    boolean tieBelow, boolean halfArc) {
        int span = x2 - x1;
        if (span <= 0) return;

        /* Bow height: clamp between 4 and 14 px, proportional to span. */
        int bow = Math.max(4, Math.min(14, span / 5));
        if (!tieBelow) bow = -bow;

        Path path = new Path();
        path.moveTo(x1, y);
        if (halfArc) {
            /* Quadratic arc tapering to half-height at the right margin */
            path.quadTo(x1 + span * 0.6f, y + bow, x2, y + bow / 2.0f);
        } else {
            /* Symmetric cubic bezier */
            path.cubicTo(x1 + span / 3.0f, y + bow,
                         x2 - span / 3.0f, y + bow,
                         x2, y);
        }
        canvas.drawPath(path, paint);
    }

    /** Draw an incoming half-arc from the left margin to an arriving note head.
     *  This mirrors the outgoing half-arc shape so the two halves look symmetrical
     *  when viewed across the row break.
     *  @param tieBelow  True → arc bows downward; false → arc bows upward.
     */
    private static void drawIncomingHalfArc(Canvas canvas, Paint paint,
                                             int x1, int x2, int y,
                                             boolean tieBelow) {
        int span = x2 - x1;
        if (span <= 0) return;

        int bow = Math.max(4, Math.min(14, span / 5));
        if (!tieBelow) bow = -bow;

        /* Start at half-height (matching the outgoing arc's right end),
         * peak near the destination, then land on the note head centre. */
        Path path = new Path();
        path.moveTo(x1, y + bow / 2.0f);
        path.quadTo(x1 + span * 0.4f, y + bow, x2, y);
        canvas.drawPath(path, paint);
    }


    public MusicSymbol getCurrentNote(int currentPulseTime, TimeSignature sig) {
        for (int i = 0; i < symbols.size(); ++i) {
            MusicSymbol cur = symbols.get(i);
            if (cur instanceof ChordSymbol || cur instanceof RestSymbol) {
                if (cur.getStartTime() >= currentPulseTime) {
                    int endTime = cur instanceof ChordSymbol ? ((ChordSymbol) cur).getEndTime() : ((RestSymbol) cur).getEndTime(sig);
                    if(currentPulseTime > endTime) {
                        // it's after the end time of this note
                        // find the next chord or rest symbol
                        for(int j = i; j < symbols.size(); j++) {
                            if(symbols.get(j) instanceof ChordSymbol || symbols.get(j) instanceof  RestSymbol)
                                return symbols.get(j);
                        }
                    }
                    else
                        return cur;
                }
            }
        }
        return null;
    }

    /** Return the last ChordSymbol whose start time is strictly before currentPulseTime,
     *  or null if no such chord exists.
     */
    public MusicSymbol getPrevNote(int currentPulseTime) {
        MusicSymbol result = null;
        for (int i = 0; i < symbols.size(); ++i) {
            MusicSymbol cur = symbols.get(i);
            if (cur instanceof ChordSymbol || cur instanceof RestSymbol) {
                if (cur.getStartTime() < currentPulseTime) {
                    result = cur;
                } else {
                    break;
                }
            }
        }
        return result;
    }

    /** Shade all the chords played in the given time.
     *  Un-shade any chords shaded in the previous pulse time.
     *  Store the x coordinate location where the shade was drawn.
     */
    public int ShadeNotes(Canvas canvas, Paint paint, int shade,
                           int currentPulseTime, int prevPulseTime, int x_shade) {

        /* If there's nothing to unshade, or shade, return */
        if ((starttime > prevPulseTime || endtime < prevPulseTime) &&
            (starttime > currentPulseTime || endtime < currentPulseTime)) {
            return x_shade;
        }

        /* Skip the left side Clef symbol and key signature */
        int xpos = keysigWidth;

        MusicSymbol curr = null;
        ChordSymbol prevChord = null;
        int prev_xpos = 0;

        /* Loop through the symbols. 
         * Unshade symbols where start <= prevPulseTime < end
         * Shade symbols where start <= currentPulseTime < end
         */ 
        for (int i = 0; i < symbols.size(); i++) {
            curr = symbols.get(i);
            if (curr instanceof BarSymbol) {
                xpos += curr.getWidth();
                continue;
            }

            int start = curr.getStartTime();
            int end = 0;

            if (i+2 < symbols.size() && symbols.get(i+1) instanceof BarSymbol) {
                end = symbols.get(i+2).getStartTime();
            }
            else if (i+1 < symbols.size()) {
                end = symbols.get(i+1).getStartTime();
            }
            else {
                end = endtime;
            }


            /* If we've past the previous and current times, we're done. */
            if (((start > prevPulseTime) && (start > currentPulseTime))) {
                if (x_shade == 0) {
                    x_shade = xpos;
                }
                return x_shade;
            }
            /* If shaded notes are the same, we're done */
            if ((start <= currentPulseTime) && (currentPulseTime < end) &&
                (start <= prevPulseTime) && (prevPulseTime < end)) {

                x_shade = xpos;
                return x_shade;
            }

            boolean redrawLines = false;

            /* If symbol is in the previous time, draw a white background */
            if ((start <= prevPulseTime) && (prevPulseTime < end)) {
                canvas.translate(xpos-2, -2);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                canvas.drawRect(0, 0, curr.getWidth()+4, this.getHeight()+4, paint);
                /* Restore loop tint if this note falls within the loop region.
                 * Use the same vertical bounds as DrawLoopHighlight (staff bar height),
                 * adjusted for the active canvas translation of (xpos-2, -2). */
                if (isWithinLoopRegion(start)) {
                    int ystart = ytop - SheetMusic.LineWidth; //(tracknum == 0) ? ytop - SheetMusic.LineWidth : 0;
                    int yend = ystart + SheetMusic.StaffHeight;
                    //int ystart = (tracknum == 0) ? ytop - SheetMusic.LineWidth : 0;
                    //int yend   = (tracknum == totaltracks - 1) ? ytop + 4 * SheetMusic.NoteHeight : height;
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(LOOP_TINT_COLOR);
                    canvas.drawRect(0, ystart + 2, curr.getWidth()+4, yend + 2, paint);
                }
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                canvas.translate(-(xpos-2), 2);
                canvas.translate(xpos, 0);
                curr.Draw(canvas, paint, ytop);
                canvas.translate(-xpos, 0);

                redrawLines = true;
            }

            /* If symbol is in the current time, draw a shaded background */
            if (((start <= currentPulseTime) && (currentPulseTime < end)) ) {
                x_shade = xpos;
                canvas.translate(xpos, 0);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(shade);
                canvas.drawRect(0, 0, curr.getWidth(), this.getHeight(), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                curr.Draw(canvas, paint, ytop);
                canvas.translate(-xpos, 0);
                redrawLines = true;
            }

            /* If either a gray or white background was drawn, we need to redraw
             * the horizontal staff lines, and redraw the stem of the previous chord.
             */
            if (redrawLines) {
                int line = 1;
                int y = ytop - SheetMusic.LineWidth;
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth(1);
                canvas.translate(xpos-2, 0);
                for (line = 1; line <= 5; line++) {
                    canvas.drawLine(0, y, curr.getWidth()+4, y, paint);
                    y += SheetMusic.LineWidth + SheetMusic.LineSpace;
                }
                canvas.translate(-(xpos-2), 0);

                /* Redraw beat markers before the chord so notes paint over the ticks */
                if (showBeatMarkers) {
                    DrawBeatMarkers(canvas, paint);
                }
                if (prevChord != null) {
                    canvas.translate(prev_xpos, 0);
                    prevChord.Draw(canvas, paint, ytop);
                    canvas.translate(-prev_xpos, 0);
                }
                if (showMeasures) {
                    DrawMeasureNumbers(canvas, paint);
                }
                if (lyrics != null) {
                    DrawLyrics(canvas, paint);
                }
                DrawTieArcs(canvas, paint);
            }
            if (curr instanceof ChordSymbol) {
                ChordSymbol chord = (ChordSymbol) curr;
                if (chord.getStem() != null && !chord.getStem().getReceiver()) {
                    prevChord = (ChordSymbol) curr;
                    prev_xpos = xpos;
                }
            }
            xpos += curr.getWidth();
        }
        return x_shade;
    }

    /** Return the pulse time corresponding to the given point.
     *  Find the notes/symbols corresponding to the x position,
     *  and return the startTime (pulseTime) of the symbol.
     */
    public int PulseTimeForPoint(Point point) {

        int xpos = keysigWidth;
        int pulseTime = starttime;
        for (MusicSymbol sym : symbols) {
            pulseTime = sym.getStartTime();
            if (point.x <= xpos + sym.getWidth()) {
                return pulseTime;
            }
            xpos += sym.getWidth();
        }
        return pulseTime;
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Staff clef=" + clefsym.toString() + "\n");
        result.append("  Keys:\n");
        for (AccidSymbol a : keys) {
            result.append("    ").append(a.toString()).append("\n");
        }
        result.append("  Symbols:\n");
        for (MusicSymbol s : keys) {
            result.append("    ").append(s.toString()).append("\n");
        }
        for (MusicSymbol m : symbols) {
            result.append("    ").append(m.toString()).append("\n");
        }
        result.append("End Staff\n");
        return result.toString();
    }

}


