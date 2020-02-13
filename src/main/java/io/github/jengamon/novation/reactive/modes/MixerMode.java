package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.modes.session.SessionPadMode;
import io.github.jengamon.novation.surface.LaunchpadXSurface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class MixerMode extends AbstractMode {
    private AtomicReference<SessionPadMode> lastRowMode = new AtomicReference<>(SessionPadMode.SESSION);
    private HardwareActionBindable[][] mixerPads = new HardwareActionBindable[8][8];
    private HardwareActionBindable[] scenePads = new HardwareActionBindable[8];
    private MixerSceneLight[] sceneLights = new MixerSceneLight[8];

    private static class MixerSceneSelector implements Runnable, Supplier<String> {
        private AtomicReference<SessionPadMode> mPadMode;
        private SessionPadMode mTarget;

        private MixerSceneSelector(AtomicReference<SessionPadMode> padMode, SessionPadMode target) {
            mPadMode = padMode;
            mTarget = target;
        }

        @Override
        public void run() {
            mPadMode.set(mTarget);
        }

        @Override
        public String get() {
            return null;
        }
    }

    private static class MixerSceneLight extends SessionSendableLightState {
        private AtomicReference<SessionPadMode> mPadMode;
        private SessionPadMode mTarget;
        private int mID;

        private MixerSceneLight(AtomicReference<SessionPadMode> padMode, SessionPadMode target, int id) {
            mPadMode = padMode;
            mTarget = target;
            mID = id;
        }

        ColorTag getTargetColor() {
            if(mPadMode.get() == mTarget) {
                return new ColorTag(255, 97, 97); // TEMP
            } else {
                return ColorTag.NULL_COLOR;
            }
        }

        @Override
        public HardwareLightVisualState getVisualState() {
            return HardwareLightVisualState.createForColor(getTargetColor().toBitwigColor());
        }

        @Override
        public boolean equals(Object obj) {
            return false; // TODO Implement this.
        }

        @Override
        public void send(Session session) {
            ColorTag tag = getTargetColor();
            session.sendMidi(0xB0, mID, tag.selectNovationColor());
        }
    }

    public MixerMode(ControllerHost host, Session session, HardwareSurface surf, TrackBank mBank) {
        int[] ids = new int[]{89, 79, 69, 59, 49, 39, 29, 19};
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {

            }
        }
        System.out.println("SWITCHED TO MIXER!");
        return bindings;
    }
}
