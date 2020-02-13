package io.github.jengamon.novation.reactive.atomics;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.ObjectArrayValue;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class ObjectArraySyncWrapper<T> {
    private AtomicReferenceArray<T> value = new AtomicReferenceArray<T>(0);

    public ObjectArraySyncWrapper(ObjectArrayValue<T> array, HardwareSurface surf, ControllerHost host) {
        array.addValueObserver(arr -> {
            value = new AtomicReferenceArray<>(arr);
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });
    }

    public T get(int i) {
        return value.get(i);
    }

    public int length() {
        return value.length();
    }
}
