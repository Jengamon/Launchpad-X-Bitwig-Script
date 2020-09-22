package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.callback.BooleanValueChangedCallback;
import com.bitwig.extension.callback.IntegerValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.modes.session.SessionPadLight;
import io.github.jengamon.novation.reactive.modes.session.SessionScrollLight;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSessionMixerMode extends AbstractMixerMode {
    private SessionPadLight[][] padLights = new SessionPadLight[7][8];
    private HardwareActionBindable[][] padActions = new HardwareActionBindable[7][8];
    private SessionScrollLight[] arrowLights;
    private HardwareBindable[] arrowActions;

    public AbstractSessionMixerMode(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host,
                                    Transport transport, LaunchpadXSurface lSurf, HardwareSurface surf,
                                    CursorTrack _track, TrackBank bank, Mode targetMode) {
        super(machine, mixerMode, host, transport, lSurf, surf, _track, targetMode);

        for(int j = 0; j < 7; j++) {
            padLights[j] = new SessionPadLight[8];
            padActions[j] = new HardwareActionBindable[8];
            for(int i = 0; i < 8; i++) {
                Track track = bank.getItemAt(i);
                ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
                ClipLauncherSlot slot = slotBank.getItemAt(j);
                ColorSyncWrapper color = new ColorSyncWrapper(slot.color(), surf, host);
                BooleanSyncWrapper armed = new BooleanSyncWrapper(bank.getItemAt(i).arm(), surf, host);
                BooleanSyncWrapper sceneExists = new BooleanSyncWrapper(bank.sceneBank().getItemAt(j).exists(), surf, host);
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
                padLights[j][i] = new SessionPadLight(i, j, mBPM, color, armed, sceneExists, playbackState, isQueued);
                padActions[j][i] = slot.launchAction();
            }
        }

        arrowLights = new SessionScrollLight[4];
        arrowActions = new HardwareBindable[4];
        HardwareActionBindable[] scrollActions = new HardwareActionBindable[] {
                bank.sceneBank().scrollBackwardsAction(),
                bank.sceneBank().scrollForwardsAction(),
                bank.scrollBackwardsAction(),
                bank.scrollForwardsAction()
        };
        BooleanSyncWrapper[] canScroll = new BooleanSyncWrapper[] {
                new BooleanSyncWrapper(bank.sceneBank().canScrollBackwards(), surf, host),
                new BooleanSyncWrapper(bank.sceneBank().canScrollForwards(), surf, host),
                new BooleanSyncWrapper(bank.canScrollBackwards(), surf, host),
                new BooleanSyncWrapper(bank.canScrollForwards(), surf, host)
        };
        for(int j = 91; j < 95; j++) {
            int i = j - 91;
            arrowLights[i] = new SessionScrollLight(j, canScroll[i]);
            arrowActions[i] = scrollActions[i];
        }
    }

    public void bindSessionPadsAndArrows(LaunchpadXSurface surface, List<HardwareBinding> bindings) {
        for(int i = 0; i < 7; i++) {
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
    }

    @Override
    public void finishedBind(Session session) {
        super.finishedBind(session);
        session.sendSysex("00 00");
    }
}
