package io.github.jengamon.novation.modes.session;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.state.PadLightState;

import java.util.function.Consumer;

public class FaderModeArrowPadLight {
    private BooleanValue mValid;
    public FaderModeArrowPadLight(LaunchpadXSurface surface, BooleanValue valid, Consumer<LaunchpadXSurface> redraw) {
        mValid = valid;

        mValid.addValueObserver(v -> redraw.accept(surface));
    }

    public void draw(MultiStateHardwareLight arrowLight) {
        if(mValid.get()) {
            arrowLight.state().setValue(PadLightState.solidLight(84));
        } else {
            arrowLight.setColor(Color.nullColor());
        }
    }
}
