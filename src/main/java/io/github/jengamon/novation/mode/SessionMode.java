package io.github.jengamon.novation.mode;

import com.bitwig.extension.controller.api.ControllerHost;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.state.LaunchpadXState;

import java.util.function.Supplier;

public class SessionMode implements AbstractMode {
    @Override
    public void onInitialize(ControllerHost host, Session session, LaunchpadXState state, Supplier<Boolean> refresh) {

    }
}
