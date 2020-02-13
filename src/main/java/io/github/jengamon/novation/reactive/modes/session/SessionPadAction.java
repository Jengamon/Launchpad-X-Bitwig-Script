package io.github.jengamon.novation.reactive.modes.session;

import com.bitwig.extension.controller.api.ClipLauncherSlot;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.Track;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class SessionPadAction implements Runnable, Supplier<String> {
    private AtomicReference<SessionPadMode> mPadMode;
    private int mTrack;
    private int mScene;
    private Track mClipTrack;
    private ClipLauncherSlot mSlot;
    private BooleanSyncWrapper mIsTrackEnabled;

    public SessionPadAction(int x, int y, AtomicReference<SessionPadMode> padMode, ClipLauncherSlot slot, Track track, BooleanSyncWrapper isTrackEnabled) {
        mTrack = x;
        mScene = y;
        mSlot = slot;
        mClipTrack = track;
        mIsTrackEnabled = isTrackEnabled;
        mPadMode = padMode;
    }

    private int calcPad() {
        return (8 - mScene) * 10 + mTrack + 1;
    }

    @Override
    public void run() {
        switch(mPadMode.get()) {
            case SESSION:
                mSlot.launch();
                break;
            case MUTE:
                if(mIsTrackEnabled.get()) mClipTrack.mute().toggle();
                break;
            case SOLO:
                if(mIsTrackEnabled.get()) mClipTrack.solo().toggle();
                break;
            case STOP:
                if(mIsTrackEnabled.get()) mClipTrack.stop();
                break;
            case RECORD:
                if(mIsTrackEnabled.get()) mClipTrack.arm().toggle();
                break;
            default:
        }
    }

    @Override
    public String get() {
        return "Press Session Pad on " + calcPad() + " in mode " + mPadMode.get();
    }
}
