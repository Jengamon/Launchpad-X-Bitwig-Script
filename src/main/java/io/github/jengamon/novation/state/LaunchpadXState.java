package io.github.jengamon.novation.state;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;

public class LaunchpadXState {
    private LaunchpadXPad[] mArrows;
    private LaunchpadXPad mSessionModeButton;
    private LaunchpadXPad mNoteModeButton;

    public LaunchpadXState(ControllerHost host, HardwareSurface surf, Session session, RangedValue bpm) {
        // Create arrow buttons
        Colorer arrowColorer = new Colorer(4, bpm);
        mArrows = new LaunchpadXPad[4];
        String[] arrowNames = new String[]{"Up", "Down", "Left", "Right"};
        for(int i = 0; i < 4; i++) {
            mArrows[i] = createPad(host, surf, session,arrowNames[i],  arrowColorer, true,  new int[] {0, 1, 2}, i + 91, i, 13 + i * 23, 13, 20, 20);
        }

        Colorer modeButtonsColorer = new Colorer(2, bpm);
        mSessionModeButton = createPad(host, surf, session, "Session", modeButtonsColorer, true, new int[] {0, 1, 2}, 95, 0, 108, 13, 20, 20);
        mNoteModeButton = createPad(host, surf, session, "Note", modeButtonsColorer, true, new int[] {0, 1, 2}, 96, 1, 132, 13, 20 , 20);
    }

    public LaunchpadXPad[] arrows() { return mArrows; }
    public LaunchpadXPad sessionButton() { return mSessionModeButton; }
    public LaunchpadXPad noteButton() { return mNoteModeButton; }

    public void clear() {
        for(int i = 0; i < 4; i++) {
            ((MultiStateHardwareLight)arrows()[i].button().backgroundLight()).setColor(Color.nullColor());
        }
    }

    private LaunchpadXPad createPad(ControllerHost host, HardwareSurface surf, Session session, String id, Colorer colorer, boolean cc, int[] channels, int device_id, int color_id, double x, double y, double w, double h) {
        HardwareButton button = surf.createHardwareButton(id);
        button.setBounds(x, y, w, h);
        HardwareActionMatcher matcher, offMatcher;
        if(cc) {
            matcher = session.midiIn(ChannelType.DAW).createActionMatcher("status == 0xB0 && data2 > 0 && data1 == " + device_id);
            offMatcher = session.midiIn(ChannelType.DAW).createCCActionMatcher(0, device_id, 0);
        } else {
            matcher = session.midiIn(ChannelType.DAW).createActionMatcher("status == 0x90 && data2 > 0 && data1 ==" + device_id);
            offMatcher = session.midiIn(ChannelType.DAW).createActionMatcher("status == 0x90 && data2 == 0 && data1 == " + device_id);
        }
        button.pressedAction().setActionMatcher(matcher);
        button.releasedAction().setActionMatcher(offMatcher);
        MultiStateHardwareLight light = surf.createMultiStateHardwareLight(id + "Light");
        light.setBounds(x, y, w, h);
        return new LaunchpadXPad(host, session, button, light, colorer, cc, channels, device_id, color_id);
    }
}
