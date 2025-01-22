package io.github.jengamon.novation.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.modes.session.ArrowPadLight;
import io.github.jengamon.novation.modes.session.SessionPadLight;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSessionMixerMode extends AbstractMixerMode {
    private final SessionPadLight[][] padLights = new SessionPadLight[7][8];
    private final HardwareActionBindable[][] padActions = new HardwareActionBindable[7][8];
    private final HardwareActionBindable[][] padReleaseActions = new HardwareActionBindable[7][8];
    private final ArrowPadLight[] arrowLights = new ArrowPadLight[4];
    private final HardwareBindable[] arrowActions;

    public AbstractSessionMixerMode(AtomicReference<Mode> mixerMode, ControllerHost host,
                                    Transport transport, LaunchpadXSurface surface, TrackBank bank, Mode targetMode, int modeColor, AtomicBoolean launchAlt) {
        super(mixerMode, host, transport, surface, targetMode, modeColor);

        // Setup pad lights and buttons
        /*
        The indicies map the pad out as
        0,0.......0,7
        1,0.......1,7
        .          .
        .          .
        .          .
        7,0.......7,7

        since we want scenes to go down, we simply mark the indicies as (scene, track)
         */
        for(int scene = 0; scene < 7; scene++) {
            padActions[scene] = new HardwareActionBindable[8];
            padLights[scene] = new SessionPadLight[8];

            for(int trk = 0; trk < 8; trk++) {
                Track track = bank.getItemAt(trk);
                ClipLauncherSlotBank slotBank = track.clipLauncherSlotBank();
                ClipLauncherSlot slot = slotBank.getItemAt(scene);

                final int finalScene = scene;
                final int finalTrk = trk;
                padLights[scene][trk] = new SessionPadLight(surface, slot, track, mBPM, this::redraw, scene);
                padActions[scene][trk] = host.createAction(() -> {
                    if (launchAlt.get()) {
                        slot.launchAlt();
                    } else {
                        slot.launch();
                    }
                }, () -> "Press Scene " + finalScene + " Track " + finalTrk);
                padReleaseActions[scene][trk] = host.createAction(() -> {
                    if (launchAlt.get()) {
                        slot.launchReleaseAlt();
                    } else {
                        slot.launchRelease();
                    }
                }, () -> "Release Scene " + finalScene + " Track " + finalTrk);
            }
        }

        arrowActions = new HardwareActionBindable[] {
                bank.sceneBank().scrollBackwardsAction(),
                bank.sceneBank().scrollForwardsAction(),
                bank.scrollBackwardsAction(),
                bank.scrollForwardsAction()
        };

        BooleanValue[] arrowEnabled = new BooleanValue[]{
                bank.sceneBank().canScrollBackwards(),
                bank.sceneBank().canScrollForwards(),
                bank.canScrollBackwards(),
                bank.canScrollForwards()
        };

        LaunchpadXPad[] arrows = surface.arrows();
        for(int i = 0; i < arrows.length; i++) {
            arrowLights[i] = new ArrowPadLight(surface, arrowEnabled[i], mModeColor, this::redraw);
        }
    }

    protected final NoteButton[] getFinalRow(LaunchpadXSurface surface) {
        return surface.notes()[7];
    }

    @Override
    public void onDraw(LaunchpadXSurface surface) {
        super.onDraw(surface);

        LaunchpadXPad[] arrows = surface.arrows();
        for(int i = 0; i < arrows.length; i++) {
            arrowLights[i].draw(arrows[i].light());
        }

        LaunchpadXPad[][] pads = surface.notes();
        for(int i = 0; i < 7; i++) {
            for(int j = 0; j < 8; j++) {
                padLights[i][j].draw(pads[i][j].light());
            }
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = super.onBind(surface);

        for(int i = 0; i < 7; i++) {
            for(int j = 0; j < 8; j++) {
                NoteButton button = surface.notes()[i][j];
                bindings.add(button.button().pressedAction().addBinding(padActions[i][j]));
                bindings.add(button.button().releasedAction().addBinding(padReleaseActions[i][j]));
            }
        }
        LaunchpadXPad[] arrows = new LaunchpadXPad[]{surface.up(), surface.down(), surface.left(), surface.right()};
        for(int i = 0; i < 4; i++) {
            bindings.add(arrows[i].button().pressedAction().addBinding(arrowActions[i]));
        }

        return bindings;
    }

    @Override
    public void finishedBind(Session session) {
        super.finishedBind(session);
        session.sendSysex("00 00");
    }
}
