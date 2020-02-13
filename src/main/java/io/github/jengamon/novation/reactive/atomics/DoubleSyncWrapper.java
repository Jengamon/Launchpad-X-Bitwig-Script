package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Value;

import java.util.concurrent.atomic.AtomicLong;

public class DoubleSyncWrapper {
    private AtomicLong value = new AtomicLong(0L);

    public DoubleSyncWrapper(Value<DoubleValueChangedCallback> val, HardwareSurface surf, ControllerHost host) {
        val.addValueObserver(dbl -> {
            value.set(Double.doubleToLongBits(dbl));
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });
    }

    public double get() {
        return Double.longBitsToDouble(value.get());
    }
}
