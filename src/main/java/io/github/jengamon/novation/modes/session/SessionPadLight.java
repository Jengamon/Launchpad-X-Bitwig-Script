package io.github.jengamon.novation.modes.session;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.state.PadLightState;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SessionPadLight {
    private RangedValue mBPM;
    private BooleanValue mArmed;
    private BooleanValue mExists;
    private BooleanValue mHasContent;
    private ColorValue mColor;

    private int mSlotIndex = -1;

    private static class SlotState {
        public AtomicInteger mStateIndex;
        public AtomicBoolean mIsQueued;

        public SlotState() {
            mStateIndex = new AtomicInteger(0);
            mIsQueued = new AtomicBoolean(false);
        }
    }

    private SlotState[] mSlotStates = new SlotState[8];

    private enum State {
        STOPPED,
        PLAYING,
        RECORDING,
        QUEUE_STOP,
        QUEUE_PLAY,
        QUEUE_RECORD
    }

    public SessionPadLight(LaunchpadXSurface surface, ClipLauncherSlot slot, Track track, RangedValue bpm, Consumer<LaunchpadXSurface> redraw, int index) {
        mBPM = bpm;
        mArmed = track.arm();
        mHasContent = slot.hasContent();
        mColor = slot.color();
        mExists = slot.exists();
        mSlotIndex = index;

        // Also refresh whenever a slot's *existence* value changes ig...
        mHasContent.addValueObserver(ae -> redraw.accept(surface));
        mBPM.addValueObserver(b -> redraw.accept(surface));
        mArmed.addValueObserver(a -> redraw.accept(surface));
        mExists.addValueObserver(e -> redraw.accept(surface));
        mColor.addValueObserver((r, g, b) -> redraw.accept(surface));

        for(int i = 0; i < 8; i++) {
            mSlotStates[i] = new SlotState();
        }
        
        slot.sceneIndex().addValueObserver(si -> redraw.accept(surface));
        track.clipLauncherSlotBank().addPlaybackStateObserver((slotIndex, state, isQueued) -> {
            SlotState slotState = mSlotStates[slotIndex];
            slotState.mStateIndex.set(state);
            slotState.mIsQueued.set(isQueued);
            redraw.accept(surface);
        });
    }

    /**
     * Because Bitwig basically fucks us over with non-descriptive/buggy APIs, we have to do this.
     *
     * Calculates the state of a button, given the last updated state and isQueued values of *all* slots.
     * @return the current state the button should be in.
     */
    private State getState() {
        int state = mSlotStates[mSlotIndex].mStateIndex.get();
        boolean isQueued = mSlotStates[mSlotIndex].mIsQueued.get();
        if (state == 0) {
            return (isQueued ? State.QUEUE_STOP : State.STOPPED);
        } else if (state == 1) {
            return (isQueued ? State.QUEUE_PLAY : State.PLAYING);
        } else if (state == 2) {
            return (isQueued ? State.QUEUE_RECORD : State.RECORDING);
        } else {
            throw new RuntimeException("Invalid state " + state);
        }
    }

    public void draw(MultiStateHardwareLight slotLight) {
        byte pulseColor = (byte)0;
        byte blinkColor = (byte)0;
        byte solidColor = (byte)0;

        State mState = getState();

        byte slotColor = Utils.toNovation(mColor.get());

        if(mExists.get() && mHasContent.get()) {
            solidColor = slotColor;
            switch(mState) {
                case PLAYING:
                    pulseColor = slotColor;
                    break;
                case RECORDING:
                    blinkColor = 6;
                    break;
                case QUEUE_PLAY:
                    blinkColor = 0x19;
                    break;
                case QUEUE_STOP:
                    blinkColor = 5;
                    break;
                case QUEUE_RECORD:
                    pulseColor = 6;
                    break;
                case STOPPED:
                    break;
            }
        } else {
            if(mArmed.get() && mExists.get()) {
                solidColor = 0x7;
            }

            switch(mState) {
                case QUEUE_RECORD:
                    pulseColor = 6;
                    break;
                case RECORDING:
                case PLAYING:
                case STOPPED:
                case QUEUE_PLAY:
                case QUEUE_STOP:
                    break;
            }
        }

        slotLight.state().setValue(new PadLightState(mBPM.getRaw(), solidColor, blinkColor, pulseColor));
    }
}
