package io.github.jengamon.novation.reactive.modes.session;

import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;

import java.util.concurrent.atomic.AtomicReference;

public class MixerSessionPadLight extends SessionPadLight {
    private AtomicReference<SessionPadMode> mLastRowMode;
    public MixerSessionPadLight(int x, int y, RangedValueSyncWrapper bpm, AtomicReference<SessionPadMode> padMode, ColorSyncWrapper baseColor,
                                BooleanSyncWrapper armed, BooleanSyncWrapper sceneExists, IntegerSyncWrapper playbackState, BooleanSyncWrapper isQueued,
                                BooleanSyncWrapper isTrackEnabled, BooleanSyncWrapper isMuted, BooleanSyncWrapper isSoloed, BooleanSyncWrapper isStopped,
                                BooleanSyncWrapper trackExists, BooleanSyncWrapper hasNoteInput, BooleanSyncWrapper hasAudioInput, AtomicReference<SessionPadMode> lastRowMode) {
        super(x, y, bpm, padMode, baseColor, armed, sceneExists, playbackState, isQueued, isTrackEnabled, isMuted, isSoloed, isStopped, trackExists, hasNoteInput, hasAudioInput);

        mLastRowMode = lastRowMode;
    }

    private boolean isValid() {
        switch(mLastRowMode.get()) {
            case SESSION:
            case STOP:
            case MUTE:
            case SOLO:
            case RECORD:
                return true;
            default:
                return false;
        }
    }

    @Override
    ColorTag getSolidColor() {
        if(isValid()) {
            return super.getSolidColor();
        } else {
            return new ColorTag(0, 0, 0);
        }
    }

    @Override
    ColorTag getBlinkColor() {
        if(isValid()) {
            return super.getBlinkColor();
        } else {
            return null;
        }
    }

    @Override
    ColorTag getPulseColor() {
        if(isValid()) {
            return super.getPulseColor();
        } else {
            return null;
        }
    }
}
