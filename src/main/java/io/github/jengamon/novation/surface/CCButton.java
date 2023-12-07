package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.state.PadLightState;

/**
 * Represents a pad that communicates with the device over CC.
 */
public class CCButton extends LaunchpadXPad {
    private final HardwareButton mButton;
    private final MultiStateHardwareLight mLight;
    private final int mCC;

    public CCButton(Session session, HardwareSurface surface, String name, int cc, double x, double y) {
        mCC = cc;
        mButton = surface.createHardwareButton(name);

        mLight = surface.createMultiStateHardwareLight("L" + name);
        mButton.setBackgroundLight(mLight);
        mButton.setBounds(x, y, 21, 21);

        mLight.state().onUpdateHardware(state -> {
            PadLightState padState = (PadLightState)state;
            if(padState != null) {
                session.sendMidi(0xB0, cc, padState.solid());
                if(padState.blink() > 0) session.sendMidi(0xB1, cc, padState.blink());
                if(padState.pulse() > 0) session.sendMidi(0xB2, cc, padState.pulse());
            }
        });
        mLight.setColorToStateFunction(color -> PadLightState.solidLight(Utils.toNovation(color)));

        MidiIn in = session.midiIn(ChannelType.DAW);

//        HardwareActionMatcher onPress = in.createActionMatcher("status == 0xB0 && data2 == 127 && data1 == " + cc);
        HardwareActionMatcher onRelease = in.createCCActionMatcher(0, cc, 0);
        AbsoluteHardwareValueMatcher onVelocity = in.createAbsoluteValueMatcher("status == 0xB0 && data2 > 0 && data1 == " + cc, "data2", 7);

//        mButton.pressedAction().setActionMatcher(onPress);
        mButton.pressedAction().setPressureActionMatcher(onVelocity);
        mButton.releasedAction().setActionMatcher(onRelease);
    }

    @Override
    public HardwareButton button() { return mButton; }

    @Override
    public AbsoluteHardwareKnob aftertouch() {
        return null;
    }

    @Override
    public MultiStateHardwareLight light() { return mLight; }

    @Override
    public int id() {
        return mCC;
    }
}
