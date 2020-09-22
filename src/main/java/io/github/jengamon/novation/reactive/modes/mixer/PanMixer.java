package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.modes.session.SessionScrollLight;
import io.github.jengamon.novation.surface.Fader;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PanMixer extends AbstractFaderMixerMode {
    private TrackFaderLight[] faders = new TrackFaderLight[8];
    private Parameter[] pans = new Parameter[8];

    private BooleanSyncWrapper canTrackForward;
    private BooleanSyncWrapper canTrackBackward;
    private HardwareActionBindable trackForwardAction;
    private HardwareActionBindable trackBackwardAction;

    public PanMixer(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                    LaunchpadXSurface lSurf, HardwareSurface surf, CursorTrack track, TrackBank bank) {
        super(machine, mixerMode, host, transport, lSurf, surf, track, Mode.MIXER_PAN);

        canTrackForward = new BooleanSyncWrapper(bank.canScrollForwards(), surf, host);
        canTrackBackward = new BooleanSyncWrapper(bank.canScrollBackwards(), surf, host);
        trackForwardAction = bank.scrollForwardsAction();
        trackBackwardAction = bank.scrollBackwardsAction();

        for(int i = 0; i < 8; i++) {
            BooleanSyncWrapper exists = new BooleanSyncWrapper(bank.getItemAt(i).exists(), surf, host);
            ColorSyncWrapper color = new ColorSyncWrapper(bank.getItemAt(i).color(), surf, host);
            faders[i] = new TrackFaderLight(exists, color);
            pans[i] = bank.getItemAt(i).pan();
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        ArrayList<HardwareBinding> list = new ArrayList<>();

        // Enable faders (and bind to proper set)
        surface.setupFaders(false, true, 29);

        // Bind the scene actions and lights
        bindMixerModeIndicator(surface, list, 1, new ColorTag(0xff, 0xa1, 0x61));

        LaunchpadXPad back = surface.up();
        LaunchpadXPad frwd = surface.down();
        list.add(back.button().pressedAction().addBinding(trackBackwardAction));
        list.add(frwd.button().pressedAction().addBinding(trackForwardAction));
        back.light().state().setValue(new SessionScrollLight(back.id(), canTrackBackward));
        frwd.light().state().setValue(new SessionScrollLight(frwd.id(), canTrackForward));

        // Bind faders
        for(int i = 0; i < 8; i++) {
            Fader panFader = surface.faders()[i];
            panFader.light().state().setValue(faders[i]);
            list.add(pans[i].addBinding(panFader.fader()));
        }

        return list;
    }
}
