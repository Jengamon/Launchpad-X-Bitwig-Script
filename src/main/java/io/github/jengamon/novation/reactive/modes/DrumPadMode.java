package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.ObjectValueChangedCallback;
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
            PlayingNoteArrayValue av = new PlayingNoteArrayValue() {
                @Override
                public PlayingNote[] get() {
                    return mPlayingNotes;
                }
                @Override
                public void markInterested() { }
                @Override
                public void addValueObserver(ObjectValueChangedCallback<PlayingNote[]> callback) { }
                @Override
                public boolean isSubscribed() {return false;}
                @Override
                public void setIsSubscribed(boolean value) { }
                @Override
                public void subscribe() { }
                @Override
                public void unsubscribe() { }
            };
            if(mHasContent) {
                if(av.isNotePlaying(mID + 36)) {
                    return new ColorTag(97, 97, 255);
                } else {
                    return color;
                }
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
                session.sendMidi(0x98, mID + 36, tag.selectNovationColor());
            } else {
                session.sendMidi(0x98, mID + 36, 0);
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
//            System.out.println(get() + " " + value);
            mNoteOut.sendRawMidiEvent(0x80 | (0xF & mChannel), mScroll + mOffset, 0);
        }
    }

    private static class AftertouchExpression implements DoubleConsumer {
        private NoteInput mNoteOut;
        private int mScroll;
        private int mOffset;
        private int mChannel;
        private boolean mHasContent;

        private AftertouchExpression(NoteInput noteOut, int offset) {
            mNoteOut = noteOut;
            mOffset = offset;
            mScroll = 0;
            mChannel = 0;
            mHasContent = false;
        }

        @Override
        public void accept(double value) {
            if(mHasContent) {
                System.out.println(value);
                mNoteOut.sendRawMidiEvent(0xA0 | (0xF & mChannel), mScroll + mOffset, ((int)(value * 127) & 0xFF));
            }
        }
    }

    private static class ArrowLight extends InternalHardwareLightState implements SessionSendable {
        private int mScrollPosition;
        private int mOffset;
        private int mID;
        private static final ColorTag ARROW_COLOR = new ColorTag(0x73, 0x98, 0x14);

        private ArrowLight(int id, int offset) {
            mScrollPosition = 0;
            mOffset = offset;
            mID = id;
        }

        private boolean isInRange() {
            int val = mScrollPosition + mOffset;
            return val >= 0 && val <= 66;
        }

        @Override
        public HardwareLightVisualState getVisualState() {
            if(isInRange()) {
                return HardwareLightVisualState.createForColor(ARROW_COLOR.toBitwigColor());
            } else {
                return HardwareLightVisualState.createForColor(Color.nullColor());
            }
        }

        @Override
        public boolean equals(Object obj) {
            if(obj.getClass() != ArrowLight.class) return false;
            ArrowLight light = (ArrowLight)obj;
            return mScrollPosition == light.mScrollPosition && mOffset == light.mOffset;
        }

        @Override
        public void send(Session session) {
            if(isInRange()) {
                session.sendMidi(0xB0, mID, ARROW_COLOR.selectNovationColor());
            } else {
                session.sendMidi(0xB0, mID, 0);
            }
        }
    }

    private static class ShiftAction implements Runnable, Supplier<String> {
        private SettableIntegerValue mScrollPosition;
        private int mOffset;
        private ShiftAction(SettableIntegerValue sp, int offset) {
            mScrollPosition = sp;
            mOffset = offset;
        }

        private boolean isInRange() {
            int val = mScrollPosition.get() + mOffset;
            return val >= 0 && val <= 66;
        }

        @Override
        public String get() {
            return "Shifts drum pad bank by " + mOffset + " pads";
        }

        @Override
        public void run() {
            if(isInRange()) {
                mScrollPosition.inc(mOffset);
            }
        }
    }

    private SettableIntegerValue mScrollPosition;
    private PlayAction[] mPlayAction;
    private ReleaseAction[] mReleaseAction;
    private AftertouchExpression[] mAftertouchExpr;
    private HardwareActionBindable[] mPlayNote;
    private HardwareActionBindable[] mReleaseNote;
    private AbsoluteHardwarControlBindable[] mAftertouchNote;
    private DrumPadLight[] mDrumPadLights;

    private HardwareActionBindable mUpAction;
    private ArrowLight mUpLight;
    private HardwareActionBindable mDownAction;
    private ArrowLight mDownLight;

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
        mPlayAction = new PlayAction[64];
        mReleaseAction = new ReleaseAction[64];
        mAftertouchExpr = new AftertouchExpression[64];
        mAftertouchNote = new AbsoluteHardwarControlBindable[64];
        ShiftAction upAct = new ShiftAction(mScrollPosition, 16);
        mUpAction = host.createAction(upAct, upAct);
        mUpLight = new ArrowLight(91, 16);
        ShiftAction downAct = new ShiftAction(mScrollPosition, -16);
        mDownAction = host.createAction(downAct, downAct);
        mDownLight = new ArrowLight(92, -16);
        for(int i = 0; i < 64; i++) {
            DrumPadLight light = new DrumPadLight(i);
            PlayAction pn = new PlayAction(session.noteInput(), i);
            ReleaseAction rn = new ReleaseAction(session.noteInput(), i);
            AftertouchExpression ae = new AftertouchExpression(session.noteInput(), i);
            mPlayAction[i] = pn;
            mReleaseAction[i] = rn;
            mAftertouchExpr[i] = ae;
            mScrollPosition.addValueObserver(scp -> {
                pn.mScroll = scp;
                rn.mScroll = scp;
                ae.mScroll = scp;
                light.mScrollPosition = scp;
                mUpLight.mScrollPosition = scp;
                mDownLight.mScrollPosition = scp;
                surf.invalidateHardwareOutputState();
                host.requestFlush();
            });
            mDrumPadLights[i] = light;
            mPadColors[i] = mDrumBank.getItemAt(i).color();
            mNotes[i] = mDrumBank.getItemAt(i).playingNotes();
            mHasContent[i] = mDrumBank.getItemAt(i).exists();
            mHasContent[i].addValueObserver(v -> {
                pn.mHasContent = v;
                ae.mHasContent = v;
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
            mAftertouchNote[i] = host.createAbsoluteHardwareControlAdjustmentTarget(ae);
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
        surface.novation().light().state().setValue(new BasicColor(new ColorTag(255, 255, 255), 0xB0, new int[]{0}, nid));
        for(NoteButton[] noteRow : surface.notes()) {
            for(NoteButton noteButton : noteRow) {
                noteButton.setButtonMode(NoteButton.Mode.DRUM);
                int bid = noteButton.id();
                int aid = bid - 36;
//                System.out.println("" + aid + " " + bid);
                bindings.add(noteButton.button().pressedAction().addBinding(mPlayNote[aid]));
                bindings.add(noteButton.button().releasedAction().addBinding(mReleaseNote[aid]));
                bindings.add(noteButton.aftertouch().addBindingWithRange(mAftertouchNote[aid], 0.0, 1.0));
                noteButton.light().state().setValue(mDrumPadLights[aid]);
            }
        }
        bindings.add(surface.up().button().pressedAction().addBinding(mUpAction));
        bindings.add(surface.down().button().pressedAction().addBinding(mDownAction));
        surface.up().light().state().setValue(mUpLight);
        surface.down().light().state().setValue(mDownLight);
        return bindings;
    }
}
