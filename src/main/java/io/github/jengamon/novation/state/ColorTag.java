package io.github.jengamon.novation.state;

import com.bitwig.extension.api.Color;

public class ColorTag {
    private int mRed;
    private int mBlue;
    private int mGreen;

    public static final ColorTag NULL_TAG = new ColorTag(0, 0, 0);

    public ColorTag(int red, int green, int blue) {
        mRed = red; mGreen = green; mBlue = blue;
    }

    public Color asColor() {
        return Color.fromRGB255(mRed, mGreen, mBlue);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass() != ColorTag.class) return false;
        ColorTag other = (ColorTag)obj;
        if(other == null) return false;
        return other.mRed == mRed && other.mGreen == mGreen && other.mBlue == mBlue;
    }

    @Override
    public int hashCode() {
        return mRed + mBlue + mGreen;
    }

    @Override
    public String toString() {
        return "R" + mRed + "G" + mGreen + "B" + mBlue;
    }
}
