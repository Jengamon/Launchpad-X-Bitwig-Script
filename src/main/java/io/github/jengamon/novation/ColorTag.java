package io.github.jengamon.novation;

import com.bitwig.extension.api.Color;
import io.github.jengamon.novation.internal.Session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColorTag {
    private int mRed;
    private int mGreen;
    private int mBlue;

    public static final ColorTag[] INDEX_COLORS;

    static {
        INDEX_COLORS = new ColorTag[128];
        INDEX_COLORS[0] = new ColorTag(0, 0, 0);
        INDEX_COLORS[1] = new ColorTag(179, 179, 179);
        INDEX_COLORS[2] = new ColorTag(221, 221, 221);
        INDEX_COLORS[3] = new ColorTag(255, 255, 255);
        INDEX_COLORS[4] = new ColorTag(255, 179, 179);
        INDEX_COLORS[5] = new ColorTag(255, 97, 97);
        INDEX_COLORS[6] = new ColorTag(221, 97, 97);
        INDEX_COLORS[7] = new ColorTag(179, 97, 97);

    }

    public ColorTag(int red, int green, int blue) {
        mRed = red;
        mGreen = green;
        mBlue = blue;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != ColorTag.class) return false;
        ColorTag other = (ColorTag)obj;
        return other.mRed == mRed && other.mGreen == mGreen && other.mBlue == mBlue;
    }

    @Override
    public int hashCode() {
        return mRed + mGreen + mBlue;
    }

    @Override
    public String toString() {
        return "R" + mRed + "G" + mGreen + "B" + mBlue;
    }

    public Color toBitwigColor() {
        return Color.fromRGBA255(mRed, mGreen, mBlue, (mRed + mGreen + mBlue == 0 ? 0 : 255));
    }

    /**
     * Attempts to calculate the Novation index of a color
     * @return an index that can be sent using {@link Session}
     */
    public int selectNovationColor() {
        if(mRed == 0 && mGreen == 0 && mBlue == 0) return 0;

        int closestDelta = 255 * 3;
        int closestIndex = 0;
        for(int i = 0; i < INDEX_COLORS.length; i++) {
            // If we found a perfect match, break
            if(closestDelta == 0) break;
            ColorTag ct = INDEX_COLORS[i];
            if(ct == null) continue;
            int delta = ct.mRed - mRed + ct.mGreen - mGreen + ct.mBlue - mBlue;
            if(delta < 0) delta *= -1;
            if(delta < closestDelta) {
                closestIndex = i;
                closestDelta = delta;
            }
        }
        return closestIndex;
    }
}
