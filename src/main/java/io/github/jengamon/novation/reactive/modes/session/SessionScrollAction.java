package io.github.jengamon.novation.reactive.modes.session;

import com.bitwig.extension.controller.api.SettableIntegerValue;
import io.github.jengamon.novation.reactive.atomics.IntegerSyncWrapper;

import java.util.function.Supplier;

public class SessionScrollAction implements Runnable, Supplier<String> {
    private SettableIntegerValue mScrollPos;
    private IntegerSyncWrapper mScroll;
    private IntegerSyncWrapper mCount;
    private int mOffset;
    private int mBankSize;

    public SessionScrollAction(int offset, int bankSize, SettableIntegerValue scrollPos, IntegerSyncWrapper scroll, IntegerSyncWrapper count) {
        mOffset = offset;
        mScrollPos = scrollPos;
        mScroll = scroll;
        mCount = count;
        mBankSize = bankSize;
    }

    private boolean canScroll() {
        int target = mScroll.get() + mOffset;
        return target >= 0 && target <= mCount.get() - mBankSize;
    }

    @Override
    public void run() {
        if(canScroll()) {
            mScrollPos.inc(mOffset);
        }
    }

    @Override
    public String get() {
        return "Scrolls bank position by " + mOffset;
    }
}
