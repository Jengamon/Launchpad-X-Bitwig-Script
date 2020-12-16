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

public class VolumeMixer extends AbstractFaderMixerMode {
    private TrackColorFaderLight[] faderLights = new TrackColorFaderLight[8];
    private Parameter[] volumes = new Parameter[8];

    private ArrowPadLight trackForwardLight;
    private ArrowPadLight trackBackwardLight;
    private HardwareActionBindable trackForwardAction;
    private HardwareActionBindable trackBackwardAction;


    public VolumeMixer(AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                       LaunchpadXSurface surface, TrackBank bank) {
        super(mixerMode, host, transport, surface, Mode.MIXER_VOLUME, 64);

        for(int i = 0; i < 8; i++) {
            Track track = bank.getItemAt(i);

            faderLights[i] = new TrackColorFaderLight(surface, track, this::redraw);
            volumes[i] = track.volume();
        }

        trackForwardLight = new ArrowPadLight(surface, bank.canScrollForwards(), mModeColor, this::redraw);
        trackBackwardLight = new ArrowPadLight(surface, bank.canScrollBackwards(), mModeColor, this::redraw);
        trackForwardAction = bank.scrollForwardsAction();
        trackBackwardAction = bank.scrollBackwardsAction();
    }

    private LaunchpadXPad getBack(LaunchpadXSurface surface) { return surface.left(); }
    private LaunchpadXPad getForward(LaunchpadXSurface surface) { return surface.right(); }

    @Override
    public void onDraw(LaunchpadXSurface surface) {
        super.onDraw(surface);

        drawMixerModeIndicator(surface, 0);

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

        // Enable faders
        surface.setupFaders(true, false, 21);

        LaunchpadXPad back = getBack(surface);
        LaunchpadXPad frwd = getForward(surface);
        list.add(back.button().pressedAction().addBinding(trackBackwardAction));
        list.add(frwd.button().pressedAction().addBinding(trackForwardAction));

        for(int i = 0; i < 8; i++) {
            Fader volumeFader = surface.faders()[i];
            list.add(volumes[i].addBinding(volumeFader.fader()));
        }

        return list;
    }
}
