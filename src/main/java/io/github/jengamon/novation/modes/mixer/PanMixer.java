package io.github.jengamon.novation.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.modes.session.ArrowPadLight;
import io.github.jengamon.novation.modes.session.TrackColorFaderLight;
import io.github.jengamon.novation.surface.Fader;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PanMixer extends AbstractFaderMixerMode {
    private TrackColorFaderLight[] faderLights = new TrackColorFaderLight[8];
    private Parameter[] pans = new Parameter[8];

    private ArrowPadLight trackForwardLight;
    private ArrowPadLight trackBackwardLight;
    private HardwareActionBindable trackForwardAction;
    private HardwareActionBindable trackBackwardAction;

    public PanMixer(AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                    LaunchpadXSurface surface, TrackBank bank) {
        super(mixerMode, host, transport, surface, Mode.MIXER_VOLUME, 80);

        for(int i = 0; i < 8; i++) {
            Track track = bank.getItemAt(i);
            faderLights[i] = new TrackColorFaderLight(surface, track, this::redraw);
            pans[i] = track.pan();
        }

        trackForwardLight = new ArrowPadLight(surface, bank.canScrollForwards(), mModeColor, this::redraw);
        trackBackwardLight = new ArrowPadLight(surface, bank.canScrollBackwards(), mModeColor, this::redraw);
        trackForwardAction = bank.scrollForwardsAction();
        trackBackwardAction = bank.scrollBackwardsAction();
    }

    private LaunchpadXPad getBack(LaunchpadXSurface surface) { return surface.up(); }
    private LaunchpadXPad getForward(LaunchpadXSurface surface) { return surface.down(); }

    @Override
    public void onDraw(LaunchpadXSurface surface) {
        super.onDraw(surface);

        drawMixerModeIndicator(surface, 1);

        Fader[] faders = surface.faders();
        for(int i = 0; i < faders.length; i++) {
            faderLights[i].draw(faders[i].light());
        }

        LaunchpadXPad back = getBack(surface);
        LaunchpadXPad frwd = getForward(surface);
        trackBackwardLight.draw(back.light());
        trackForwardLight.draw(frwd.light());
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> list = super.onBind(surface);

        // Enable faders (and bind to proper set)
        surface.setupFaders(false, true, 29);

        LaunchpadXPad back = getBack(surface);
        LaunchpadXPad frwd = getForward(surface);
        list.add(back.button().pressedAction().addBinding(trackBackwardAction));
        list.add(frwd.button().pressedAction().addBinding(trackForwardAction));

        // Bind faders
        for(int i = 0; i < 8; i++) {
            Fader panFader = surface.faders()[i];
            list.add(pans[i].addBinding(panFader.fader()));
        }

        return list;
    }
}
