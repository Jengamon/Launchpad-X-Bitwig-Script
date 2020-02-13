package io.github.jengamon.novation.surface.ihls;

import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.internal.Session;
import io.github.jengamon.novation.reactive.SessionSendable;

import java.util.Arrays;

public class BasicColor extends InternalHardwareLightState implements SessionSendable {
    private ColorTag mColor;
    private int mCommand;
    private int[] mIDs;
    private int[] mChannels;

    public BasicColor(ColorTag color, int command, int[] channels, int... ids) {
        if(channels.length != ids.length)
            throw new RuntimeException("channels and ids must have same length: " + Arrays.toString(channels) + " " + Arrays.toString(ids));
        mColor = color; mCommand = command; mIDs = ids; mChannels = channels;
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        return HardwareLightVisualState.createForColor(mColor.toBitwigColor());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || obj.getClass() != BasicColor.class) return false;
        BasicColor other = (BasicColor)obj;
        return mColor.equals(other.mColor) && Arrays.equals(mIDs, other.mIDs)  && Arrays.equals(mChannels, other.mChannels) && mCommand == other.mCommand;
    }

    @Override
    public void send(Session session) {
        for(int i = 0; i < mIDs.length; i++) {
            int id = mIDs[i];
            int channel = mChannels[i];
            session.sendMidi(mCommand | channel, id, mColor.selectNovationColor());
        }
    }
}
