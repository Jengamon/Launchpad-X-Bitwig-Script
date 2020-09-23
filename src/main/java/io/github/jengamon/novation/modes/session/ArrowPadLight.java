package io.github.jengamon.novation.modes.session;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.BooleanValue;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.state.PadLightState;

import java.util.function.Consumer;

public class ArrowPadLight {
    private BooleanValue mIsValid;
    public ArrowPadLight(LaunchpadXSurface surface, BooleanValue isValid, Consumer<LaunchpadXSurface> redraw) {
        mIsValid = isValid;

        mIsValid.addValueObserver(v -> redraw.accept(surface));
    }

    public void draw(MultiStateHardwareLight arrowLight) {
        if(mIsValid.get()) {
            arrowLight.state().setValue(PadLightState.solidLight(84));
        } else {
            arrowLight.setColor(Color.nullColor());
        }
    }
}
