package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareBinding;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.List;

public abstract class AbstractMode {
    public abstract List<HardwareBinding> onBind(LaunchpadXSurface surface);
    public void processSysex(byte[] sysex) { }
}
