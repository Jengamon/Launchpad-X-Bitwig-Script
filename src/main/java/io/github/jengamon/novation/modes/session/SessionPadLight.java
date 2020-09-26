package io.github.jengamon.novation.modes.session;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.state.PadLightState;

import java.util.function.Consumer;

public class SessionPadLight {
    private RangedValue mBPM;
    private State mState;
    private IntegerValue mSlotIndex;
    private BooleanValue mArmed;
    private BooleanValue mExists;
    private ColorValue mColor;

    private enum State {
        STOPPED,
        PLAYING,
        RECORDING,
        QUEUE_STOP,
        QUEUE_PLAY,
        QUEUE_RECORD
    }

    public SessionPadLight(LaunchpadXSurface surface, ClipLauncherSlot slot, Track track, RangedValue bpm, Consumer<LaunchpadXSurface> redraw) {
        mBPM = bpm;
        mArmed = track.arm();
        mExists = slot.hasContent();
        mColor = slot.color();
        mSlotIndex = slot.sceneIndex();
        mState = State.STOPPED;

        mBPM.addValueObserver(b -> redraw.accept(surface));
        mArmed.addValueObserver(a -> redraw.accept(surface));
        mExists.addValueObserver(e -> redraw.accept(surface));
        mColor.addValueObserver((r, g, b) -> redraw.accept(surface));
        mSlotIndex.addValueObserver(si -> redraw.accept(surface));
        track.clipLauncherSlotBank().addPlaybackStateObserver((slotIndex, state, isQueued) -> {
            if(slotIndex == mSlotIndex.get()) {
                if(state == 0) {
                    mState = (isQueued ? State.QUEUE_STOP : State.STOPPED);
                } else if (state == 1) {
                    mState = (isQueued ? State.QUEUE_PLAY : State.PLAYING);
                } else if (state == 2) {
                    mState = (isQueued ? State.QUEUE_RECORD : State.RECORDING);
                } else {
                    throw new RuntimeException("Invalid state " + state);
                }
            }
            redraw.accept(surface);
        });
    }

    public void draw(MultiStateHardwareLight slotLight) {
        byte pulseColor = (byte)0;
        byte blinkColor = (byte)0;
        byte solidColor = (byte)0;

        byte slotColor = Utils.toNovation(mColor.get());

        if(mExists.get()) {
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
            if(mArmed.get()) {
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
