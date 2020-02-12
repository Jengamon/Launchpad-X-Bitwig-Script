package io.github.jengamon.novation.mode;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareSurface;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.state.ArrowValue;
import io.github.jengamon.novation.state.LaunchpadXState;

import java.util.function.Supplier;

public interface AbstractMode {
    void onInitialize(ControllerHost host, Session session, LaunchpadXState state, Supplier<Boolean> refresh);
}
