package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.controller.api.HardwareBinding;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMode {
    public abstract List<HardwareBinding> onBind(LaunchpadXSurface surface);
    public List<String> processSysex(byte[] sysex) { return new ArrayList<>(); }
    public void finishedBind(Session session) {}
}
