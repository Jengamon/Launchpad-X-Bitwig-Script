package io.github.jengamon.novation.surface.state;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import io.github.jengamon.novation.Utils;

public class PadLightState extends InternalHardwareLightState {
    private byte mSolid;
    private byte mPulse;
    private byte mBlink;
    private double mBPM;

    public PadLightState(double bpm, byte solid, byte blink, byte pulse) {
        mBPM = bpm;
        mSolid = (solid < 0 ? 0 : solid);
        mBlink = (blink < 0 ? 0 : blink);
        mPulse = (pulse < 0 ? 0 : pulse);
    }

    public static PadLightState solidLight(int color) {
        return new PadLightState(1.0, (byte)color, (byte)0, (byte)0);
    }

    public static PadLightState pulseLight(double bpm, int color) {
        return new PadLightState(bpm, (byte)0, (byte)0, (byte)color);
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        if(mPulse > 0) {
            Color pulseColor = Utils.fromNovation(mPulse);
            return HardwareLightVisualState.createBlinking(
                    pulseColor,
                    Color.mix(pulseColor, Color.nullColor(), 0.7),
                    60.0 / mBPM,
                    60.0 / mBPM
            );
        }

        Color solidColor = Utils.fromNovation(mSolid);
        if(mBlink > 0) {
            Color blinkColor = Utils.fromNovation(mBlink);
            return HardwareLightVisualState.createBlinking(
                    blinkColor,
                    solidColor,
                    60.0 / mBPM,
                    60.0 / mBPM
            );
        }

        return HardwareLightVisualState.createForColor(solidColor);
    }

    @Override
    public boolean equals(Object o) {
        if(o.getClass() == PadLightState.class) {
            PadLightState other = (PadLightState)o;
            return mSolid == other.mSolid && mPulse == other.mPulse && mBlink == other.mBlink && mBPM == other.mBPM;
        } else {
            return false;
        }
    }

    public byte solid() { return mSolid; }
    public byte pulse() { return mPulse; }
    public byte blink() { return mBlink; }
}
