package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendable;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;
import io.github.jengamon.novation.surface.ihls.BasicColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public class DrumPadMode extends AbstractMode {
    private static class DrumPadLight extends InternalHardwareLightState implements SessionSendable {
        private boolean mHasContent;
        private Color mPadColor;
        private PlayingNote[] mPlayingNotes;
        private int mScrollPosition;
        private int mID;

        private DrumPadLight(int id) {
            mID = id;
            mHasContent = false;
            mPadColor = Color.nullColor();
            mPlayingNotes = new PlayingNote[0];
            mScrollPosition = 0;
        }

        private ColorTag getColorTag() {
            ColorTag color = Utils.toTag(mPadColor);
            if(mHasContent) {
                return color;
            } else {
                return null;
            }
        }

        @Override
        public HardwareLightVisualState getVisualState() {
            ColorTag tag = getColorTag();
            if(tag != null) {
                return HardwareLightVisualState.createForColor(tag.toBitwigColor());
            } else {
                return HardwareLightVisualState.createForColor(Color.nullColor());
            }
        }

        @Override
        public boolean equals(Object obj) {
            if(obj.getClass() != DrumPadLight.class) return false;
            DrumPadLight light = (DrumPadLight)obj;
            System.out.println("COMPARED " + this + " to " + obj);
            return Objects.equals(getColorTag(), light.getColorTag()) && mID == + light.mID && mScrollPosition == light.mScrollPosition;
        }

        @Override
        public void send(Session session) {
            ColorTag tag = getColorTag();
            if(tag != null) {
                session.sendMidi(0x98, mID + mScrollPosition, tag.selectNovationColor());
            } else {
                session.sendMidi(0x98, mID + mScrollPosition, 0);
            }
        }
    }

    private static class PlayAction implements DoubleConsumer, Supplier<String> {
        private NoteInput mNoteOut;
        private int mScroll;
        private boolean mHasContent;
        private int mOffset;
        private int mChannel;

        private PlayAction(NoteInput noteOut, int offset) {
            mNoteOut = noteOut;
            mOffset = offset;
            mChannel = 0;
            mScroll = 0;
            mHasContent = false;
        }

        @Override
        public String get() {
            return "Play MIDI note " + (mScroll + mOffset);
        }

        @Override
        public void accept(double value) {
            System.out.println(get() + " " + mHasContent + " " + value);
            if(mHasContent) {
                mNoteOut.sendRawMidiEvent(0x90 | (0xF & mChannel), mScroll + mOffset, (int)Math.round(value * 127));
            }
        }
    }

    private static class ReleaseAction implements Runnable, Supplier<String> {
        private NoteInput mNoteOut;
        private int mScroll;
        private int mOffset;
        private int mChannel;

        private ReleaseAction(NoteInput noteOut, int offset) {
            mNoteOut = noteOut;
            mOffset = offset;
            mChannel = 0;
            mScroll = 0;
        }

        @Override
        public String get() {
            return "Release MIDI note " + (mScroll + mOffset);
        }

        @Override
        public void run() {
            System.out.println(get());
            mNoteOut.sendRawMidiEvent(0x80 | (0xF & mChannel), mScroll + mOffset, 0);
        }
    }

    private SettableIntegerValue mScrollPosition;
    private HardwareActionBindable[] mPlayNote;
    private HardwareActionBindable[] mReleaseNote;
    private DrumPadLight[] mDrumPadLights;

    public DrumPadMode(ControllerHost host, Session session, HardwareSurface surf, CursorDevice device) {
        BooleanValue mHasDrumPads = device.hasDrumPads();
        mHasDrumPads.addValueObserver(hdp -> {
            if(hdp) {
                session.sendSysex("0f 01");
            } else {
                session.sendSysex("0f 00");
            }
        });
        DrumPadBank mDrumBank = device.createDrumPadBank(64);
        ColorValue[] mPadColors = new ColorValue[64];
        PlayingNoteArrayValue[] mNotes = new PlayingNoteArrayValue[64];
        BooleanValue[] mHasContent = new BooleanValue[64];
        mDrumPadLights = new DrumPadLight[64];
        mScrollPosition = mDrumBank.scrollPosition();
        mPlayNote = new HardwareActionBindable[64];
        mReleaseNote = new HardwareActionBindable[64];
        for(int i = 0; i < 64; i++) {
            DrumPadLight light = new DrumPadLight(i);
            PlayAction pn = new PlayAction(session.noteInput(), i);
            ReleaseAction rn = new ReleaseAction(session.noteInput(), i);
            mScrollPosition.addValueObserver(scp -> {
                pn.mScroll = scp;
                rn.mScroll = scp;
                light.mScrollPosition = scp;
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            });
            mDrumPadLights[i] = light;
            mPadColors[i] = mDrumBank.getItemAt(i).color();
            mNotes[i] = mDrumBank.getItemAt(i).playingNotes();
            mHasContent[i] = mDrumBank.getItemAt(i).exists();
            mHasContent[i].addValueObserver(v -> {
                pn.mHasContent = v;
                light.mHasContent = v;
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            });
            mNotes[i].addValueObserver(notes -> {
                light.mPlayingNotes = notes;
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            });
            mPadColors[i].addValueObserver((r, g, b) -> {
                light.mPadColor = Color.fromRGB(r, g, b);
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            });
            mPlayNote[i] = host.createAction(pn, pn);
            mReleaseNote[i] = host.createAction(rn, rn);
        }
    }

    private boolean isInBounds(int by) {
        int nsp = mScrollPosition.get() + by;
        return nsp >= 0 && nsp <= 66;
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        int nid = surface.novation().id();
        surface.novation().light().state().setValue(new BasicColor(new ColorTag(0, 23, 45), 0x90, new int[]{0}, nid));
        for(NoteButton[] noteRow : surface.notes()) {
            for(NoteButton noteButton : noteRow) {
                noteButton.setButtonMode(NoteButton.Mode.DRUM);
                int bid = noteButton.id();
                int aid = bid - 36;
//                System.out.println("" + aid + " " + bid);
                bindings.add(noteButton.button().pressedAction().addBinding(mPlayNote[aid]));
                bindings.add(noteButton.button().releasedAction().addBinding(mReleaseNote[aid]));
                noteButton.light().state().setValue(mDrumPadLights[aid]);
            }
        }
        return bindings;
    }
}
