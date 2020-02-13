package io.github.jengamon.novation.surface;

import com.bitwig.extension.controller.api.*;

public interface LaunchpadXPad {
    HardwareButton button();
    AbsoluteHardwareKnob aftertouch();
    MultiStateHardwareLight light();
    int id();
    void resetColor();
}
