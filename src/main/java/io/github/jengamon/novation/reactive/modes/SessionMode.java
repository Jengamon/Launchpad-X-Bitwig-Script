package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.HardwareBinding;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.ihls.BasicColor;

import java.util.ArrayList;
import java.util.List;

public class SessionMode extends AbstractMode {
    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        int nid = surface.novation().id();
        surface.novation().light().state().setValue(new BasicColor(new ColorTag(67, 54, 32), 0xB0, new int[]{1}, nid));
        return new ArrayList<>();
    }
}
