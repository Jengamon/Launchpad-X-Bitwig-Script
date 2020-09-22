package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.FaderSendable;

import java.util.concurrent.atomic.AtomicInteger;

public class Fader {
    private AbsoluteHardwareKnob mFader;
    private MultiStateHardwareLight mLight;
    private AtomicInteger mCC = new AtomicInteger(0);

    private MidiIn mIn;

    public Fader(ControllerHost host, Session session, HardwareSurface surface, String name, double x, double y) {
        mFader = surface.createAbsoluteHardwareKnob(name);
        mLight = surface.createMultiStateHardwareLight("L" + name);

        mFader.setBackgroundLight(mLight);
        mLight.state().onUpdateHardware(state -> {
            FaderSendable sendable = (FaderSendable)state;
            if(sendable != null) {
                ColorTag color = sendable.faderColor();
                session.sendMidi(0xB5, mCC.get(), color.selectNovationColor());
            }
        });

        BooleanValue isUpdating = mFader.isUpdatingTargetValue();
        isUpdating.markInterested();
        mFader.targetValue().addValueObserver(tv -> {
            boolean didUpdate = mFader.isUpdatingTargetValue().get();
            if(!didUpdate) {
                session.sendMidi(0xB4, mCC.get(), (int) Math.round(tv * 127));
            }
        });

        mFader.setBounds(x, y, 10, 10);
        mIn = session.midiIn(ChannelType.DAW);
    }

    public void resetColor() {
        mLight.state().setValue(new FaderSendable() {
            @Override
            public ColorTag faderColor() {
                return ColorTag.NULL_COLOR;
            }

            @Override
            public boolean equals(Object o) {
                return false;
            }
        });
    }

    public int id() { return mCC.get(); }
    public void setId(int cc) {
        mCC.set(cc);
        AbsoluteHardwareValueMatcher faderChange = mIn.createAbsoluteValueMatcher("status == 0xB4 && data1 == " + cc, "data2", 7);
        mFader.setAdjustValueMatcher(faderChange);
    }
    public AbsoluteHardwareKnob fader() { return mFader; }
    public MultiStateHardwareLight light() { return mLight; }
}
