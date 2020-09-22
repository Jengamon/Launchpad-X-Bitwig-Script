package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class StopClipMixer extends AbstractSessionMixerMode {
    private BooleanSyncWrapper[] mTrackExists = new BooleanSyncWrapper[8];
    private BooleanSyncWrapper[] mIsStopped = new BooleanSyncWrapper[8];
    private HardwareActionBindable[] mStopAction = new HardwareActionBindable[8];
    private static final ColorTag mOnColor = new ColorTag(0xaa, 0x61, 0x61);
    private static final ColorTag mOffColor = new ColorTag(0xff, 0x61, 0x61);

    public StopClipMixer(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                         LaunchpadXSurface lSurf, HardwareSurface surf, CursorTrack _track, TrackBank bank) {
        super(machine, mixerMode, host, transport, lSurf, surf, _track, bank, Mode.MIXER_STOP);

        for(int i = 0; i < 8; i++) {
            Track track = bank.getItemAt(i);
            mTrackExists[i] = new BooleanSyncWrapper(track.exists(), surf, host);
            mIsStopped[i] = new BooleanSyncWrapper(track.isStopped(), surf, host);
            mStopAction[i] = track.stopAction();
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        ArrayList<HardwareBinding> list = new ArrayList<>();

        // Bind the scene actions and lights
        bindMixerModeIndicator(surface, list, 4, new ColorTag(0xff, 0x61, 0x61));

        // Bind the session pads and arrows
        bindSessionPadsAndArrows(surface, list);

        NoteButton[] finalRow = surface.notes()[7];
        for(int i = 0; i < 8; i++) {
            NoteButton pad = finalRow[i];
            pad.setButtonMode(NoteButton.Mode.SESSION);
            list.add(pad.button().pressedAction().addBinding(mStopAction[i]));
            pad.light().state().setValue(new ToggleLight(pad.id(), mTrackExists[i], mIsStopped[i], mOnColor, mOffColor));
        }

        return list;
    }
}
