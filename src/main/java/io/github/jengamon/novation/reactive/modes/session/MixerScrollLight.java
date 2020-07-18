package io.github.jengamon.novation.reactive.modes.session;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendableLightState;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;

import java.util.concurrent.atomic.AtomicReference;

public class MixerScrollLight extends SessionSendableLightState {
    private AtomicReference<SessionPadMode> mLastRowMode;

    // The ID this light corresponds to.
    private int mID;
    // The amount this control scrolls by in *all* modes.
    private int mScrollOffset;
    // The desired color for this button
    private ColorTag mTag;

    // Deals with track modes (VOLUME, PAN)
    private IntegerSyncWrapper mBankScroll;
    private IntegerSyncWrapper mBankCount;
    // Bank size that we are looking at.
    private int mBankSize;

    // Deals with send counts (SENDS)
    private IntegerSyncWrapper mSendScroll;
    private IntegerSyncWrapper mSendCount;
    // Bank size that we are looking at.
    private int mSendBankSize;

    // Deals with session view modes (STOP, MUTE, SOLO, RECORD)
    private IntegerSyncWrapper mSBankScroll;
    private IntegerSyncWrapper mSBankCount;
    // bank size we are looking at.
    private int mSBankSize;

    public MixerScrollLight(int id, int scrollOffset, ColorTag tag, int bankSize, IntegerSyncWrapper bankScroll, IntegerSyncWrapper bankCount,
                            int sendBankSize, IntegerSyncWrapper sendScroll, IntegerSyncWrapper sendCount,
                            int sbankSize, IntegerSyncWrapper sbankScroll, IntegerSyncWrapper sbankCount, AtomicReference<SessionPadMode> lastRowMode) {
        mID = id;
        mScrollOffset = scrollOffset;
        mTag = tag;

        mBankSize = bankSize;
        mBankScroll = bankScroll;
        mBankCount = bankCount;

        mSendBankSize = sendBankSize;
        mSendScroll = sendScroll;
        mSendCount = sendCount;

        mSBankSize = sbankSize;
        mSBankScroll = sbankScroll;
        mSBankCount = sbankCount;

        mLastRowMode = lastRowMode;
    }

    boolean isInBounds(IntegerSyncWrapper scroll, IntegerSyncWrapper count, int bankSize) {
        int target = scroll.get() + mScrollOffset;
        return target >= 0 && target <= count.get() - bankSize;
    }

    // Calculate the color to be given.
    ColorTag calculateColor() {
        switch(mLastRowMode.get()) {
            case VOLUME:
                if(mID == 93 || mID == 94) {
                    return (isInBounds(mBankScroll, mBankCount, mBankSize) ? mTag : ColorTag.NULL_COLOR);
                } else {
                    return ColorTag.NULL_COLOR;
                }
            case PAN:
                if(mID == 91 || mID == 92) {
                    return (isInBounds(mBankScroll, mBankCount, mBankSize) ? mTag : ColorTag.NULL_COLOR);
                } else {
                    return ColorTag.NULL_COLOR;
                }
            case SENDS:
                if(mID == 93 || mID == 94) {
                    return (isInBounds(mSendScroll, mSendCount, mSendBankSize) ? mTag : ColorTag.NULL_COLOR);
                } else {
                    return ColorTag.NULL_COLOR;
                }
            case STOP:
            case SOLO:
            case MUTE:
            case RECORD:
                return (isInBounds(mSBankScroll, mSBankCount, mSBankSize) ? mTag : ColorTag.NULL_COLOR);
            default:
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
        if(obj == null || obj.getClass() != MixerScrollLight.class) return false;
        // TODO Real comparisons
        return false;
    }

    @Override
    public void send(Session session) {
        ColorTag color = calculateColor();
        session.sendMidi(0xB0, mID, color.selectNovationColor());
    }
}
