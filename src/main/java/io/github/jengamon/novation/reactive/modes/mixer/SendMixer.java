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

public class SendMixer extends AbstractFaderMixerMode {
    private TrackFaderLight[] faders = new TrackFaderLight[8];
    private Parameter[] sends = new Parameter[8];

    private BooleanSyncWrapper canTrackForward;
    private BooleanSyncWrapper canTrackBackward;
    private HardwareActionBindable trackForwardAction;
    private HardwareActionBindable trackBackwardAction;

    public SendMixer(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                     LaunchpadXSurface lSurf, HardwareSurface surf, CursorTrack track) {
        super(machine, mixerMode, host, transport, lSurf, surf, track, Mode.MIXER_SEND);

        SendBank bank = track.sendBank();

        for(int i = 0; i < 8; i++) {
            BooleanSyncWrapper exists = new BooleanSyncWrapper(bank.getItemAt(i).exists(), surf, host);
//            BooleanSyncWrapper prefader = new BooleanSyncWrapper(bank.getItemAt(i).isPreFader(), surf, host);
            ColorSyncWrapper color = new ColorSyncWrapper(bank.getItemAt(i).sendChannelColor(), surf, host);
            faders[i] = new TrackFaderLight(exists, color);
            sends[i] = bank.getItemAt(i);
        }

        canTrackForward = new BooleanSyncWrapper(bank.canScrollForwards(), surf, host);
        canTrackBackward = new BooleanSyncWrapper(bank.canScrollBackwards(), surf, host);
        trackForwardAction = bank.scrollForwardsAction();
        trackBackwardAction = bank.scrollBackwardsAction();
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        ArrayList<HardwareBinding> list = new ArrayList<>();

        // Enable faders
        surface.setupFaders(true, false, 37);

        // Bind the scene actions and lights
        bindMixerModeIndicator(surface, list, 2, new ColorTag(0xfa, 0xb3, 0xff));

        LaunchpadXPad back = surface.left();
        LaunchpadXPad frwd = surface.right();
        list.add(back.button().pressedAction().addBinding(trackBackwardAction));
        list.add(frwd.button().pressedAction().addBinding(trackForwardAction));
        back.light().state().setValue(new SessionScrollLight(back.id(), canTrackBackward));
        frwd.light().state().setValue(new SessionScrollLight(frwd.id(), canTrackForward));

        for(int i = 0; i < 8; i++) {
            Fader sendFader = surface.faders()[i];
            sendFader.light().state().setValue(faders[i]);
            list.add(sends[i].addBinding(sendFader.fader()));
        }

        return list;
    }
}
