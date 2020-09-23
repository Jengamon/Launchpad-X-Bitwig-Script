package io.github.jengamon.novation.modes.mixer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.Transport;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Switches the Launchpad to fader mode, when all bindings are complete
 */
public abstract class AbstractFaderMixerMode extends AbstractMixerMode {

    public AbstractFaderMixerMode(AtomicReference<Mode> mixerMode, ControllerHost host,
                                  Transport transport, LaunchpadXSurface lSurf, Mode targetMode) {
        super(mixerMode, host, transport, lSurf, targetMode);
    }

    @Override
    public void finishedBind(Session session) {
        super.finishedBind(session);
        session.sendSysex("00 0D");
    }
}
