package io.github.jengamon.novation.reactive;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.modes.DrumPadMode;
import io.github.jengamon.novation.reactive.modes.SessionMode;

public class InternalState {
    private Mode mMode;
    private RangedValue mBPM;
    private CursorTrack mCursorTrack;
    private CursorDevice mCursorDevice;
    private TrackBank mTrackBank;
    private TrackBank mMixerTrackBank;
    private DrumPadBank mDrumPadBank;
    private Transport mTransport;

    private BooleanValue mFollowPref;
    private BooleanValue mSendTrackNotes;
    private EnumValue mRecordMode;
    private EnumValue mRecordButtonBehavior;
    private EnumValue mModeDoublePref;

    private SessionMode mSessionMode;
    private DrumPadMode mDrumPadMode;

    public Mode mode() { return mMode; }
    public void setMode(Mode mode) { mMode = mode; }

    public InternalState(ControllerHost host, Mode mode) {
        mMode = mode;
        mTransport = host.createTransport();
        mBPM = mTransport.tempo().modulatedValue();
        mBPM.markInterested();
        mCursorTrack = host.createCursorTrack(8, 0);
        mCursorDevice = mCursorTrack.createCursorDevice("Primary", "Primary Instrument", 0, CursorDeviceFollowMode.FIRST_INSTRUMENT);
        mTrackBank = host.createTrackBank(8, 8, 8, true);
        mMixerTrackBank = host.createTrackBank(8, 0, 0, false);
        mDrumPadBank = mCursorDevice.createDrumPadBank(64);

        Preferences preferences = host.getPreferences();
        mFollowPref = preferences.getBooleanSetting("Follow Selection?", "Behavior", true);
        mSendTrackNotes = preferences.getBooleanSetting("Send Track Notes?", "Behavior", true);
        mRecordMode = preferences.getEnumSetting("Record Mode", "Behavior", new String[]{"Clip Launcher", "Global"}, "Clip Launcher");
        mRecordButtonBehavior = preferences.getEnumSetting("Record Button Behavior", "Behavior", new String[]{"Toggle Record", "Cycle Selection"}, "Toggle Record");
        mModeDoublePref = preferences.getEnumSetting("On Mixer Mode Button Double Press", "Behavior", new String[]{"Do Nothing", "Do Action"}, "Do Nothing");

        //mSessionMode = new SessionMode();
        //mDrumPadMode = new DrumPadMode(mDrumPadBank);
    }
}
