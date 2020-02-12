package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import io.github.jengamon.novation.internal.Session;

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

    private CCButton[] mSceneButtons;

    private NoteButton[][] mNoteButtons;

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
                mNoteButtons[row][col] = new NoteButton(host, session, surface, "" + row + "," + col, row_offsets[row] + col, drum_pad_notes[row * 8 + col], 13 + (col * 23), 13 + 23 + (row * 23));
            }
        }
    }

    public void clear() {
        mUpArrow.resetColor();
        mDownArrow.resetColor();
        mLeftArrow.resetColor();
        mRightArrow.resetColor();
        mRecordButton.resetColor();
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
