package io.github.jengamon.novation.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.surface.LaunchpadXPad;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;
import io.github.jengamon.novation.surface.state.PadLightState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DrumPadMode extends AbstractMode {
    private final AtomicInteger mChannel = new AtomicInteger(0);
    private final DrumPadLight[] drumPadLights = new DrumPadLight[64];
    private final HardwareActionBindable[] mPlayNote;
    private final HardwareActionBindable[] mReleaseNote;
    private final AbsoluteHardwarControlBindable[] mAftertouchNote;
    private final AbsoluteHardwarControlBindable mChannelPressure;
    private final ArrowPadLight[] mArrowLights = new ArrowPadLight[4];
    private final HardwareActionBindable[] mArrowActions = new HardwareActionBindable[4];

    private class ArrowPadLight {
        private final int mOffset;
        private final IntegerValue mScrollPosition;
        private final ColorValue mTrackColor;
        public ArrowPadLight(LaunchpadXSurface surface, int offset, IntegerValue scrollPosition, ColorValue trackColor) {
            mOffset = offset;
            mScrollPosition = scrollPosition;
            mTrackColor = trackColor;

            mScrollPosition.addValueObserver(sp -> redraw(surface));
            mTrackColor.addValueObserver((r, g, b) -> redraw(surface));
        }

        public void draw(MultiStateHardwareLight arrowLight) {
            int testPos = mScrollPosition.get() + mOffset;
            if(testPos >= 0 && testPos < (128 - 63)) {
                arrowLight.setColor(mTrackColor.get());
            } else {
                arrowLight.setColor(Color.nullColor());
            }
        }
    }

    private class DrumPadLight {
        private final ColorValue mColor;
        private final AtomicBoolean mPlaying;
        private final BooleanValue mExists;
        private final BooleanValue mEnabled;
        public DrumPadLight(LaunchpadXSurface surface, DrumPad drumPad, AtomicBoolean playing) {
            mColor = drumPad.color();
            mPlaying = playing;
            mExists = drumPad.exists();
            mEnabled = drumPad.isActivated();

            mColor.addValueObserver((r, g, b) -> redraw(surface));
            mExists.addValueObserver(e -> redraw(surface));
            mEnabled.addValueObserver(e -> redraw(surface));
        }

        public void draw(MultiStateHardwareLight padLight) {
            if(mExists.get() && mEnabled.get()) {
                if(mPlaying.get()) {
                    padLight.state().setValue(PadLightState.solidLight(78));
                } else {
                    padLight.setColor(mColor.get());
                }
            } else {
                padLight.setColor(Color.nullColor());
            }
        }
    }

    public DrumPadMode(ControllerHost host, Session session, LaunchpadXSurface surface, CursorDevice device) {
        BooleanValue mHasDrumPads = device.hasDrumPads();
        mHasDrumPads.addValueObserver(hdp -> {
            if(hdp) {
                session.sendSysex("0f 01");
            } else {
                session.sendSysex("0f 00");
            }
        });
        int[] arrowOffsets = new int[]{16, -16, -4, 4};
        DrumPadBank mDrumBank = device.createDrumPadBank(64);
        SettableIntegerValue mScrollPosition = mDrumBank.scrollPosition();
        AtomicInteger scrollPos = new AtomicInteger(0);
        mScrollPosition.addValueObserver(scrollPos::set);

        for(int i = 0; i < 4; i++) {
            int offset = arrowOffsets[i];
            mArrowLights[i] = new ArrowPadLight(surface, offset, mScrollPosition, device.channel().color());
            mArrowActions[i] = host.createAction(() -> {
                int newPos = scrollPos.get() + offset;
                if(newPos >= 0 && newPos < (128 - 63)) {
                    mScrollPosition.inc(offset);
                }
            }, () -> "Scroll by " + offset);
        }

        mPlayNote = new HardwareActionBindable[64];
        mReleaseNote = new HardwareActionBindable[64];
        mAftertouchNote = new AbsoluteHardwarControlBindable[64];

        NoteInput noteOut = session.noteInput();

        for(int i = 0; i < 64; i++) {
            DrumPad dpad = mDrumBank.getItemAt(i);
            BooleanValue hasContent = dpad.exists();
            hasContent.markInterested();
            BooleanValue notDeactivated = dpad.isActivated();
            notDeactivated.markInterested();
            AtomicBoolean playing = new AtomicBoolean(false);

            drumPadLights[i] = new DrumPadLight(surface, dpad, playing);

            int finalI = i;
            dpad.playingNotes().addValueObserver((pns) -> {
                playing.set(Arrays.stream(pns).anyMatch((pn) -> pn.pitch() == finalI + mScrollPosition.get()));
                redraw(surface);
            });
            mPlayNote[i] = host.createAction(val -> {
                if(hasContent.get() && notDeactivated.get()) {
                    noteOut.sendRawMidiEvent(0x90 | (0xF & mChannel.get()), scrollPos.get() + finalI, (int)Math.round(val * 127));
                    playing.set(true);
                    redraw(surface);
                }
            }, () -> "Play Drum Pad " + finalI);
            mReleaseNote[i] = host.createAction(() -> {
                noteOut.sendRawMidiEvent(0x80 | (0xF & mChannel.get()), scrollPos.get() + finalI, 0);
                playing.set(false);
                redraw(surface);
            }, () -> "Release Drum Pad " + finalI);
            mAftertouchNote[i] = host.createAbsoluteHardwareControlAdjustmentTarget(val -> {
                if(hasContent.get() && notDeactivated.get()) {
                    noteOut.sendRawMidiEvent(0xA0 | (0xF & mChannel.get()), scrollPos.get() + finalI, (int)Math.round(val * 127));
                }
            });
        }

        mChannelPressure = host.createAbsoluteHardwareControlAdjustmentTarget(val ->
                noteOut.sendRawMidiEvent(0xD0 | (0xF & mChannel.get()), (int)Math.round(val * 127), 0)
        );

        long channelQueryDelay = 32L;

        host.scheduleTask(new Runnable() {
            @Override
            public void run() {
                session.sendSysex("16");
                host.scheduleTask(this, channelQueryDelay);
            }
        }, 1);
    }

    @Override
    public void onDraw(LaunchpadXSurface surface) {
        LaunchpadXPad[] arrows = surface.arrows();
        for(int i = 0; i < arrows.length; i++) {
            mArrowLights[i].draw(arrows[i].light());
        }

        for(NoteButton[] noteRow : surface.notes()) {
            for(NoteButton noteButton : noteRow) {
                int did = noteButton.drum_id() - 36;
                drumPadLights[did].draw(noteButton.light());
            }
        }
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        for(NoteButton[] noteRow : surface.notes()) {
            for(NoteButton noteButton : noteRow) {
                int did = noteButton.drum_id() - 36;
                bindings.add(noteButton.button().pressedAction().addBinding(mPlayNote[did]));
                bindings.add(noteButton.button().releasedAction().addBinding(mReleaseNote[did]));
                bindings.add(noteButton.aftertouch().addBindingWithRange(mAftertouchNote[did], 0.0, 1.0));
            }
        }

        LaunchpadXPad[] arrows = new LaunchpadXPad[] {surface.up(), surface.down(), surface.left(), surface.right()};
        for(int i = 0; i < arrows.length; i++) {
            LaunchpadXPad pad = arrows[i];
            bindings.add(pad.button().pressedAction().addBinding(mArrowActions[i]));
        }

        bindings.add(surface.channelPressure().addBindingWithRange(mChannelPressure, 0.0, 1.0));

        return bindings;
    }

    @Override
    public List<String> processSysex(byte[] sysex) {
        List<String> responses = new ArrayList<>();
        if (sysex[0] == 0x16) {
            mChannel.set(sysex[4]);
        }
        return responses;
    }
}
