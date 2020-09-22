package io.github.jengamon.novation.reactive.modes.mixer;

import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Mode;
import io.github.jengamon.novation.ModeMachine;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RecordArmMixer extends AbstractSessionMixerMode {
    private static class RecordArmLight extends SessionSendableLightState {
        private BooleanSyncWrapper mTrackExists;
        private BooleanSyncWrapper mArmed;
        private BooleanSyncWrapper mHasNoteInput;
        private BooleanSyncWrapper mHasAudioInput;
        private int mID;

        public RecordArmLight(int id, BooleanSyncWrapper trackExists, BooleanSyncWrapper armed, BooleanSyncWrapper hasNoteInput,
                              BooleanSyncWrapper hasAudioInput) {
            mTrackExists = trackExists;
            mArmed = armed;
            mHasAudioInput = hasAudioInput;
            mHasNoteInput = hasNoteInput;
            mID = id;
        }

        ColorTag calculateColor() {
            if(mTrackExists.get()) {
                if(mArmed.get()) {
                    return new ColorTag(0xff, 0x61, 0x61);
                } else if(mHasNoteInput.get() || mHasAudioInput.get()) {
                    return new ColorTag(0xaa, 0x61, 0x61);
                } else {
                    return ColorTag.NULL_COLOR;
                }
            } else {
                return ColorTag.NULL_COLOR;
            }
        }

        @Override
        public HardwareLightVisualState getVisualState() {
            ColorTag color = calculateColor();
            return HardwareLightVisualState.createForColor(color.toBitwigColor());
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public void send(Session session) {
            ColorTag color = calculateColor();
            session.sendMidi(0x90, mID, color.selectNovationColor());
        }
    }

    private BooleanSyncWrapper[] mTrackExists = new BooleanSyncWrapper[8];
    private BooleanSyncWrapper[] mArmed = new BooleanSyncWrapper[8];
    private BooleanSyncWrapper[] mHasNoteInput = new BooleanSyncWrapper[8];
    private BooleanSyncWrapper[] mHasAudioInput = new BooleanSyncWrapper[8];
    private HardwareActionBindable[] mArmAction = new HardwareActionBindable[8];

    public RecordArmMixer(ModeMachine machine, AtomicReference<Mode> mixerMode, ControllerHost host, Transport transport,
                          LaunchpadXSurface lSurf, HardwareSurface surf, CursorTrack _track, TrackBank bank) {
        super(machine, mixerMode, host, transport, lSurf, surf, _track, bank, Mode.MIXER_ARM);

        for(int i = 0; i < 8; i++) {
            Track track = bank.getItemAt(i);
            mTrackExists[i] = new BooleanSyncWrapper(track.exists(), surf, host);
            mArmed[i] = new BooleanSyncWrapper(track.arm(), surf, host);
            mHasNoteInput[i] = new BooleanSyncWrapper(track.sourceSelector().hasNoteInputSelected(), surf, host);
            mHasAudioInput[i] = new BooleanSyncWrapper(track.sourceSelector().hasAudioInputSelected(), surf, host);
            mArmAction[i] = track.arm().toggleAction();
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        ArrayList<HardwareBinding> list = new ArrayList<>();

        // Bind the scene actions and lights
        bindMixerModeIndicator(surface, list, 7, new ColorTag(0xe0, 0x60, 0x5f));

        // Bind the session pads and arrows
        bindSessionPadsAndArrows(surface, list);

        // Bind the final row of pads
        NoteButton[] finalRow = surface.notes()[7];
        for(int i = 0; i < 8; i++) {
            NoteButton pad = finalRow[i];
            pad.setButtonMode(NoteButton.Mode.SESSION);
            list.add(pad.button().pressedAction().setBinding(mArmAction[i]));
            pad.light().state().setValue(new RecordArmLight(pad.id(), mTrackExists[i], mArmed[i], mHasNoteInput[i], mHasAudioInput[i]));
        }

        return list;
    }
}
