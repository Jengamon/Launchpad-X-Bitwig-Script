package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;

public class ToggleLight extends SessionSendableLightState {
    private BooleanSyncWrapper mTrackExists;
    private BooleanSyncWrapper mToggleState;
    private ColorTag mOnState;
    private ColorTag mOffState;
    private int mID;

    public ToggleLight(int id, BooleanSyncWrapper trackExists, BooleanSyncWrapper toggleState, ColorTag onState, ColorTag offState) {
        mID = id;
        mTrackExists = trackExists;
        mToggleState = toggleState;
        mOnState = onState;
        mOffState = offState;
    }

    private ColorTag calculateColor() {
        if(mTrackExists.get()) {
            return (mToggleState.get() ? mOnState : mOffState);
        } else {
            return ColorTag.NULL_COLOR;
        }
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        ColorTag color = calculateColor();
        return HardwareLightVisualState.createForColor(color.toBitwigColor());
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public void send(Session session) {
        ColorTag color = calculateColor();
        session.sendMidi(0x90, mID, color.selectNovationColor());
    }
}
