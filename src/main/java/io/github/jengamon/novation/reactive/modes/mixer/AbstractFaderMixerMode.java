package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.Transport;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Switches the Launchpad to fader mode, when all bindings are complete
 */
public abstract class AbstractFaderMixerMode extends AbstractMixerMode {

    public AbstractFaderMixerMode(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host,
                                  Transport transport, LaunchpadXSurface lSurf, HardwareSurface surf,
                                  CursorTrack track, Mode targetMode) {
        super(machine, mixerMode, host, transport, lSurf, surf, track, targetMode);
    }

    @Override
    public void finishedBind(Session session) {
        super.finishedBind(session);
        session.sendSysex("00 0D");
    }
}
