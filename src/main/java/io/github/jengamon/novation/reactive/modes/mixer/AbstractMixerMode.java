package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;
import io.github.jengamon.novation.reactive.modes.AbstractMode;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.ihls.BasicColor;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractMixerMode extends AbstractMode {
    protected RangedValueSyncWrapper mBPM;
    protected AtomicReference<Mode> mMixerMode;
    private final Mode mTargetMode;

    private static final Mode[] scenemodes = new Mode[] {
            Mode.MIXER_VOLUME,
            Mode.MIXER_PAN,
            Mode.MIXER_SEND,
            Mode.MIXER_CONTROLS,
            Mode.MIXER_STOP,
            Mode.MIXER_MUTE,
            Mode.MIXER_SOLO,
            Mode.MIXER_ARM
    };
    private HardwareActionBindable[] sceneActions = new HardwareActionBindable[8];

    public AbstractMixerMode(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host,
                             Transport transport, LaunchpadXSurface lSurf, HardwareSurface surf, CursorTrack track,
                             Mode targetMode) {
        mBPM = new RangedValueSyncWrapper(transport.tempo().modulatedValue(), surf, host);
        mMixerMode = mixerMode;
        mTargetMode = targetMode;
        /*track.position().addValueObserver(pos -> {
            lSurf.refreshFaders();
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });*/

        for(int i = 0; i < 8; i++) {
            final int j = i;
            sceneActions[i] = host.createAction(() -> {
                // mixerMode.set(scenemodes[j]);
                machine.setMode(lSurf, scenemodes[j]);
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            }, () -> "Set mode to " + scenemodes[j].toString());
        }
    }

    public void bindMixerModeIndicator(LaunchpadXSurface surface, List<HardwareBinding> bindings, int padIndex, ColorTag tag) {
        for(int i = 0; i < 8; i++) {
            LaunchpadXPad scene = surface.scenes()[i];
            bindings.add(scene.button().pressedAction().addBinding(sceneActions[i]));
            if(i == padIndex) {
                scene.light().state().setValue(new MixerModeLight(mBPM, scene.id(), tag));
            } else {
                scene.light().state().setValue(new BasicColor(new ColorTag(0x80, 0x80, 0x80), 0xB0, new int[]{0}, scene.id()));
            }
        }
    }

    @Override
    public void finishedBind(Session session) {
        session.sendSysex("14 6C 02");
        mMixerMode.set(mTargetMode);
    }
}
