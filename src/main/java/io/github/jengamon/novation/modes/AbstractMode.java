package io.github.jengamon.novation.modes;

import com.bitwig.extension.controller.api.HardwareBinding;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMode {
    protected ModeMachine mModeMachine;
    private Mode mTarget;

    public final void onInit(ModeMachine machine, Mode target) {
        mModeMachine = machine;
        mTarget = target;
    }

    protected final void redraw(LaunchpadXSurface surface) {
        if(mModeMachine.mode() == mTarget) {
            mModeMachine.redraw(surface);
        }
    }

    public abstract List<HardwareBinding> onBind(LaunchpadXSurface surface);
    public void onDraw(LaunchpadXSurface surface) {}
    public List<String> processSysex(byte[] sysex) { return new ArrayList<>(); }
    public void finishedBind(Session session) {}
}
