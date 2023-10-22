package com.cutiegirl;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.NoteInput;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static com.cutiegirl.Combos.clip;
import static com.cutiegirl.TEMPLATE.*;
import static com.cutiegirl.MODIFIER.*;

final class KeyPiano {
    KeyPiano(CringeScriptExtension driver, KeyHandler handler) {
        isEnabled = false;
        host = driver.getHost();
        noteInput = driver.noteInput;
        notePress[0] = true;

        updateTemplate = handler::updateTemplate;

        NoteCombo.keyPiano = this;
    }
    void pressNote(int note) {
        noteInput.sendRawMidiEvent(0x90, calcOctaveNote(note), 127);
        numPressed++;
    }
    void releaseNote(int note) {
        noteInput.sendRawMidiEvent(0x80, calcOctaveNote(note), 0);
    }
    void tapNote(int note) {
        Arrays.fill(notePress, false);
        notePress[note + 7] = true;
        numPressed--;
    }
    void retreatNote(int note) {
//        if (numPressed != 1)
            notePress[note + 7] = true;
        numPressed--;
    }
    void incOctave() {
        if (rawOctave < 10) {
            rawOctave++;
            clip.scrollKeysPageUp();
            killAllNotes();
        }
        host.showPopupNotification("Key Piano Octave: "+ (rawOctave - 2));
    }
    void decOctave() {
        if (rawOctave > 0) {
            rawOctave--;
            clip.scrollKeysPageDown();
            killAllNotes();
        }
        host.showPopupNotification("Key Piano Octave: "+ (rawOctave - 2));
    }
    void setActiveScale(int i) {
        activeScale = i - 1;
        killAllNotes();
        host.showPopupNotification("Scale: "+ (activeScale == -1 ? "NONE" : scaleNames[activeScale]));
    }
    void setTranspose(int i) {
        transpose = i%12;
    }
    void setChord(int i) {
        chord = i;
        buildChord();
    }
    void killAllNotes() {
        for (int i = 0; i < 128; i++) noteInput.sendRawMidiEvent(0x80, i, 0);
    }
    void togglePiano() {
        isEnabled = !isEnabled;
        host.showPopupNotification("Key Piano: "+ (isEnabled ? "ON" : "OFF"));
        setTemplate();
    }
    void toggleChordMode() {
        chordMode = !chordMode;
        host.showPopupNotification("Chord Mode: "+ (chordMode ? "ON" : "OFF"));
        setTemplate();
    }
    private void setTemplate() {
        if (chordMode) updateTemplate.accept(CHORD);
        else if (isEnabled) updateTemplate.accept(PIANO);
        else updateTemplate.accept(DEFAULT);
    }
    int calcRawNote(int note) {
        int rawNote = activeScale == -1 ? note : scales[activeScale][note%7] + (note/7)*12;
        return Math.min(rawNote, 127 - rawOctave*12);
    }
    int calcOctaveNote(int note) {
        return calcRawNote(note) + rawOctave*12;
    }
    int[] getRawNotes() {
        return IntStream.range(0,28)
                .map(i -> notePress[i] ? calcRawNote(i) : -1)
                .toArray();
    }
    int[] getOctaveNotes() {
        return IntStream.range(0, 28)
                .filter(num -> notePress[num])
                .map(this::calcOctaveNote)
                .toArray();
    }
    void toggleChordModifier(int i) {
        MODIFIER target = MODIFIER.values()[i];
        target.status = !target.status;
        showChordModifiers();
        buildChord();
    }
    void showChordModifiers() {
        StringBuilder str = new StringBuilder();
        for (MODIFIER value : MODIFIER.values())
            str.append(value.name()).append(": ").append(value.status).append(" ");
        host.showPopupNotification(str.toString());
    }
    private void buildChord() {
        Arrays.fill(notePress, false);
        int third = (INVERTED.status ? (chord + 2)%7 : chord + 2) + 7;
        int fifth = (INVERTED.status ? (chord + 4)%7 : chord + 4) + 7;

        notePress[chord + 7] = notePress[third] = notePress[fifth] = true;

        if (BASS.status) notePress[chord] = notePress[chord + 4] = true;
        if (SEVENTH.status) notePress[(chord + 13) < 28 ? chord + 13 : chord + 6] = true;
    }
    void playChord(int i) {
        noteInput.sendRawMidiEvent(0x90, calcOctaveNote(i), 127);
        noteInput.sendRawMidiEvent(0x90, calcOctaveNote((INVERTED.status ? (i+2)%7 : i+2)), 127);
        noteInput.sendRawMidiEvent(0x90, calcOctaveNote((INVERTED.status ? (i+4)%7 : i+4)), 127);
        if (BASS.status) {
            noteInput.sendRawMidiEvent(0x90, calcOctaveNote(i)-12, 127);
            noteInput.sendRawMidiEvent(0x90, calcOctaveNote(i+4)-12, 127);
        }
        if (SEVENTH.status) noteInput.sendRawMidiEvent(0x90, calcOctaveNote(/*(i + 13) < 28 ? i + 13 : */i + 6), 127);
    }
    void stopChord(int i) {
        noteInput.sendRawMidiEvent(0x80, calcOctaveNote(i), 0);
        noteInput.sendRawMidiEvent(0x80, calcOctaveNote((INVERTED.status ? (i+2)%7 : i+2)), 0);
        noteInput.sendRawMidiEvent(0x80, calcOctaveNote((INVERTED.status ? (i+4)%7 : i+4)), 0);
        if (BASS.status) {
            noteInput.sendRawMidiEvent(0x80, calcOctaveNote(i)-12, 0);
            noteInput.sendRawMidiEvent(0x80, calcOctaveNote(i+4)-12, 0);
        }
        if (SEVENTH.status) noteInput.sendRawMidiEvent(0x80, calcOctaveNote(/*(i + 13) < 28 ? i + 13 : */i + 6), 0);
    }
    boolean hasOctave(int o) {
        for (int i = o*7; i < (o+1)*7; i++) if (notePress[i]) return true;
        return false;
    }
    boolean[] getOctaves() {
        boolean[] octaves = new boolean[4];
        for (int i = 0; i < 4; i++) octaves[i] = hasOctave(i);
        return octaves;
    }
    boolean[] getNotePress() {
        return notePress;
    }

    private final NoteInput noteInput;
    private final boolean[] notePress = new boolean[28];
    private final String[] scaleNames = new String[] {
        "Ionian",
        "Dorian",
        "Phrygian",
        "Lydian",
        "Mixolydian",
        "Aeolian",
        "Locrian",
    };
    private final int[][] scales = new int[][]{
        {0, 2, 4, 5, 7, 9, 11},
        {0, 2, 3, 5, 7, 9, 10},
        {0, 1, 3, 5, 7, 8, 10},
        {0, 2, 4, 6, 7, 9, 11},
        {0, 2, 4, 5, 7, 9, 10},
        {0, 2, 3, 5, 7, 8, 10},
        {0, 1, 3, 5, 6, 8, 10},
    };
    private int activeScale = -1;
    private int rawOctave = 0;
    int transpose = 0;
    private int numPressed = 0;
    private int chord = 0;
    private final Consumer<TEMPLATE> updateTemplate;

    private boolean isEnabled;
    private boolean chordMode;
    private final ControllerHost host;
}

enum MODIFIER {
    BASS,
    SEVENTH,
    INVERTED;
    boolean status = false;
}