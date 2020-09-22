package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.reactive.FaderSendable;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.surface.Fader;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ControlsMixer extends AbstractFaderMixerMode {
    private FaderSendable[] faders = new FaderSendable[8];
    private Parameter[] controls = new Parameter[8];
    private static final ColorTag[] CONTROL_TAGS = new ColorTag[] {
            new ColorTag(0xff, 0x61, 0x61),
            new ColorTag(0xff, 0xa1, 0x61),
            new ColorTag(0xff, 0xff, 0x61),
            new ColorTag(0x61, 0xff, 0x61),
            new ColorTag(0x61, 0xff, 0xcc),
            new ColorTag(0x61, 0xee, 0xff),
            new ColorTag(0xff, 0x61, 0xff),
            new ColorTag(0xff, 0x61, 0xc2),
    };

    public ControlsMixer(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                         LaunchpadXSurface lSurf, HardwareSurface surf, CursorTrack track) {
        super(machine, mixerMode, host, transport, lSurf, surf, track, Mode.MIXER_CONTROLS);

        CursorRemoteControlsPage controlPage = track.createCursorDevice().createCursorRemoteControlsPage(8);

        for(int i = 0; i < 8; i++) {
            RemoteControl control = controlPage.getParameter(i);
            BooleanSyncWrapper controlExists = new BooleanSyncWrapper(control.exists(), surf, host);
            final int j = i;
            faders[i] = new FaderSendable() {
                @Override
                public boolean equals(Object o) {
                    return false;
                }

                @Override
                public ColorTag faderColor() {
                    if(controlExists.get()) {
                        return CONTROL_TAGS[j];
                    } else {
                        return ColorTag.NULL_COLOR;
                    }
                }
            };
            controls[i] = controlPage.getParameter(i);
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        ArrayList<HardwareBinding> list = new ArrayList<>();

        // Enable faders
        surface.setupFaders(true, false, 45);

        // Bind the scene actions and lights
        bindMixerModeIndicator(surface, list, 3, new ColorTag(0xf3, 0xb3, 0x9f));

        for(int i = 0; i < 8; i++) {
            Fader controlFader = surface.faders()[i];
            controlFader.light().state().setValue(faders[i]);
            list.add(controls[i].addBinding(controlFader.fader()));
        }

        return list;
    }
}
