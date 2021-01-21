package io.github.jengamon.novation.modes.mixer;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.surface.Fader;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.state.FaderLightState;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ControlsMixer extends AbstractFaderMixerMode {
    private FixedFaderLight[] faderLights = new FixedFaderLight[8];
    private Parameter[] controls = new Parameter[8];
    private static final byte[] CONTROL_TAGS = new byte[] {
            (byte)0x05,
            (byte)0x54,
            (byte)0x0d,
            (byte)0x15,
            (byte)0x1d,
            (byte)0x25,
            (byte)0x35,
            (byte)0x39,
    };

    private class FixedFaderLight {
        private byte mColor;
        private BooleanValue mExists;
        public FixedFaderLight(LaunchpadXSurface surface, byte color, BooleanValue exists) {
            mExists = exists;
            mColor = color;

            mExists.addValueObserver(e -> redraw(surface));
        }

        public void draw(MultiStateHardwareLight light) {
            if(mExists.get()) {
                light.state().setValue(new FaderLightState(mColor));
            } else {
                light.setColor(Color.nullColor());
            }
        }
    }

    public ControlsMixer(AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                         LaunchpadXSurface surface, CursorDevice device) {
        super(mixerMode, host, transport, surface, Mode.MIXER_CONTROLS, 68);

        CursorRemoteControlsPage controlPage = device.createCursorRemoteControlsPage(8);

        for(int i = 0; i < 8; i++) {
            RemoteControl control = controlPage.getParameter(i);

            byte faderColor = CONTROL_TAGS[i];
            faderLights[i] = new FixedFaderLight(surface, faderColor, control.exists());
            controls[i] = control;
        }
    }

    @Override
    public void onDraw(LaunchpadXSurface surface) {
        super.onDraw(surface);

        drawMixerModeIndicator(surface, 3);

        Fader[] faders = surface.faders();
        for(int i = 0; i < 8; i++) {
            faderLights[i].draw(faders[i].light());
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> list = super.onBind(surface);

        // Figure out if the faders should be bipolar or not
        // WARNING Very hacky
        boolean[] bipolar = new boolean[8];
        for(int i = 0; i < bipolar.length; i++) {
            // TODO Determine if parameter is bipolar or not
            bipolar[i] = false;
        }

        // Enable faders
        surface.setupFaders(true, bipolar, 45);

        for(int i = 0; i < 8; i++) {
            Fader controlFader = surface.faders()[i];
            list.add(controls[i].addBinding(controlFader.fader()));
        }

        return list;
    }
}
