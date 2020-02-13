package io.github.jengamon.novation.reactive.modes.session;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;

import java.util.concurrent.atomic.AtomicReference;

import static io.github.jengamon.novation.reactive.modes.session.SessionPadMode.SESSION;

public class SessionPadLight extends SessionSendableLightState {
    private AtomicReference<SessionPadMode> mPadMode;
    private ColorSyncWrapper mBaseColor;
    private BooleanSyncWrapper mArmed;
    private BooleanSyncWrapper mSceneExists;
    private IntegerSyncWrapper mPlaybackState;
    private BooleanSyncWrapper mIsQueued;
    private RangedValueSyncWrapper mBPM;
    private BooleanSyncWrapper mIsTrackEnabled;
    private int mTrack;
    private int mScene;

    public SessionPadLight(int x, int y, RangedValueSyncWrapper bpm, AtomicReference<SessionPadMode> padMode, ColorSyncWrapper baseColor, BooleanSyncWrapper armed,
                           BooleanSyncWrapper sceneExists, IntegerSyncWrapper playbackState, BooleanSyncWrapper isQueued, BooleanSyncWrapper isTrackEnabled) {
        mTrack = x;
        mScene = y;
        mBaseColor = baseColor;
        mPadMode = padMode;
        mArmed = armed;
        mSceneExists = sceneExists;
        mPlaybackState = playbackState;
        mIsTrackEnabled = isTrackEnabled;
        mIsQueued = isQueued;
        mBPM = bpm;
    }

    public int track() { return mTrack; }
    public int scene() { return mScene; }

    private ColorTag getSolidColor() {
        ColorTag baseColor = Utils.toTag(mBaseColor.get());
        switch (mPadMode.get()) {
            case SESSION:
            case VOLUME:
            case PAN:
            case SENDS:
            case CONTROLS:
                if(baseColor.selectNovationColor() == 0 && mArmed.get() && mSceneExists.get() && mIsTrackEnabled.get()) {
                    return new ColorTag(0xaa, 0x61, 0x61);
                } else {
                    return (mIsTrackEnabled.get() ? baseColor : new ColorTag(0, 0, 0));
                }
            default:
                return new ColorTag(0, 0, 0);
        }
    }

    private ColorTag getBlinkColor() {
        switch(mPadMode.get()) {
            case SESSION:
            case VOLUME:
            case PAN:
            case SENDS:
            case CONTROLS:
                switch(mPlaybackState.get()) {
                    case 0: // Stopped
                        if(mIsQueued.get() && mIsTrackEnabled.get()) {
                            return new ColorTag(255, 97, 97);
                        } else {
                            return null;
                        }
                    case 1: // Playing
                        if(mIsQueued.get() && mIsTrackEnabled.get()) {
                            return new ColorTag(97, 255, 97);
                        } else {
                            return null;
                        }
                    case 2: // Recording
                        if(mIsQueued.get() && mIsTrackEnabled.get()) {
                            return new ColorTag(0xdd, 0x61, 0x61);
                        } else {
                            return null;
                        }
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private ColorTag getPulseColor() {
        switch(mPadMode.get()) {
            case SESSION:
            case VOLUME:
            case PAN:
            case SENDS:
            case CONTROLS:
                switch(mPlaybackState.get()) {
                    case 2: // Recording
                        if(!mIsQueued.get() && mIsTrackEnabled.get()) return new ColorTag(0xdd, 0x61, 0x61);
                    case 1: // Playing
                        if(!mIsQueued.get() && mIsTrackEnabled.get()) return Utils.toTag(mBaseColor.get());
                    case 0: // Stopped
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        ColorTag sc = getSolidColor();
        ColorTag bc = getBlinkColor();
        ColorTag pc = getPulseColor();
        if(pc != null) {
            return HardwareLightVisualState.createBlinking(pc.toBitwigColor(), Color.mix(pc.toBitwigColor(), Color.nullColor(), 0.7), 60.0 / mBPM.get(), 60.0 / mBPM.get());
        } else {
            if(bc != null) {
                return HardwareLightVisualState.createBlinking(bc.toBitwigColor(), sc.toBitwigColor(), 30.0 / mBPM.get(), 30.0 / mBPM.get());
            } else {
                return HardwareLightVisualState.createForColor(sc.toBitwigColor());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public void send(Session session) {
        ColorTag sc = getSolidColor();
        ColorTag bc = getBlinkColor();
        ColorTag pc = getPulseColor();
        int id = (8 - mScene) * 10 + mTrack + 1;
        session.sendMidi(0x90, id, sc.selectNovationColor());
        if(bc != null) session.sendMidi(0x91, id, bc.selectNovationColor());
        if(pc != null) session.sendMidi(0x92, id, pc.selectNovationColor());
    }
}
