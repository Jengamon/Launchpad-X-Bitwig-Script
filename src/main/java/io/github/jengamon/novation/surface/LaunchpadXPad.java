package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.AbsoluteHardwareKnob;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import io.github.jengamon.novation.surface.state.PadLightState;

public abstract class LaunchpadXPad {
    public abstract HardwareButton button();
    public abstract AbsoluteHardwareKnob aftertouch();
    public abstract MultiStateHardwareLight light();
    public abstract int id();

    public void resetColor() {
        light().state().setValue(PadLightState.solidLight(0));
    }

    public void updateBPM(double newBPM) {
        PadLightState state = (PadLightState)light().state().currentValue();
        if(state != null) {
            PadLightState newState = new PadLightState(newBPM, state.solid(), state.blink(), state.pulse());
            light().state().setValue(newState);
        }
    }
}
