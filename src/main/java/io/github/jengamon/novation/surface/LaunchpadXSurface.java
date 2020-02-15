package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.modes.session.SessionPadMode;

public class LaunchpadXSurface {
    private CCButton mUpArrow;
    private CCButton mDownArrow;
    private CCButton mLeftArrow;
    private CCButton mRightArrow;
    private CCButton mSessionButton;
    private CCButton mNoteButton;
    // Ignore the "Custom" Button
    private CCButton mRecordButton;
    private CCButton mNovationButton;
    private Session mSession;
    private HardwareSurface mSurface;

    private CCButton[] mSceneButtons;

    private NoteButton[][] mNoteButtons;

    private Fader[] mVolumeFaders;
    private Fader[] mPanFaders;
    private Fader[] mSendFaders;
    private Fader[] mControlFaders;

    private int[] mVolumeFaderCCs = new int[]{21, 22, 23, 24, 25, 26, 27, 28};
    private int[] mPanFaderCCs = new int[]{29, 30, 31, 32, 33, 34, 35, 36};
    private int[] mSendFaderCCs = new int[]{37, 38, 39, 40, 41, 42, 43, 44};
    private int[] mControlFaderCCs = new int[]{45, 46, 47, 48, 49, 50, 51, 52};

    public LaunchpadXPad up() { return mUpArrow; }
    public LaunchpadXPad down() { return mDownArrow; }
    public LaunchpadXPad left() { return  mLeftArrow; }
    public LaunchpadXPad right() { return mRightArrow; }

    public LaunchpadXPad session() { return mSessionButton; }
    public LaunchpadXPad note() { return mNoteButton; }

    public LaunchpadXPad record() { return mRecordButton; }
    public LaunchpadXPad novation() { return mNovationButton; }

    public LaunchpadXPad[] scenes() { return mSceneButtons; }
    public NoteButton[][] notes() { return mNoteButtons; }

    public Fader[] volumeFaders() { return mVolumeFaders; }
    public Fader[] panFaders() {  return mPanFaders; }
    public Fader[] sendFaders() { return mSendFaders; }
    public Fader[] controlFaders() { return mControlFaders; }

    public LaunchpadXSurface(ControllerHost host, Session session, HardwareSurface surface) {
        mUpArrow = new CCButton(session, surface, "Up", 91, 13, 13);
        mDownArrow = new CCButton(session, surface, "Down", 92, 13 + 23, 13);
        mLeftArrow = new CCButton(session, surface, "Left", 93, 13 + 23*2, 13);
        mRightArrow = new CCButton(session, surface, "Right", 94, 13 + 23*3, 13);
        mSessionButton = new CCButton(session, surface, "Session", 95, 13 + 23*4, 13);
        mNoteButton = new CCButton(session, surface, "Note Mode", 96, 13 + 23*5, 13);
        mRecordButton = new CCButton(session, surface, "Record", 98, 13 + 23*7, 13);
        mNovationButton = new CCButton(session, surface, "N", 99, 13 + 23 * 8, 13);
        mSceneButtons = new CCButton[8];
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
                mNoteButtons[row][col] = new NoteButton(host, session, surface, "" + row + "," + col, row_offsets[row] + col + 1, drum_pad_notes[row * 8 + col], 13 + (col * 23), 13 + 23 + (row * 23));
            }
        }

        mVolumeFaders = new Fader[8];
        for(int i = 0; i < 8; i++) {
            mVolumeFaders[i] = new Fader(host, session, surface, "FV" + i, mVolumeFaderCCs[i], 0, i * 20);
        }

        mPanFaders = new Fader[8];
        for(int i = 0; i < 8; i++) {
            mPanFaders[i] = new Fader(host, session, surface, "FP" + i, mPanFaderCCs[i], 241 - 10, i * 20);
        }

        mSendFaders = new Fader[8];
        for(int i = 0; i < 8; i++) {
            mSendFaders[i] = new Fader(host, session, surface, "FS" + i, mSendFaderCCs[i], 241 - 20, i * 20);
        }

        mControlFaders = new Fader[8];
        for(int i = 0; i < 8; i++) {
            mControlFaders[i] = new Fader(host, session, surface, "FC" + i, mControlFaderCCs[i], 241 - 20, i * 20);
        }
    }

    public void enableFaders(SessionPadMode mode, boolean vertical, boolean bipolar) {
        StringBuilder sysexString = new StringBuilder();
        int[] mFaderCCs;
        switch(mode) {
            case VOLUME:
                mFaderCCs = mVolumeFaderCCs;
                break;
            case PAN:
                mFaderCCs = mPanFaderCCs;
                break;
            case SENDS:
                mFaderCCs = mSendFaderCCs;
                break;
            case CONTROLS:
                mFaderCCs = mControlFaderCCs;
                break;
            default:
                return;
        }
        sysexString.append("01 00");
        if(vertical) {
            sysexString.append("00 ");
        } else {
            sysexString.append("01 ");
        }
//        assert colors.length == 8;
        for(int i = 0; i < 8; i++) {
            sysexString.append(Utils.toHexString((byte)i));
            if(bipolar) {
                sysexString.append("01");
            } else {
                sysexString.append("00");
            }
            sysexString.append(Utils.toHexString((byte)mFaderCCs[i]));
//            sysexString.append(Utils.toHexString((byte)colors[i]));
            sysexString.append("00 ");
        }
        mSurface.invalidateHardwareOutputState();
        mSession.sendSysex(sysexString.toString());
        refreshFaders();
        mSession.sendSysex("00 0D");
    }

    public void refreshFaders() {
        for(int i = 0; i < 8; i++) {
            Fader volFader = mVolumeFaders[i];
            Fader panFader = mPanFaders[i];
            Fader sendFader = mSendFaders[i];
            Fader controlFader = mControlFaders[i];
            mSession.sendMidi(0xB4, volFader.id(), (int)Math.round(volFader.fader().targetValue().get() * 127));
            mSession.sendMidi(0xB4, panFader.id(), (int)Math.round(panFader.fader().targetValue().get() * 127));
            mSession.sendMidi(0xB4, sendFader.id(), (int)Math.round(sendFader.fader().targetValue().get() * 127));
            mSession.sendMidi(0xB4, controlFader.id(), (int)Math.round(controlFader.fader().targetValue().get() * 127));
        }
    }

    public void disableFaders() {
        mSession.sendSysex("00 00");
        mSurface.invalidateHardwareOutputState();
    }

    public void clear() {
        mUpArrow.resetColor();
        mDownArrow.resetColor();
        mLeftArrow.resetColor();
        mRightArrow.resetColor();
        mNovationButton.resetColor();

        for(LaunchpadXPad scenePad : mSceneButtons) {
            scenePad.resetColor();
        }

        for(LaunchpadXPad[] noteRow : mNoteButtons) {
            for(LaunchpadXPad note : noteRow) {
                note.resetColor();
            }
        }
    }
}
