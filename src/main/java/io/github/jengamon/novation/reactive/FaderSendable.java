package io.github.jengamon.novation.reactive;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import io.github.jengamon.novation.ColorTag;

public abstract class FaderSendable extends InternalHardwareLightState {
    public abstract ColorTag faderColor();

    @Override
    public HardwareLightVisualState getVisualState() {
        return HardwareLightVisualState.createForColor(faderColor().toBitwigColor());
    }
}
