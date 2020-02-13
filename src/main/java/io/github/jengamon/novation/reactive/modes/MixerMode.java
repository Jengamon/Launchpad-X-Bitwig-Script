package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.controller.api.HardwareBinding;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.List;

public class MixerMode extends AbstractMode {
    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        System.out.println("SWITCHED TO MIXER!");
        return bindings;
    }
}
