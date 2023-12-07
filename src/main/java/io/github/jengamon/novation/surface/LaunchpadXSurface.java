package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;

import java.util.Arrays;

public class LaunchpadXSurface {
    private final CCButton mUpArrow;
    private final CCButton mDownArrow;
    private final CCButton mLeftArrow;
    private final CCButton mRightArrow;
    private final CCButton mSessionButton;
    private final CCButton mNoteButton;
    private final CCButton mCustomButton;
    private final CCButton mRecordButton;
    private final CCButton mNovationButton;
    private final AbsoluteHardwareControl mChannelPressure;
    private final Session mSession;
    private final HardwareSurface mSurface;

    private final CCButton[] mSceneButtons;

    private final NoteButton[][] mNoteButtons;

    private final Fader[] mFaders;

    //private int[] mVolumeFaderCCs = new int[]{21, 22, 23, 24, 25, 26, 27, 28};
    //private int[] mFaderCCs = new int[]{45, 46, 47, 48, 49, 50, 51, 52};

    public LaunchpadXPad up() { return mUpArrow; }
    public LaunchpadXPad down() { return mDownArrow; }
    public LaunchpadXPad left() { return  mLeftArrow; }
    public LaunchpadXPad right() { return mRightArrow; }
    public LaunchpadXPad[] arrows() { return new LaunchpadXPad[] {mUpArrow, mDownArrow, mLeftArrow, mRightArrow}; }

    public LaunchpadXPad session() { return mSessionButton; }
    public LaunchpadXPad note() { return mNoteButton; }
    public LaunchpadXPad custom() { return mCustomButton; }

    public LaunchpadXPad record() { return mRecordButton; }
    public LaunchpadXPad novation() { return mNovationButton; }

    public LaunchpadXPad[] scenes() { return mSceneButtons; }
    public NoteButton[][] notes() { return mNoteButtons; }
    public AbsoluteHardwareControl channelPressure() { return mChannelPressure; }

    public Fader[] faders() { return mFaders; }

    public LaunchpadXSurface(ControllerHost host, Session session, HardwareSurface surface) {
        mUpArrow = new CCButton(session, surface, "Up", 91, 13, 13);
        mDownArrow = new CCButton(session, surface, "Down", 92, 13 + 23, 13);
        mLeftArrow = new CCButton(session, surface, "Left", 93, 13 + 23*2, 13);
        mRightArrow = new CCButton(session, surface, "Right", 94, 13 + 23*3, 13);
        mSessionButton = new CCButton(session, surface, "Session", 95, 13 + 23*4, 13);
        mNoteButton = new CCButton(session, surface, "Note", 96, 13 + 23*5, 13);
        mCustomButton = new CCButton(session, surface, "Custom", 97, 13+23*6, 13);
        mRecordButton = new CCButton(session, surface, "Record", 98, 13 + 23*7, 13);
        mNovationButton = new CCButton(session, surface, "N", 99, 13 + 23 * 8, 13);
        mSceneButtons = new CCButton[8];

        mChannelPressure = surface.createAbsoluteHardwareKnob("Pressure");
        MidiIn in = session.midiIn(ChannelType.DAW);
        AbsoluteHardwareValueMatcher onDrumChannelPressure = in.createAbsoluteValueMatcher("status == 0xD8", "data1", 7);
        mChannelPressure.setAdjustValueMatcher(onDrumChannelPressure);

        mSession = session;
        mSurface = surface;

        for(int i = 0; i < 8; i++) {
            mSceneButtons[i] = new CCButton(session, surface, "S" + (i+1), (8 - i) * 10 + 9, 13 + 23 * 8, 13 + 23 * (1 + i));
        }

        mNoteButtons = new NoteButton[8][8];
        int[] row_offsets = new int[]{80, 70, 60, 50, 40, 30, 20, 10};
        int[] drum_pad_notes = new int[] {
            64, 65, 66, 67, 96, 97, 98, 99,
            60, 61, 62, 63, 92, 93, 94, 95,
            56, 57, 58, 59, 88, 89, 90, 91,
            52, 53, 54, 55, 84, 85, 86, 87,
            48, 49, 50, 51, 80, 81, 82, 83,
            44, 45, 46, 47, 76, 77, 78, 79,
            40, 41, 42, 43, 72, 73, 74, 75,
            36, 37, 38, 39, 68, 69, 70, 71
        };
        for(int row = 0; row < 8; row++) {
            mNoteButtons[row] = new NoteButton[8];
            for(int col = 0; col < 8; col++) {
                mNoteButtons[row][col] = new NoteButton(host, session, surface, row + "," + col, row_offsets[row] + col + 1, drum_pad_notes[row * 8 + col], 13 + (col * 23), 13 + 23 + (row * 23));
            }
        }

        mFaders = new Fader[8];
        for(int i = 0; i < 8; i++) {
            mFaders[i] = new Fader(session, surface, "FV" + i,0, i * 24);
        }
    }

    public void setupFaders(boolean vertical, boolean bipolar, int baseCC) {
        boolean[] bipolars = new boolean[8];
        Arrays.fill(bipolars, bipolar);
        setupFaders(vertical, bipolars, baseCC);
    }

    public void setupFaders(boolean vertical, boolean[] bipolar, int baseCC) {
        StringBuilder sysexString = new StringBuilder();
        sysexString.append("01 00");
        if(vertical) {
            sysexString.append("00 ");
        } else {
            sysexString.append("01 ");
        }
//        assert colors.length == 8;
        for(int i = 0; i < 8; i++) {
            int cc = baseCC + i;
            sysexString.append(Utils.toHexString((byte)i));
            if(bipolar[i]) {
                sysexString.append("01");
            } else {
                sysexString.append("00");
            }
            sysexString.append(Utils.toHexString((byte)cc));
//            sysexString.append(Utils.toHexString((byte)colors[i]));
            sysexString.append("00 ");
        }
        mSurface.invalidateHardwareOutputState();
        mSession.sendSysex(sysexString.toString());

        for(int i = 0; i < 8; i++) {
            mFaders[i].setId(baseCC + i);
        }
    }

    /**
     * Clears all color states for the surface.
     */
    public void clear() {
        mUpArrow.resetColor();
        mDownArrow.resetColor();
        mLeftArrow.resetColor();
        mRightArrow.resetColor();

        for(LaunchpadXPad scenePad : mSceneButtons) {
            scenePad.resetColor();
        }

        for(LaunchpadXPad[] noteRow : mNoteButtons) {
            for(LaunchpadXPad note : noteRow) {
                note.resetColor();
            }
        }

        for(Fader fader : mFaders) {
            fader.resetColor();
        }
    }
}
