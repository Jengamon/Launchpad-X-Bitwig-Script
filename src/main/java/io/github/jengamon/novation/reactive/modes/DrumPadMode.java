package io.github.jengamon.novation.reactive.modes;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.callback.ObjectValueChangedCallback;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendable;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.NotesSyncWrapper;
import io.github.jengamon.novation.surface.LaunchpadXSurface;
import io.github.jengamon.novation.surface.NoteButton;
import io.github.jengamon.novation.surface.ihls.BasicColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public class DrumPadMode extends AbstractMode {
    private static class DrumPadLight extends InternalHardwareLightState implements SessionSendable {
        private BooleanSyncWrapper mHasContent;
        private ColorSyncWrapper mPadColor;
        private NotesSyncWrapper mPlayingNotes;
        private IntegerSyncWrapper mScrollPosition;
        private int mID;

        private DrumPadLight(int id, BooleanSyncWrapper hasContent, ColorSyncWrapper padColor, IntegerSyncWrapper scroll, NotesSyncWrapper notes) {
            mID = id;
            mHasContent = hasContent;
            mPadColor = padColor;
            mPlayingNotes = notes;
            mScrollPosition = scroll;
        }

        private ColorTag getColorTag() {
            ColorTag color = Utils.toTag(mPadColor.get());
            if(mHasContent.get()) {
                if(mPlayingNotes.isNotePlaying(mID + mScrollPosition.get())) {
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
            if(obj == null || obj.getClass() != DrumPadLight.class) return false;
            DrumPadLight light = (DrumPadLight)obj;
            System.out.println("COMPARED " + this + " to " + obj);
            return Objects.equals(getColorTag(), light.getColorTag()) && mID == light.mID && mScrollPosition.get() == light.mScrollPosition.get();
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
        private IntegerSyncWrapper mScroll;
        private BooleanSyncWrapper mHasContent;
        private int mOffset;
        private AtomicInteger mChannel;

        private PlayAction(NoteInput noteOut, int offset, BooleanSyncWrapper hasContent, IntegerSyncWrapper scroll, AtomicInteger channel) {
            mNoteOut = noteOut;
            mOffset = offset;
            mChannel = channel;
            mScroll = scroll;
            mHasContent = hasContent;
        }

        @Override
        public String get() {
            return "Play MIDI note " + (mScroll.get() + mOffset);
        }

        @Override
        public void accept(double value) {
//            System.out.println(get() + " " + mHasContent + " " + value);
            if(mHasContent.get()) {
                mNoteOut.sendRawMidiEvent(0x90 | (0xF & mChannel.get()), mScroll.get() + mOffset, (int)Math.round(value * 127));
            }
        }
    }

    private static class ReleaseAction implements Runnable, Supplier<String> {
        private NoteInput mNoteOut;
        private IntegerSyncWrapper mScroll;
        private int mOffset;
        private AtomicInteger mChannel;

        private ReleaseAction(NoteInput noteOut, int offset, IntegerSyncWrapper scroll, AtomicInteger channel) {
            mNoteOut = noteOut;
            mOffset = offset;
            mChannel = channel;
            mScroll = scroll;
        }

        @Override
        public String get() {
            return "Release MIDI note " + (mScroll.get() + mOffset);
        }

        @Override
        public void run() {
//            System.out.println(get() + " " + value);
            mNoteOut.sendRawMidiEvent(0x80 | (0xF & mChannel.get()), mScroll.get() + mOffset, 0);
        }
    }

    private static class AftertouchExpression implements DoubleConsumer {
        private NoteInput mNoteOut;
        private IntegerSyncWrapper mScroll;
        private int mOffset;
        private AtomicInteger mChannel;
        private boolean mHasContent;

        private AftertouchExpression(NoteInput noteOut, int offset, IntegerSyncWrapper scroll, AtomicInteger channel) {
            mNoteOut = noteOut;
            mOffset = offset;
            mScroll = scroll;
            mChannel = channel;
            mHasContent = false;
        }

        @Override
        public void accept(double value) {
            if(mHasContent) {
                System.out.println(value);
                mNoteOut.sendRawMidiEvent(0xA0 | (0xF & mChannel.get()), mScroll.get() + mOffset, ((int)(value * 127) & 0xFF));
            }
        }
    }

    private static class ArrowLight extends InternalHardwareLightState implements SessionSendable {
        private IntegerSyncWrapper mScrollPosition;
        private int mOffset;
        private int mID;
        private static final ColorTag ARROW_COLOR = new ColorTag(0x73, 0x98, 0x14);

        private ArrowLight(int id, int offset, IntegerSyncWrapper scroll) {
            mScrollPosition = scroll;
            mOffset = offset;
            mID = id;
        }

        private boolean isInRange() {
            int val = mScrollPosition.get() + mOffset;
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
            if(obj == null || obj.getClass() != ArrowLight.class) return false;
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
        private IntegerSyncWrapper mScroll;
        private int mOffset;
        private ShiftAction(SettableIntegerValue sp, int offset, IntegerSyncWrapper scroll) {
            mScrollPosition = sp;
            mOffset = offset;
            mScroll = scroll;
        }

        private boolean isInRange() {
            int val = mScroll.get() + mOffset;
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

    private HardwareActionBindable[] mPlayNote;
    private HardwareActionBindable[] mReleaseNote;
    private AbsoluteHardwarControlBindable[] mAftertouchNote;
    private DrumPadLight[] mDrumPadLights;

    private AtomicInteger mChannel = new AtomicInteger(0);

    private HardwareActionBindable mUpAction;
    private ArrowLight mUpLight;
    private HardwareActionBindable mDownAction;
    private ArrowLight mDownLight;
    private HardwareActionBindable mLeftAction;
    private ArrowLight mLeftLight;
    private HardwareActionBindable mRightAction;
    private ArrowLight mRightLight;

    public DrumPadMode(ControllerHost host, Session session, HardwareSurface surf, CursorDevice device) {
        BooleanValue mHasDrumPads = device.hasDrumPads();
        mHasDrumPads.addValueObserver(hdp -> {
            if(hdp) {
                session.sendSysex("0f 01");
            } else {
                session.sendSysex("0f 00");
            }
            surf.invalidateHardwareOutputState();
            host.requestFlush();
        });
        DrumPadBank mDrumBank = device.createDrumPadBank(64);
        SettableIntegerValue mScrollPosition = mDrumBank.scrollPosition();
        IntegerSyncWrapper mScroll = new IntegerSyncWrapper(mScrollPosition, surf, host);
        mDrumPadLights = new DrumPadLight[64];
        mPlayNote = new HardwareActionBindable[64];
        mReleaseNote = new HardwareActionBindable[64];
        mAftertouchNote = new AbsoluteHardwarControlBindable[64];
        ShiftAction upAct = new ShiftAction(mScrollPosition, 16, mScroll);
        mUpAction = host.createAction(upAct, upAct);
        mUpLight = new ArrowLight(91, 16, mScroll);
        ShiftAction downAct = new ShiftAction(mScrollPosition, -16, mScroll);
        mDownAction = host.createAction(downAct, downAct);
        mDownLight = new ArrowLight(92, -16, mScroll);
        ShiftAction leftAct = new ShiftAction(mScrollPosition, -4, mScroll);
        mLeftAction = host.createAction(leftAct, leftAct);
        mLeftLight = new ArrowLight(93, -4, mScroll);
        ShiftAction rightAct = new ShiftAction(mScrollPosition, 4, mScroll);
        mRightAction = host.createAction(rightAct, rightAct);
        mRightLight = new ArrowLight(94, 4, mScroll);
        for(int i = 0; i < 64; i++) {
            ColorSyncWrapper mPadColors = new ColorSyncWrapper(mDrumBank.getItemAt(i).color(), surf, host);
            NotesSyncWrapper mNotes = new NotesSyncWrapper(mDrumBank.getItemAt(i).playingNotes(), surf, host);
            BooleanSyncWrapper mHasContent = new BooleanSyncWrapper(mDrumBank.getItemAt(i).exists(), surf, host);
            DrumPadLight light = new DrumPadLight(i, mHasContent, mPadColors, mScroll, mNotes);
            PlayAction pn = new PlayAction(session.noteInput(), i, mHasContent, mScroll, mChannel);
            ReleaseAction rn = new ReleaseAction(session.noteInput(), i, mScroll, mChannel);
            AftertouchExpression ae = new AftertouchExpression(session.noteInput(), i, mScroll, mChannel);
            mDrumPadLights[i] = light;
            mPlayNote[i] = host.createAction(pn, pn);
            mReleaseNote[i] = host.createAction(rn, rn);
            mAftertouchNote[i] = host.createAbsoluteHardwareControlAdjustmentTarget(ae);
        }
        session.sendSysex("16");
    }

    @Override
    public List<HardwareBinding> onBind(LaunchpadXSurface surface) {
        List<HardwareBinding> bindings = new ArrayList<>();
        int nid = surface.novation().id();
        surface.novation().light().state().setValue(new BasicColor(new ColorTag(255, 255, 255), 0xB0, new int[]{0}, nid));
        for(NoteButton[] noteRow : surface.notes()) {
            for(NoteButton noteButton : noteRow) {
                noteButton.setButtonMode(NoteButton.Mode.DRUM);
                int aid = noteButton.id() - 36;
                bindings.add(noteButton.button().pressedAction().addBinding(mPlayNote[aid]));
                bindings.add(noteButton.button().releasedAction().addBinding(mReleaseNote[aid]));
                bindings.add(noteButton.aftertouch().addBindingWithRange(mAftertouchNote[aid], 0.0, 1.0));
                noteButton.light().state().setValue(mDrumPadLights[aid]);
            }
        }
        bindings.add(surface.up().button().pressedAction().addBinding(mUpAction));
        bindings.add(surface.down().button().pressedAction().addBinding(mDownAction));
        bindings.add(surface.left().button().pressedAction().addBinding(mLeftAction));
        bindings.add(surface.right().button().pressedAction().addBinding(mRightAction));
        surface.up().light().state().setValue(mUpLight);
        surface.down().light().state().setValue(mDownLight);
        surface.left().light().state().setValue(mLeftLight);
        surface.right().light().state().setValue(mRightLight);
        return bindings;
    }

    @Override
    public List<String> processSysex(byte[] sysex) {
        List<String> responses = new ArrayList<>();
        if (sysex[0] == 0x16) {
            mChannel.set(sysex[4]);
        }
        responses.add("16");
        return responses;
    }
}
