package io.github.jengamon.novation;

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
    private Session mSession;

    public ModeMachine(Session session) {
        mModes = new HashMap<>();
        mBindings = new ArrayList<>();
        mMode = Mode.UNKNOWN;
        mSession = session;
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
        AbstractMode modus = mModes.get(mode);
        mBindings = modus.onBind(surface);
        modus.finishedBind(mSession);
    }

    public void sendSysex(byte[] message) {
        List<String> responses = mModes.get(mMode).processSysex(message);
        for(String response : responses) {
            mSession.sendSysex(response);
        }
    }
}
