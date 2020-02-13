package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Value;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class BooleanSyncWrapper {
    private AtomicBoolean value = new AtomicBoolean(false);

    public BooleanSyncWrapper(Value<BooleanValueChangedCallback> val, HardwareSurface surface, ControllerHost host) {
        val.addValueObserver(newValue -> {
            value.set(newValue);
            surface.invalidateHardwareOutputState();
            host.requestFlush();
        });
    }

    public boolean get() {
        return value.get();
    }
}
