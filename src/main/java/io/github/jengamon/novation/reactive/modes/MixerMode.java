package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;
import io.github.jengamon.novation.reactive.modes.session.SessionPadAction;
import io.github.jengamon.novation.reactive.modes.session.SessionPadLight;
import io.github.jengamon.novation.reactive.modes.session.SessionPadMode;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;
import io.github.jengamon.novation.surface.ihls.BasicColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class MixerMode extends AbstractMode {
    private AtomicReference<SessionPadMode> lastRowMode = new AtomicReference<>(SessionPadMode.VOLUME);
    private HardwareActionBindable[][] mixerPads = new HardwareActionBindable[8][8];
    private SessionPadLight[][] mixerLights = new SessionPadLight[8][8];
    private HardwareActionBindable[] scenePads = new HardwareActionBindable[8];
    private MixerSceneLight[] sceneLights = new MixerSceneLight[8];

    private static class MixerSceneLight extends SessionSendableLightState {
        private AtomicReference<SessionPadMode> mPadMode;
        private SessionPadMode mTarget;
        private RangedValueSyncWrapper mBPM;
        private int mID;

        private MixerSceneLight(AtomicReference<SessionPadMode> padMode, SessionPadMode target, RangedValueSyncWrapper bpm, int id) {
            mPadMode = padMode;
            mTarget = target;
            mID = id;
            mBPM = bpm;
        }

        ColorTag getTargetColor() {
            if(mPadMode.get() == mTarget) {
                return new ColorTag(204, 97, 252);
            } else {
                return ColorTag.NULL_COLOR;
            }
        }

        @Override
        public HardwareLightVisualState getVisualState() {
            ColorTag target = getTargetColor();
            return HardwareLightVisualState.createBlinking(target.toBitwigColor(), Color.mix(target.toBitwigColor(), Color.nullColor(), 0.7), 60.0 / mBPM.get(), 60.0 / mBPM.get());
        }

        @Override
        public boolean equals(Object obj) {
            return false; // TODO Implement this.
        }

        @Override
        public void send(Session session) {
            ColorTag tag = getTargetColor();
            session.sendMidi(0xB2, mID, tag.selectNovationColor());
        }
    }

    public MixerMode(ControllerHost host, Transport transport, Session session, HardwareSurface surf, CursorDevice mDevice, TrackBank mBank) {
        int[] ids = new int[]{89, 79, 69, 59, 49, 39, 29, 19};
        SessionPadMode[] targets = new SessionPadMode[]{
                SessionPadMode.VOLUME,
                SessionPadMode.PAN,
                SessionPadMode.SENDS,
                SessionPadMode.CONTROLS,
                SessionPadMode.STOP,
                SessionPadMode.MUTE,
                SessionPadMode.SOLO,
                SessionPadMode.RECORD,
        };
        AtomicReference<SessionPadMode> otherPadMode = new AtomicReference<>(SessionPadMode.SESSION);
        RangedValueSyncWrapper bpm = new RangedValueSyncWrapper(transport.tempo().modulatedValue(), surf, host);

        for(int i = 0; i < 8; i++) {
            sceneLights[i] = new MixerSceneLight(lastRowMode, targets[i], bpm, ids[i]);
            int finalI = i;
            scenePads[i] = host.createAction(() -> {
                lastRowMode.set(targets[finalI]);
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            }, () -> "Press Scene button " + finalI);
        }
        // Setup session rows
        for(int j = 0; j < 7; j++) {
            mixerPads[j] = new HardwareActionBindable[8];
            mixerLights[j] = new SessionPadLight[8];
            for(int i = 0; i < 8; i++) {
                Track track = mBank.getItemAt(i);
                ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
                ClipLauncherSlot slot = slotBank.getItemAt(j);
                ColorSyncWrapper color = new ColorSyncWrapper(slot.color(), surf, host);
                BooleanSyncWrapper armed = new BooleanSyncWrapper(mBank.getItemAt(i).arm(), surf, host);
                BooleanSyncWrapper sceneExists = new BooleanSyncWrapper(mBank.sceneBank().getItemAt(j).exists(), surf, host);
                BooleanSyncWrapper trackEnabled = new BooleanSyncWrapper(track.isActivated(), surf, host);
                BooleanSyncWrapper isMuted = new BooleanSyncWrapper(track.mute(), surf, host);
                BooleanSyncWrapper isSoloed = new BooleanSyncWrapper(track.solo(), surf, host);
                BooleanSyncWrapper isStopped = new BooleanSyncWrapper(track.isStopped(), surf, host);
                BooleanSyncWrapper trackExists = new BooleanSyncWrapper(track.exists(), surf, host);
                int finalJ = j;
                // Extract the playback state
                Value<IntegerValueChangedCallback> playbackStateValue = new Value<IntegerValueChangedCallback>() {
                    @Override
                    public void markInterested() { /* Noop */ }
                    @Override
                    public void addValueObserver(IntegerValueChangedCallback callback) {
                        slotBank.addPlaybackStateObserver((s, st, iq) -> {
                            if(s == finalJ) { callback.valueChanged(st); }
                        });
                    }
                    @Override
                    public boolean isSubscribed() { return false; }
                    @Override
                    public void setIsSubscribed(boolean value) { }
                    @Override
                    public void subscribe() { }
                    @Override
                    public void unsubscribe() { }
                };
                Value<BooleanValueChangedCallback> isQueuedValue = new Value<BooleanValueChangedCallback>() {
                    @Override
                    public boolean isSubscribed() { return false; }
                    @Override
                    public void setIsSubscribed(boolean value) { }
                    @Override
                    public void subscribe() { }
                    @Override
                    public void unsubscribe() { }
                    @Override
                    public void markInterested() { }
                    @Override
                    public void addValueObserver(BooleanValueChangedCallback callback) {
                        slotBank.addPlaybackStateObserver((s, st, iq) -> {
                            if(s == finalJ) { callback.valueChanged(iq); }
                        });
                    }
                };
                IntegerSyncWrapper playbackState = new IntegerSyncWrapper(playbackStateValue, surf, host);
                BooleanSyncWrapper isQueued = new BooleanSyncWrapper(isQueuedValue, surf, host);
                BooleanSyncWrapper hasNoteInput = new BooleanSyncWrapper(track.sourceSelector().hasNoteInputSelected(), surf, host);
                BooleanSyncWrapper hasAudioInput = new BooleanSyncWrapper(track.sourceSelector().hasAudioInputSelected(), surf, host);
                mixerLights[j][i] = new SessionPadLight(i, j, bpm, otherPadMode, color, armed,
                        sceneExists, playbackState, isQueued, trackEnabled, isMuted, isSoloed, isStopped, trackExists,
                        hasNoteInput, hasAudioInput);
                SessionPadAction action = new SessionPadAction(i, j, otherPadMode, slot, track, trackEnabled);
                mixerPads[j][i] = host.createAction(action, action);
            }
        }

        mixerPads[7] = new HardwareActionBindable[8];
        mixerLights[7] = new SessionPadLight[8];
        for(int i = 0; i < 8; i++) {
            Track track = mBank.getItemAt(i);
            ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
            ClipLauncherSlot slot = slotBank.getItemAt(7);
            ColorSyncWrapper color = new ColorSyncWrapper(slot.color(), surf, host);
            BooleanSyncWrapper armed = new BooleanSyncWrapper(mBank.getItemAt(i).arm(), surf, host);
            BooleanSyncWrapper sceneExists = new BooleanSyncWrapper(mBank.sceneBank().getItemAt(7).exists(), surf, host);
            BooleanSyncWrapper trackEnabled = new BooleanSyncWrapper(track.isActivated(), surf, host);
            BooleanSyncWrapper trackExists = new BooleanSyncWrapper(track.exists(), surf, host);
            BooleanSyncWrapper isMuted = new BooleanSyncWrapper(track.mute(), surf, host);
            BooleanSyncWrapper isSoloed = new BooleanSyncWrapper(track.solo(), surf, host);
            BooleanSyncWrapper isStopped = new BooleanSyncWrapper(track.isStopped(), surf, host);
            int finalJ = 7;
            // Extract the playback state
            Value<IntegerValueChangedCallback> playbackStateValue = new Value<IntegerValueChangedCallback>() {
                @Override
                public void markInterested() { /* Noop */ }
                @Override
                public void addValueObserver(IntegerValueChangedCallback callback) {
                    slotBank.addPlaybackStateObserver((s, st, iq) -> {
                        if(s == finalJ) { callback.valueChanged(st); }
                    });
                }
                @Override
                public boolean isSubscribed() { return false; }
                @Override
                public void setIsSubscribed(boolean value) { }
                @Override
                public void subscribe() { }
                @Override
                public void unsubscribe() { }
            };
            Value<BooleanValueChangedCallback> isQueuedValue = new Value<BooleanValueChangedCallback>() {
                @Override
                public boolean isSubscribed() { return false; }
                @Override
                public void setIsSubscribed(boolean value) { }
                @Override
                public void subscribe() { }
                @Override
                public void unsubscribe() { }
                @Override
                public void markInterested() { }
                @Override
                public void addValueObserver(BooleanValueChangedCallback callback) {
                    slotBank.addPlaybackStateObserver((s, st, iq) -> {
                        if(s == finalJ) { callback.valueChanged(iq); }
                    });
                }
            };
            IntegerSyncWrapper playbackState = new IntegerSyncWrapper(playbackStateValue, surf, host);
            BooleanSyncWrapper isQueued = new BooleanSyncWrapper(isQueuedValue, surf, host);
            BooleanSyncWrapper hasNoteInput = new BooleanSyncWrapper(track.sourceSelector().hasNoteInputSelected(), surf, host);
            BooleanSyncWrapper hasAudioInput = new BooleanSyncWrapper(track.sourceSelector().hasAudioInputSelected(), surf, host);
            mixerLights[7][i] = new SessionPadLight(i, 7, bpm, lastRowMode, color, armed, sceneExists, playbackState,
                    isQueued, trackEnabled, isMuted, isSoloed, isStopped, trackExists, hasNoteInput, hasAudioInput);
            SessionPadAction action = new SessionPadAction(i, 7, lastRowMode, slot, track, trackEnabled);
            mixerPads[7][i] = host.createAction(action, action);
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        BasicColor novationColor = new BasicColor(new ColorTag(204, 97, 252), 0xB0, new int[]{0}, 99);
        surface.novation().light().state().setValue(novationColor);
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                NoteButton button = surface.notes()[i][j];
                button.setButtonMode(NoteButton.Mode.SESSION);
                bindings.add(button.button().pressedAction().setBinding(mixerPads[i][j]));
                button.light().state().setValue(mixerLights[i][j]);
            }
        }
        for(int i = 0; i < 8; i++) {
            LaunchpadXPad sceneButton = surface.scenes()[i];
            bindings.add(sceneButton.button().pressedAction().setBinding(scenePads[i]));
            sceneButton.light().state().setValue(sceneLights[i]);
        }
        return bindings;
    }
}
