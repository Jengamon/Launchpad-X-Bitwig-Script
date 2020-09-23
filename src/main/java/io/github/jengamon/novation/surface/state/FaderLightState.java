package io.github.jengamon.novation.surface.state;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import io.github.jengamon.novation.Utils;

public class FaderLightState extends InternalHardwareLightState {
    private byte mSolid;

    public FaderLightState(byte solid) {
        mSolid = (solid < 0 ? 0 : solid);
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        Color solidColor = Utils.fromNovation(mSolid);

        return HardwareLightVisualState.createForColor(solidColor);
    }

    @Override
    public boolean equals(Object o) {
        if(o.getClass() == FaderLightState.class) {
            FaderLightState other = (FaderLightState)o;
            return mSolid == other.mSolid;
        } else {
            return false;
        }
    }

    public byte solid() { return mSolid; }
}
