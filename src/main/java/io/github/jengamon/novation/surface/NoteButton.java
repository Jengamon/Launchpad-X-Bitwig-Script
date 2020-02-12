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

//        HardwareActionMatcher onPress = in.createActionMatcher(expr + " && data2 > 0 ");
        HardwareActionMatcher onRelease = in.createActionMatcher(expr + " && data2 == 0");
        HardwareActionMatcher onAfterRelease = in.createActionMatcher(aftExpr + " && data2 == 0");
        HardwareActionMatcher onChannelRelease = in.createActionMatcher("status == 0xD0 && event == 0");
        AbsoluteHardwareValueMatcher onVelocity = in.createNoteOnVelocityValueMatcher(0, note);
        AbsoluteHardwareValueMatcher onAftertouch = in.createPolyAftertouchValueMatcher(0, note);
        AbsoluteHardwareValueMatcher onChannelPressure = in.createAbsoluteValueMatcher("status == 0xD0", "data1", 8);
//        HardwareActionMatcher onDrumPress = in.createActionMatcher(drumExpr + " && data2 > 0 ");
        HardwareActionMatcher onDrumRelease = in.createActionMatcher(drumExpr + "&& data2 == 0");
        HardwareActionMatcher onDrumAfterRelease = in.createActionMatcher(drumAftExpr + " && data2 == 0");
        HardwareActionMatcher onDrumChannelRelease = in.createActionMatcher("status == 0xD8 && event == 0");
        AbsoluteHardwareValueMatcher onDrumChannelPressure = in.createAbsoluteValueMatcher("status == 0xD8", "data1 & 0xFF", 8);
        AbsoluteHardwareValueMatcher onDrumVelocity = in.createNoteOnVelocityValueMatcher(8, dpnote);;
        AbsoluteHardwareValueMatcher onDrumAftertouch = in.createPolyAftertouchValueMatcher(8, dpnote);

        mButton.setAftertouchControl(mAftertouch);
        mAftertouch.setAdjustValueMatcher(host.createOrAbsoluteHardwareValueMatcher(onAftertouch, onChannelPressure));
//        mButton.pressedAction().setActionMatcher(onPress);
        mButton.pressedAction().setPressureActionMatcher(onVelocity);
        mButton.releasedAction().setActionMatcher(host.createOrHardwareActionMatcher(onRelease, host.createOrHardwareActionMatcher(onAfterRelease, onChannelRelease)));

        mDrumButton.setAftertouchControl(mDrumAftertouch);
        mDrumAftertouch.setAdjustValueMatcher(host.createOrAbsoluteHardwareValueMatcher(onDrumAftertouch, onDrumChannelPressure));
//        mDrumButton.pressedAction().setActionMatcher(onDrumPress);
        mDrumButton.pressedAction().setPressureActionMatcher(onDrumVelocity);
        mDrumButton.releasedAction().setActionMatcher(host.createOrHardwareActionMatcher(onDrumRelease, host.createOrHardwareActionMatcher(onDrumAfterRelease, onDrumChannelRelease)));
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
        mLight.state().setValue(new BasicColor(ColorTag.INDEX_COLORS[0], 0x90, new int[]{0, 8}, mNote, mDPNote));
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
