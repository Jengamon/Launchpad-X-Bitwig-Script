package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.FaderSendable;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;
import io.github.jengamon.novation.reactive.modes.session.*;
import io.github.jengamon.novation.surface.Fader;
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
    private MixerSessionPadLight[][] mixerLights = new MixerSessionPadLight[8][8];
    private HardwareActionBindable[] scenePads = new HardwareActionBindable[8];
    private MixerSceneLight[] sceneLights = new MixerSceneLight[8];
    private MixerScrollLight[] arrowLights = new MixerScrollLight[4];
    private HardwareBindable[] arrowActions = new HardwareBindable[4];

    private MixerFaderLight[] faderLights = new MixerFaderLight[8];
    private SettableRangedValue[] volumeActions = new SettableRangedValue[8];
    private SettableRangedValue[] panActions = new SettableRangedValue[8];
    private SettableRangedValue[] sendActions = new SettableRangedValue[8];
    private SettableRangedValue[] controlActions = new SettableRangedValue[8];

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

    private static class MixerFaderLight extends FaderSendable {
        private static final ColorTag[] CONTROL_TAGS = new ColorTag[] {
                new ColorTag(0xff, 0x61, 0x61),
                new ColorTag(0xff, 0xa1, 0x61),
                new ColorTag(0xff, 0xff, 0x61),
                new ColorTag(0x61, 0xff, 0x61),
                new ColorTag(0x61, 0xff, 0xcc),
                new ColorTag(0x61, 0xee, 0xff),
                new ColorTag(0xff, 0x61, 0xff),
                new ColorTag(0xff, 0x61, 0xc2),
        };

        private AtomicReference<SessionPadMode> mPadMode;
        private BooleanSyncWrapper mTrackExists;
        private BooleanSyncWrapper mSendExists;
        private BooleanSyncWrapper mControlExists;
        private ColorSyncWrapper mTrackColor;
        private int mID;

        private MixerFaderLight(int id, AtomicReference<SessionPadMode> padMode, BooleanSyncWrapper trackExists, BooleanSyncWrapper sendExists, BooleanSyncWrapper controlExists, ColorSyncWrapper trackColor) {
            mPadMode = padMode;
            mTrackColor = trackColor;
            mTrackExists = trackExists;
            mSendExists = sendExists;
            mControlExists = controlExists;
            mID = id;
        }

        @Override
        public ColorTag faderColor() {
            switch(mPadMode.get()) {
                case SENDS:
                    if(mSendExists.get()) {
                        return new ColorTag(0xff, 0xff, 0xff);
                    } else {
                        return ColorTag.NULL_COLOR;
                    }
                case VOLUME:
                case PAN:
                    if(mTrackExists.get()) {
                        return Utils.toTag(mTrackColor.get());
                    } else {
                        return ColorTag.NULL_COLOR;
                    }
                case CONTROLS:
                    if(mControlExists.get()) {
                        return CONTROL_TAGS[mID];
                    } else {
                        return ColorTag.NULL_COLOR;
                    }
                default:
                    return ColorTag.NULL_COLOR;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    private void setFaderState(LaunchpadXSurface lSurf) {
        // TODO Get the proper state to determine bipolarity and verticality
        switch(lastRowMode.get()) {
            case PAN:
                lSurf.enableFaders(lastRowMode.get(), false, true);
                break;
            case VOLUME:
            case SENDS:
            case CONTROLS:
                lSurf.enableFaders(lastRowMode.get(), true, false);
                break;
            default:
                lSurf.disableFaders();
                break;
        }
    }

    public MixerMode(ControllerHost host, Transport transport, LaunchpadXSurface lSurf, Session session, HardwareSurface surf, CursorTrack mTrack, CursorDevice mDevice, TrackBank mBank) {
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
        mTrack.position().addValueObserver(pos -> {
            lSurf.refreshFaders();
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });

        for(int i = 0; i < 8; i++) {
            sceneLights[i] = new MixerSceneLight(lastRowMode, targets[i], bpm, ids[i]);
            int finalI = i;
            scenePads[i] = host.createAction(() -> {
                lastRowMode.set(targets[finalI]);
                setFaderState(lSurf);
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            }, () -> "Press Scene button " + finalI);
        }
        // Setup session rows
        for(int j = 0; j < 7; j++) {
            mixerPads[j] = new HardwareActionBindable[8];
            mixerLights[j] = new MixerSessionPadLight[8];
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
                mixerLights[j][i] = new MixerSessionPadLight(i, j, bpm, otherPadMode, color, armed,
                        sceneExists, playbackState, isQueued, trackEnabled, isMuted, isSoloed, isStopped, trackExists,
                        hasNoteInput, hasAudioInput, lastRowMode);
                SessionPadAction action = new SessionPadAction(i, j, otherPadMode, slot, track, trackEnabled);
                mixerPads[j][i] = host.createAction(action, action);
            }
        }

        mixerPads[7] = new HardwareActionBindable[8];
        mixerLights[7] = new MixerSessionPadLight[8];
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
            mixerLights[7][i] = new MixerSessionPadLight(i, 7, bpm, lastRowMode, color, armed, sceneExists, playbackState,
                    isQueued, trackEnabled, isMuted, isSoloed, isStopped, trackExists, hasNoteInput, hasAudioInput, lastRowMode);
            SessionPadAction action = new SessionPadAction(i, 7, lastRowMode, slot, track, trackEnabled);
            mixerPads[7][i] = host.createAction(action, action);
        }

        CursorRemoteControlsPage controls = mTrack.createCursorDevice().createCursorRemoteControlsPage(8);
        for(int i = 0; i < 8; i++) {
            Parameter volume = mBank.getItemAt(i).volume();
            Parameter pan = mBank.getItemAt(i).pan();
            Parameter send = mTrack.sendBank().getItemAt(i);
            Parameter control = controls.getParameter(i);
            volumeActions[i] = volume;
            panActions[i] = pan;
            sendActions[i] = send;
            controlActions[i] = control;
            BooleanSyncWrapper trackExists = new BooleanSyncWrapper(mBank.getItemAt(i).exists(), surf, host);
            BooleanSyncWrapper sendExists = new BooleanSyncWrapper(mTrack.sendBank().getItemAt(i).exists(), surf, host);
            BooleanSyncWrapper controlExists = new BooleanSyncWrapper(controls.getParameter(i).exists(), surf, host);
            ColorSyncWrapper trackColor = new ColorSyncWrapper(mBank.getItemAt(i).color(), surf, host);
            faderLights[i] = new MixerFaderLight(i, lastRowMode, trackExists, sendExists, controlExists, trackColor);
        }

        int[] aoffsets = {-1, 1, -1, 1};
        SettableIntegerValue trackScrollPos = mBank.scrollPosition();
        SettableIntegerValue sendScrollPos = mTrack.sendBank().scrollPosition();
        SettableIntegerValue sceneScrollPos = mBank.sceneBank().scrollPosition();
        IntegerSyncWrapper trackScroll = new IntegerSyncWrapper(trackScrollPos, surf, host);
        IntegerSyncWrapper sendScroll = new IntegerSyncWrapper(sendScrollPos, surf, host);
        IntegerSyncWrapper sceneScroll = new IntegerSyncWrapper(sceneScrollPos, surf, host);
        IntegerSyncWrapper trackCount = new IntegerSyncWrapper(mBank.channelCount(), surf, host);
        IntegerSyncWrapper sendCount = new IntegerSyncWrapper(mTrack.sendBank().itemCount(), surf, host);
        IntegerSyncWrapper sceneCount = new IntegerSyncWrapper(mBank.sceneBank().itemCount(), surf, host);
        int trackBankSize = mBank.getCapacityOfBank();
        int sendBankSize = mTrack.sendBank().getCapacityOfBank();
        int sceneBankSize = mBank.sceneBank().getCapacityOfBank();
        int[] svBankSize = new int[] {sceneBankSize, sceneBankSize, trackBankSize, trackBankSize};
        SettableIntegerValue[] svPos = new SettableIntegerValue[]{sceneScrollPos, sceneScrollPos, trackScrollPos, trackScrollPos};
        IntegerSyncWrapper[] svScroll = new IntegerSyncWrapper[]{sceneScroll, sceneScroll, trackScroll, trackScroll};
        IntegerSyncWrapper[] svCount = new IntegerSyncWrapper[]{sceneCount, sceneCount, trackCount, trackCount};
        for(int j = 91; j < 95; j++) {
            int i = j - 91;
            arrowLights[i] = new MixerScrollLight(j, aoffsets[i], new ColorTag(0xff, 0xa1, 0x61),
                    trackBankSize, trackScroll, trackCount, sendBankSize, sendScroll, sendCount,
                    svBankSize[i], svScroll[i], svCount[i], lastRowMode);
            MixerScrollAction action = new MixerScrollAction(aoffsets[i], trackBankSize, trackScrollPos, trackScroll,
                    trackCount, sendBankSize, sendScrollPos, sendScroll, sendCount,
                    svBankSize[i], svPos[i], svScroll[i], svCount[i], lastRowMode);
            arrowActions[i] = host.createAction(action, action);
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        setFaderState(surface);
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
        for(int i = 0; i < 8; i++) {
            Fader volumeFader = surface.volumeFaders()[i];
            Fader panFader = surface.panFaders()[i];
            Fader sendFader = surface.sendFaders()[i];
            Fader controlFader = surface.controlFaders()[i];
            bindings.add(volumeActions[i].addBinding(volumeFader.fader()));
            volumeFader.light().state().setValue(faderLights[i]);
            bindings.add(panActions[i].addBinding(panFader.fader()));
            panFader.light().state().setValue(faderLights[i]);
            bindings.add(sendFader.fader().addBindingWithRange(sendActions[i], 0, 1));
            sendFader.light().state().setValue(faderLights[i]);
            bindings.add(controlFader.fader().addBindingWithRange(controlActions[i], 0, 1));
            controlFader.light().state().setValue(faderLights[i]);
        }
        LaunchpadXPad[] arrows = new LaunchpadXPad[]{surface.up(), surface.down(), surface.left(), surface.right()};
        for(int i = 0; i < 4; i++) {
            bindings.add(arrows[i].button().pressedAction().addBinding(arrowActions[i]));
            arrows[i].light().state().setValue(arrowLights[i]);
        }
        return bindings;
    }
}
