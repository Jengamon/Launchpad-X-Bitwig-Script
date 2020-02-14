package io.github.jengamon.novation;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareBinding;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.modes.AbstractMode;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModeMachine {
    private Map<Mode, AbstractMode> mModes;
    private Mode mMode;
    private List<HardwareBinding> mBindings;

    public ModeMachine() {
        mModes = new HashMap<>();
        mBindings = new ArrayList<>();
        mMode = Mode.UNKNOWN;
    }

    public Mode mode() { return mMode; }

    public void register(Mode mode, AbstractMode am) {
        mModes.put(mode, am);
    }

    public void setMode(LaunchpadXSurface surface, Mode mode) {
        for(HardwareBinding binding : mBindings) {
            binding.removeBinding();
        }
        mMode = mode;
        if(!mModes.containsKey(mode)) throw new RuntimeException("Invalid mode state: " + mode);
        surface.clear();
        mBindings = mModes.get(mode).onBind(surface);
    }

    public void sendSysex(Session session, byte[] message) {
        List<String> responses = mModes.get(mMode).processSysex(message);
        for(String response : responses) {
            session.sendSysex(response);
        }
    }
}
