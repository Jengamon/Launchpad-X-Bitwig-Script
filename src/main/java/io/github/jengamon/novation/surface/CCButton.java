package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendable;
import io.github.jengamon.novation.surface.ihls.BasicColor;

/**
 * Represents a pad that communicates with the device over CC.
 */
public class CCButton implements LaunchpadXPad {
    private HardwareButton mButton;
    private MultiStateHardwareLight mLight;
    private int mCC;

    public CCButton(Session session, HardwareSurface surface, String name, int cc, double x, double y) {
        mCC = cc;
        mButton = surface.createHardwareButton(name);

        mLight = surface.createMultiStateHardwareLight("L" + name);
        mButton.setBackgroundLight(mLight);
        mButton.setBounds(x, y, 21, 21);

        mLight.onUpdateHardware(() -> {
            SessionSendable sendable = (SessionSendable)mLight.state().currentValue();
            if(sendable != null) {
                sendable.send(session);
            }
        });

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
    public void resetColor() {
        mLight.state().setValue(new BasicColor(ColorTag.NULL_COLOR, 0xB0, new int[]{0}, mCC));
    }

    @Override
    public int id() {
        return mCC;
    }
}
