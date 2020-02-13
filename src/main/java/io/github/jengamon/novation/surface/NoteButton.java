package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendable;
import io.github.jengamon.novation.surface.ihls.BasicColor;

/**
 * Represents a pad the communicates with NoteOn events
 * These are slightly more complicated than the CC button devices as they have 2 notes,
 * a drum pad mode note and a session view note
 */
public class NoteButton implements LaunchpadXPad {
    public enum Mode {
        SESSION,
        DRUM,
    }
    private HardwareButton mButton;
    private AbsoluteHardwareKnob mAftertouch;

    private HardwareButton mDrumButton;
    private AbsoluteHardwareKnob mDrumAftertouch;

    private MultiStateHardwareLight mLight;
    private int mNote;
    private int mDPNote;
    private Mode mMode = Mode.SESSION;

    public NoteButton(ControllerHost host, Session session, HardwareSurface surface, String name, int note, int dpnote, double x, double y) {
        mNote = note;
        mDPNote = dpnote;

        mButton = surface.createHardwareButton(name);
        mAftertouch = surface.createAbsoluteHardwareKnob("AFT" + name);

        mDrumButton = surface.createHardwareButton("D" + name);
        mDrumAftertouch = surface.createAbsoluteHardwareKnob("DAFT" + name);

        mLight = surface.createMultiStateHardwareLight("L" + name);
        mButton.setBackgroundLight(mLight);
        mDrumButton.setBackgroundLight(mLight);
        mButton.setBounds(x, y, 21, 21);
        mDrumButton.setBounds(x, y, 21, 21);
        mDrumButton.setLabel("");
        mButton.setLabel(""); // Don't label note pads

        // Upload the state to the hardware
        mLight.state().onUpdateHardware(state -> {
            SessionSendable sendable = (SessionSendable)state;
            if(sendable != null) {
                sendable.send(session);
            }
        });

        MidiIn in = session.midiIn(ChannelType.DAW);

        String expr = host.midiExpressions().createIsNoteOnExpression(0, note);
        String aftExpr = host.midiExpressions().createIsPolyAftertouch(0, note);
        String drumExpr = host.midiExpressions().createIsNoteOnExpression(8, dpnote);
        String drumAftExpr = host.midiExpressions().createIsPolyAftertouch(8, dpnote);

        System.out.println(drumExpr);
        System.out.println(drumAftExpr);

        HardwareActionMatcher onRelease = in.createActionMatcher("status == 0x90 && data2 == 0 && data1 == " + note);
        AbsoluteHardwareValueMatcher onAfterRelease = in.createAbsoluteValueMatcher(aftExpr + " && data2 == 0", "data2", 8);
        AbsoluteHardwareValueMatcher onChannelPressure = in.createAbsoluteValueMatcher("status == 0xD0", "data1", 7);
        AbsoluteHardwareValueMatcher onVelocity = in.createNoteOnVelocityValueMatcher(0, note);
        AbsoluteHardwareValueMatcher onAftertouch = in.createPolyAftertouchValueMatcher(0, note);
        HardwareActionMatcher onDrumRelease = in.createActionMatcher("status == 0x98 && data1 == " + dpnote + " && data2 == 0");
        AbsoluteHardwareValueMatcher onDrumPolyAfterOff = in.createAbsoluteValueMatcher( drumAftExpr + " && data2 == 0", "data2", 8);
        AbsoluteHardwareValueMatcher onDrumVelocity = in.createNoteOnVelocityValueMatcher(8, dpnote);;
        AbsoluteHardwareValueMatcher onDrumAftertouch = in.createPolyAftertouchValueMatcher(8, dpnote);
        AbsoluteHardwareValueMatcher onDrumChannelPressure = in.createAbsoluteValueMatcher("status == 0xD8", "data1", 7);

        mButton.setAftertouchControl(mAftertouch);
        mAftertouch.setAdjustValueMatcher(host.createOrAbsoluteHardwareValueMatcher(onAftertouch, onChannelPressure));
        mButton.pressedAction().setPressureActionMatcher(onVelocity);
        mButton.releasedAction().setActionMatcher(onRelease);
        mButton.releasedAction().setPressureActionMatcher(onAfterRelease);

        mDrumButton.setAftertouchControl(mDrumAftertouch);
        mDrumAftertouch.setAdjustValueMatcher(host.createOrAbsoluteHardwareValueMatcher(onDrumAftertouch, onDrumChannelPressure));
        mDrumButton.pressedAction().setPressureActionMatcher(onDrumVelocity);
        mDrumButton.releasedAction().setActionMatcher(onDrumRelease);
        mDrumButton.releasedAction().setPressureActionMatcher(onDrumPolyAfterOff);
    }

    public void setButtonMode(NoteButton.Mode mode) {
        mMode = mode;
    }

    @Override
    public HardwareButton button() {
        switch(mMode) {
            case SESSION:
                return mButton;
            case DRUM:
                return mDrumButton;
        }
        return null;
    }

    @Override
    public AbsoluteHardwareKnob aftertouch() {
        switch(mMode) {
            case SESSION:
                return mAftertouch;
            case DRUM:
                return mDrumAftertouch;
        }
        return null;
    }

    @Override
    public MultiStateHardwareLight light() {
        return mLight;
    }

    @Override
    public void resetColor() {
        mLight.state().setValue(new BasicColor(ColorTag.NULL_COLOR, 0x90, new int[]{0, 8}, mNote, mDPNote));
    }

    @Override
    public int id() {
        switch(mMode) {
            case SESSION:
                return mNote;
            case DRUM:
                return mDPNote;
        }
        return 0;
    }
}
