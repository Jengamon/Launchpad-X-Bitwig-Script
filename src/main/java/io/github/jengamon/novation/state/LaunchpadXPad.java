package io.github.jengamon.novation.state;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareButton;
import com.bitwig.extension.controller.api.MultiStateHardwareLight;
import io.github.jengamon.novation.internal.Session;

import java.util.HashMap;
import java.util.Map;

public class LaunchpadXPad {
    private HardwareButton mButton;
    private MultiStateHardwareLight mLight;
    private static Map<ColorTag, Integer> mColorMap = new HashMap<>();

    static {
        mColorMap.put(new ColorTag(0, 0, 0), 0);
        mColorMap.put(new ColorTag(80, 80, 80), 1);
    }

    public LaunchpadXPad(ControllerHost host, Session session, HardwareButton button, MultiStateHardwareLight light, Colorer colorer, boolean cc, int[] channels, int device_id, int color_id) {
        mButton = button;
        mLight = light;
        // Setup the light hardware callback
        light.setColorToStateFunction(colorer.apply(color_id));
        mButton.setBackgroundLight(light);
        light.onUpdateHardware(() -> {
            LaunchpadXHardwareLight state = (LaunchpadXHardwareLight)light.state().currentValue();
            if(state == null) {
                return;
            }
            Integer base_index = mColorMap.computeIfAbsent(state.baseColor(), _i -> 0);
            Integer blink_index = mColorMap.computeIfAbsent(state.blinkColor(), _i -> 0);
            Integer pulse_index = mColorMap.computeIfAbsent(state.pulseColor(), _i -> 0);
            int command;
            if(cc) {
                command = 0xB0;
            } else {
                command = 0x90;
            }
            session.sendMidi(command | channels[0], device_id, base_index);
            if(blink_index != 0) session.sendMidi(command | channels[1], device_id, blink_index);
            if(pulse_index != 0) session.sendMidi(command | channels[2], device_id, pulse_index);
            host.requestFlush();
        });
    }

    public HardwareButton button() { return mButton; }
    public MultiStateHardwareLight light() { return mLight; }
}
