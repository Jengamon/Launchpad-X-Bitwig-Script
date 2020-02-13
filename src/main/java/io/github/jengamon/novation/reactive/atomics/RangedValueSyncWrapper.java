package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.RangedValue;

import java.util.concurrent.atomic.AtomicLong;

public class RangedValueSyncWrapper {
    private AtomicLong value = new AtomicLong(0L);

    public RangedValueSyncWrapper(RangedValue val, HardwareSurface surf, ControllerHost host) {
        val.addRawValueObserver(raw -> {
            value.set(Double.doubleToLongBits(raw));
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });
    }

    public double get() {
        return Double.longBitsToDouble(value.get());
    }
}
