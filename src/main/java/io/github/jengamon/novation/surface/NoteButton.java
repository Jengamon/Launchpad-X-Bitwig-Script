package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.state.PadLightState;

/**
 * Represents a pad the communicates with NoteOn events
 * These are slightly more complicated than the CC button devices as they have 2 notes,
 * a drum pad mode note and a session view note
 */
public class NoteButton extends LaunchpadXPad {
    private HardwareButton mButton;
    private AbsoluteHardwareKnob mAftertouch;

    private MultiStateHardwareLight mLight;
    private int mNote;
    private int mDPNote;

    public NoteButton(ControllerHost host, Session session, HardwareSurface surface, String name, int note, int dpnote, double x, double y) {
        mNote = note;
        mDPNote = dpnote;

        mButton = surface.createHardwareButton(name);
        mAftertouch = surface.createAbsoluteHardwareKnob("AFT" + name);

        mLight = surface.createMultiStateHardwareLight("L" + name);
        mButton.setBackgroundLight(mLight);
        mButton.setBounds(x, y, 21, 21);
        mButton.setLabel(" "); // Don't label note pads

        // Upload the state to the hardware
        mLight.state().onUpdateHardware(state -> {
            PadLightState padState = (PadLightState)state;
            if(padState != null) {
                session.sendMidi(0x90, note, padState.solid());
                session.sendMidi(0x98, dpnote, padState.solid());
                if(padState.blink() > 0) session.sendMidi(0x91, note, padState.blink());
                if(padState.blink() > 0) session.sendMidi(0x99, dpnote, padState.blink());
                if(padState.pulse() > 0) session.sendMidi(0x92, note, padState.pulse());
                if(padState.pulse() > 0) session.sendMidi(0x9A, dpnote, padState.pulse());
            }
        });
        mLight.setColorToStateFunction(color -> PadLightState.solidLight(Utils.toNovation(color)));

        MidiIn in = session.midiIn(ChannelType.DAW);

        String expr = host.midiExpressions().createIsNoteOnExpression(0, note);
        String aftExpr = host.midiExpressions().createIsPolyAftertouch(0, note);
        String drumExpr = host.midiExpressions().createIsNoteOnExpression(8, dpnote);
        String drumAftExpr = host.midiExpressions().createIsPolyAftertouch(8, dpnote);

//        System.out.println(drumExpr);
//        System.out.println(drumAftExpr);

        HardwareActionMatcher onRelease = in.createActionMatcher("status == 0x90 && data2 == 0 && data1 == " + note);
        AbsoluteHardwareValueMatcher onAfterRelease = in.createAbsoluteValueMatcher(aftExpr + " && data2 == 0", "data2", 8);
        AbsoluteHardwareValueMatcher onChannelPressure = in.createAbsoluteValueMatcher("status == 0xD0", "data1", 7);
        AbsoluteHardwareValueMatcher onVelocity = in.createNoteOnVelocityValueMatcher(0, note);
        AbsoluteHardwareValueMatcher onAftertouch = in.createPolyAftertouchValueMatcher(0, note);
        HardwareActionMatcher onDrumRelease = in.createActionMatcher("status == 0x98 && data1 == " + dpnote + " && data2 == 0");
        AbsoluteHardwareValueMatcher onDrumPolyAfterOff = in.createAbsoluteValueMatcher( drumAftExpr + " && data2 == 0", "data2", 8);
        AbsoluteHardwareValueMatcher onDrumVelocity = in.createNoteOnVelocityValueMatcher(8, dpnote);
        AbsoluteHardwareValueMatcher onDrumAftertouch = in.createPolyAftertouchValueMatcher(8, dpnote);
        AbsoluteHardwareValueMatcher onDrumChannelPressure = in.createAbsoluteValueMatcher("status == 0xD8", "data1", 7);

        mButton.setAftertouchControl(mAftertouch);
        mAftertouch.setAdjustValueMatcher(
                host.createOrAbsoluteHardwareValueMatcher(
                        host.createOrAbsoluteHardwareValueMatcher(onAftertouch, onChannelPressure),
                        host.createOrAbsoluteHardwareValueMatcher(onDrumAftertouch, onDrumChannelPressure)
                )
        );
        mButton.pressedAction().setPressureActionMatcher(
                host.createOrAbsoluteHardwareValueMatcher(onVelocity, onDrumVelocity)
        );
        mButton.releasedAction().setActionMatcher(
                host.createOrHardwareActionMatcher(onRelease, onDrumRelease)
        );
        mButton.releasedAction().setPressureActionMatcher(
                host.createOrAbsoluteHardwareValueMatcher(onAfterRelease, onDrumPolyAfterOff)
        );
    }

    @Override
    public HardwareButton button() {
        return mButton;
    }

    @Override
    public AbsoluteHardwareKnob aftertouch() {
        return mAftertouch;
    }

    @Override
    public MultiStateHardwareLight light() {
        return mLight;
    }

    @Override
    public int id() {
        return mNote;
    }

    public int drum_id() {
        return mDPNote;
    }
}
