package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.ColorValueChangedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Value;

import java.util.concurrent.atomic.AtomicReference;

public class ColorSyncWrapper {
    private AtomicReference<Color> value = new AtomicReference<>();

    public ColorSyncWrapper(Value<ColorValueChangedCallback> val, HardwareSurface surface, ControllerHost host) {
        val.addValueObserver((r, g, b) -> {
            value.set(Color.fromRGB(r, g, b));
            surface.invalidateHardwareOutputState();
            host.requestFlush();
        });
        value.set(Color.nullColor());
    }

    public Color get() {
        return value.get();
    }
}
