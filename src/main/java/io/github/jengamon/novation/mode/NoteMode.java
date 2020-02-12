package io.github.jengamon.novation.mode;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.*;
import io.github.jengamon.novation.internal.ChannelType;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.state.ArrowValue;
import io.github.jengamon.novation.state.LaunchpadXState;

import java.util.function.Supplier;

/**
 * NoteMode is a representation of the simple DrumPad mode
 */
public class NoteMode implements AbstractMode {
    private DrumPadBank mBank;
    private MidiOut mOutput;
    private SettableIntegerValue mScrollPositon;
    private BooleanValue mHasDrumPads;

    private ShiftAction[] mShifts;

    private class ShiftAction implements Runnable, Supplier<String> {
        private SettableIntegerValue mScrollPosition;
        private Runnable mRefresh;
        private int mOffset;

        ShiftAction(Runnable refresh, SettableIntegerValue scrollPosition, int by) {
            mScrollPosition = scrollPosition;
            mRefresh = refresh;
            mOffset = by;
        }

        private boolean changeInRange() {
            int value = mScrollPositon.get() + mOffset;
            return value >= 0 && value <= 66;
        }

        @Override
        public synchronized void run() {
            if(changeInRange()) {
                mScrollPosition.inc(mOffset);
                mRefresh.run();
            }
        }

        @Override
        public String get() {
            return "Shifts drum pad view by " + mOffset + " pads";
        }
    }

    public NoteMode(CursorDevice device) {
        mBank = device.createDrumPadBank(64);
        mScrollPositon = mBank.scrollPosition();
        mHasDrumPads = device.hasDrumPads();

        mScrollPositon.markInterested();
        mHasDrumPads.markInterested();
    }

    @Override
    public void onInitialize(ControllerHost host, Session session, LaunchpadXState state, Supplier<Boolean> inMode) {
        mOutput = session.midiOut(ChannelType.CUSTOM);

        Runnable refreshLights = () -> {
            for(int i = 0; i < 4; i++) {
                if(inMode.get()) {
                    boolean lightState = mShifts[i].changeInRange();
                    if(lightState) {
                        state.arrows()[i].light().setColor(Color.fromHex("50505000"));
                    } else {
                        state.arrows()[i].light().setColor(Color.nullColor());
                    }
                }
            }
            host.requestFlush();
        };

        mShifts = new ShiftAction[] {
                new ShiftAction(refreshLights, mScrollPositon, 16),
                new ShiftAction(refreshLights, mScrollPositon, -16),
                new ShiftAction(refreshLights, mScrollPositon, -4),
                new ShiftAction(refreshLights, mScrollPositon, 4)
        };

        for(int i = 0; i < 4; i++) {
            ShiftAction action = mShifts[i];
            HardwareActionBindable actionBindable = host.createAction(action, action);
            state.arrows()[i].button().pressedAction().addBinding(actionBindable);
        }

        mHasDrumPads.addValueObserver((hdp) -> {
            if(hdp) {
                session.sendSysex("0f 01");
            } else {
                session.sendSysex("0f 00");
            }
            refreshLights.run();
            host.requestFlush();
        });
    }
}
