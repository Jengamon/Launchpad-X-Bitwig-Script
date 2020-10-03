package io.github.jengamon.novation.surface;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.state.FaderLightState;

import java.util.concurrent.atomic.AtomicInteger;

public class Fader {
    private HardwareSlider mFader;
    private MultiStateHardwareLight mLight;
    private AtomicInteger mCC = new AtomicInteger(0);

    private MidiIn mIn;

    public Fader(Session session, HardwareSurface surface, String name, double x, double y) {
        mFader = surface.createHardwareSlider(name);
        mLight = surface.createMultiStateHardwareLight("L" + name);

        mFader.setBackgroundLight(mLight);
        mLight.state().onUpdateHardware(state -> {
            FaderLightState faderState = (FaderLightState)state;
            if(faderState != null) {
                session.sendMidi(0xB5, mCC.get(), faderState.solid());
            }
        });
        mLight.setColorToStateFunction(color -> new FaderLightState(Utils.toNovation(color)));

        BooleanValue isUpdating = mFader.isUpdatingTargetValue();
        isUpdating.markInterested();
        mFader.targetValue().addValueObserver(tv -> {
            boolean didUpdate = isUpdating.get();
//            System.out.println("DU>" + didUpdate);
            if(!didUpdate) {
                session.sendMidi(0xB4, mCC.get(), (int) Math.round(tv * 127));
            }
        });

        mFader.setBounds(x, y, 10, 23);
        mIn = session.midiIn(ChannelType.DAW);
    }

    public void resetColor() {
        mLight.setColor(Color.nullColor());
    }

    public int id() { return mCC.get(); }
    public void setId(int cc) {
        mCC.set(cc);
        AbsoluteHardwareValueMatcher faderChange = mIn.createAbsoluteCCValueMatcher(4, cc);
        mFader.setAdjustValueMatcher(faderChange);
    }
    public HardwareSlider fader() { return mFader; }
    public MultiStateHardwareLight light() { return mLight; }
}
