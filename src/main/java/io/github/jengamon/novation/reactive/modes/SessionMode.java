package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.RangedValueSyncWrapper;
import io.github.jengamon.novation.reactive.modes.session.SessionPadLight;
import io.github.jengamon.novation.reactive.modes.session.SessionScrollLight;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;

import java.util.ArrayList;
import java.util.List;

public class SessionMode extends AbstractMode {
    private TrackBank mBank;

    private static class SceneLight extends SessionSendableLightState {
        private RangedValueSyncWrapper mBPM;
        private ColorSyncWrapper mSceneColor;
        private BooleanSyncWrapper mExists;
        private BooleanSyncWrapper mPulse;
        private int mID;

        private SceneLight(int id, RangedValueSyncWrapper bpm, ColorSyncWrapper sceneColor, BooleanSyncWrapper exists, BooleanSyncWrapper pulse) {
            mBPM = bpm;
            mID = id;
            mSceneColor = sceneColor;
            mExists = exists;
            mPulse = pulse;
        }

        @Override
        public HardwareLightVisualState getVisualState() {
            if(mExists.get()) {
                if(mPulse.get()) {
                    return HardwareLightVisualState.createBlinking(mSceneColor.get(), Color.mix(mSceneColor.get(), Color.nullColor(), 0.7), 60.0 / mBPM.get(), 60.0 / mBPM.get());
                } else {
                    return HardwareLightVisualState.createForColor(mSceneColor.get());
                }
            } else {
                return HardwareLightVisualState.createForColor(Color.nullColor());
            }
        }

        // TODO Comparisons
        @Override
        public boolean equals(Object obj) {
            if(obj == null || obj.getClass() != SceneLight.class) return false;
            SceneLight light = (SceneLight)obj;
            return mExists.get() == light.mExists.get() && mSceneColor.get() == light.mSceneColor.get() && mBPM.get() == light.mBPM.get();
        }

        @Override
        public void send(Session session) {
            if(mExists.get()) {
                session.sendMidi(0xB0 | (mPulse.get() ? 2 : 0), mID, Utils.toTag(mSceneColor.get()).selectNovationColor());
            } else {
                session.sendMidi(0xB0, mID, 0);
            }
        }
    }

    private HardwareActionBindable[] sceneLaunchActions;
    private SceneLight[] sceneLights;
    private SessionPadLight[][] padLights;
    private HardwareActionBindable[][] padActions;
    private SessionScrollLight[] arrowLights;
    private HardwareBindable[] arrowActions;

    public SessionMode(TrackBank bank, Transport transport, HardwareSurface surf, ControllerHost host, BooleanSyncWrapper pulseSessionPads) {
        mBank = bank;
        sceneLaunchActions = new HardwareActionBindable[8];
        sceneLights = new SceneLight[8];
        int[] ids = new int[]{89, 79, 69, 59, 49, 39, 29, 19};
        RangedValueSyncWrapper bpm = new RangedValueSyncWrapper(transport.tempo().modulatedValue(), surf, host);
        // Set up scene buttons
        for(int i = 0; i < 8; i++) {
            Scene scene = mBank.sceneBank().getItemAt(i);
            ColorSyncWrapper padColor = new ColorSyncWrapper(scene.color(), surf, host);
            BooleanSyncWrapper exists = new BooleanSyncWrapper(scene.exists(), surf, host);
            int finalI = i;
            sceneLaunchActions[i] = host.createAction(() -> {
                scene.launch();
                scene.selectInEditor();
            }, () -> "Press Scene " + finalI);
            sceneLights[i] = new SceneLight(ids[i], bpm, padColor, exists, pulseSessionPads);
        }

        // Setup pad lights and buttons
        padLights = new SessionPadLight[8][8];
        padActions = new HardwareActionBindable[8][8];
        for(int j = 0; j < 8; j++) {
            padLights[j] = new SessionPadLight[8];
            padActions[j] = new HardwareActionBindable[8];
            for(int i = 0; i < 8; i++) {
                Track track = mBank.getItemAt(i);
                ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
                ClipLauncherSlot slot = slotBank.getItemAt(j);
                ColorSyncWrapper color = new ColorSyncWrapper(slot.color(), surf, host);
                BooleanSyncWrapper armed = new BooleanSyncWrapper(mBank.getItemAt(i).arm(), surf, host);
                BooleanSyncWrapper sceneExists = new BooleanSyncWrapper(mBank.sceneBank().getItemAt(j).exists(), surf, host);
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
                /*
                The indicies map the pad out as
                0,0.......0,8
                1,0.......1,8
                .          .
                .          .
                .          .
                8,0.......8,8
                But we want
                [scene][track] displayed, so we manually transpose the arrays
                 */
                padLights[j][i] = new SessionPadLight(i, j, bpm, color, armed, sceneExists, playbackState, isQueued);
                padActions[j][i] = slot.launchAction();
            }
        }
        arrowLights = new SessionScrollLight[4];
        arrowActions = new HardwareBindable[4];
        HardwareActionBindable[] scrollActions = new HardwareActionBindable[] {
                mBank.sceneBank().scrollBackwardsAction(),
                mBank.sceneBank().scrollForwardsAction(),
                mBank.scrollBackwardsAction(),
                mBank.scrollForwardsAction()
        };
        BooleanSyncWrapper[] canScroll = new BooleanSyncWrapper[] {
                new BooleanSyncWrapper(mBank.sceneBank().canScrollBackwards(), surf, host),
                new BooleanSyncWrapper(mBank.sceneBank().canScrollForwards(), surf, host),
                new BooleanSyncWrapper(mBank.canScrollBackwards(), surf, host),
                new BooleanSyncWrapper(mBank.canScrollForwards(), surf, host)
        };
        for(int j = 91; j < 95; j++) {
            int i = j - 91;
            arrowLights[i] = new SessionScrollLight(j, canScroll[i]);
            arrowActions[i] = scrollActions[i];
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        for(int i = 0; i < 8; i++) {
            bindings.add(surface.scenes()[i].button().pressedAction().addBinding(sceneLaunchActions[i]));
            surface.scenes()[i].light().state().setValue(sceneLights[i]);
        }
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                NoteButton button = surface.notes()[i][j];
                button.setButtonMode(NoteButton.Mode.SESSION);
                bindings.add(button.button().pressedAction().addBinding(padActions[i][j]));
                button.light().state().setValue(padLights[i][j]);
            }
        }
        LaunchpadXPad[] arrows = new LaunchpadXPad[]{surface.up(), surface.down(), surface.left(), surface.right()};
        for(int i = 0; i < 4; i++) {
            bindings.add(arrows[i].button().pressedAction().addBinding(arrowActions[i]));
            arrows[i].light().state().setValue(arrowLights[i]);
        }
        return bindings;
    }

    @Override
    public void finishedBind(Session session) {
        session.sendSysex("14 00 00");
        session.sendSysex("00 00");
    }
}
