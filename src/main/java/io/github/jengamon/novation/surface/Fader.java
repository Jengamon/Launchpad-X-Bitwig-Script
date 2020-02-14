package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.FaderSendable;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;

public class Fader {
    private AbsoluteHardwareKnob mFader;
    private MultiStateHardwareLight mLight;
    private int mCC;

    public Fader(ControllerHost host, Session session, HardwareSurface surface, String name, int cc, double x, double y) {
        mCC = cc;

        mFader = surface.createAbsoluteHardwareKnob(name);
        mLight = surface.createMultiStateHardwareLight("L" + name);

        mFader.setBackgroundLight(mLight);
        mLight.state().onUpdateHardware(state -> {
            FaderSendable sendable = (FaderSendable)state;
            if(sendable != null) {
                ColorTag color = sendable.faderColor();
                session.sendMidi(0xB5, cc, color.selectNovationColor());
            }
        });

        BooleanSyncWrapper isUpdating = new BooleanSyncWrapper(mFader.isUpdatingTargetValue(), surface, host);
        mFader.targetValue().addValueObserver(tv -> {
            if(!isUpdating.get()) {
                session.sendMidi(0xB4, cc, (int)Math.round(tv * 127));
            }
        });

        mFader.disableTakeOver();

        mFader.setBounds(x, y, 10, 10);

        MidiIn in = session.midiIn(ChannelType.DAW);

        AbsoluteHardwareValueMatcher faderChange = in.createAbsoluteCCValueMatcher(4, cc);

        mFader.setAdjustValueMatcher(faderChange);
    }

    public int id() { return mCC; }
    public AbsoluteHardwareKnob fader() { return mFader; }
    public MultiStateHardwareLight light() { return mLight; }
}
