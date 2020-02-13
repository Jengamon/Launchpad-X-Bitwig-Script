package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Value;

import java.util.concurrent.atomic.AtomicInteger;

public class IntegerSyncWrapper {
    private AtomicInteger value = new AtomicInteger(0);

    public IntegerSyncWrapper(Value<IntegerValueChangedCallback> val, HardwareSurface surf, ControllerHost host) {
        val.addValueObserver(num -> {
            value.set(num);
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });
    }

    public int get() {
        return value.get();
    }
}
