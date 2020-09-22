package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;

public class MixerModeLight extends SessionSendableLightState {
    private RangedValueSyncWrapper mBPM;
    private int mID;
    private static ColorTag mTarget;

    public MixerModeLight(RangedValueSyncWrapper bpm, int id, ColorTag target) {
        mID = id;
        mBPM = bpm;
        mTarget = target;
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return HardwareLightVisualState.createBlinking(
                mTarget.toBitwigColor(),
                Color.nullColor(),
                //Color.mix(mTarget.toBitwigColor(), Color.nullColor(), 0.7),
                60.0 / mBPM.get(),
                60.0 / mBPM.get()
        );
    }

    @Override
    public boolean equals(Object obj) {
        return false; // TODO Implement this.
    }

    @Override
    public void send(Session session) {
        session.sendMidi(0xB2, mID, mTarget.selectNovationColor());
    }
}
