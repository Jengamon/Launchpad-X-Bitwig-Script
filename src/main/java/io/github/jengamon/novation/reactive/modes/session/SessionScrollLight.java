package io.github.jengamon.novation.reactive.modes.session;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;

public class SessionScrollLight extends SessionSendableLightState {
    private int mID;
    private int mScrollOffset;
    private IntegerSyncWrapper mScroll;
    private IntegerSyncWrapper mCount;
    private ColorTag mColor;
    private int mBankSize;

    public SessionScrollLight(int id, int offset, int bankSize, IntegerSyncWrapper scroll, IntegerSyncWrapper count, ColorTag color) {
        mID = id;
        mScrollOffset = offset;
        mScroll = scroll;
        mCount = count;
        mColor = color;
        mBankSize = bankSize;
    }

    ColorTag calculateColor() {
        int target = mScroll.get() + mScrollOffset;
        if(target >= 0 && target <= mCount.get() - mBankSize) {
            return mColor;
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
    public boolean equals(Object obj) {
        if(obj == null || obj.getClass() != SessionScrollLight.class) return false;
        // TODO Actually implement proper comparison
        return false;
    }

    @Override
    public void send(Session session) {
        ColorTag color = calculateColor();
        session.sendMidi(0xB0, mID, color.selectNovationColor());
    }
}
