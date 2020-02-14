package io.github.jengamon.novation;

import com.bitwig.extension.api.Color;
import io.github.jengamon.novation.internal.Session;

import java.util.*;

public class ColorTag {
    private int mRed;
    private int mGreen;
    private int mBlue;

    public static final Map<String, Integer> INDEX_COLORS = new HashMap<>();
    public static final ColorTag NULL_COLOR = new ColorTag(0, 0, 0);

    static {
        INDEX_COLORS.put("000000", 0);

        // Bitwig colors (approximations)
        INDEX_COLORS.put("ff5706", 0x54);
        INDEX_COLORS.put("d99d10", 0x3D);
        INDEX_COLORS.put("545454", 0x75);
        INDEX_COLORS.put("7a7a7a", 0x76);
        INDEX_COLORS.put("c9c9c9", 0x77);
        INDEX_COLORS.put("8689ac", 0x74);
        INDEX_COLORS.put("a37943", 0x3D);
        INDEX_COLORS.put("c69f70", 0x7E);
        INDEX_COLORS.put("00a694", 0x41);
        INDEX_COLORS.put("5761c6", 0x2D);
        INDEX_COLORS.put("848ae0", 0x2C);
        INDEX_COLORS.put("9549cb", 0x36);
        INDEX_COLORS.put("bc76f0", 0x35);
        INDEX_COLORS.put("0099d9", 0x27);
        INDEX_COLORS.put("44c8ff", 0x25);
        INDEX_COLORS.put("43d2b9", 0x21);
        INDEX_COLORS.put("009d47", 0x1B);
        INDEX_COLORS.put("3ebb62", 0x19);
        INDEX_COLORS.put("d93871", 0x39);
        INDEX_COLORS.put("e16691", 0x38);
        INDEX_COLORS.put("d92e24", 0x6A);
        INDEX_COLORS.put("ec6157", 0x6B);
        INDEX_COLORS.put("ff833e", 0x6C);
        INDEX_COLORS.put("e4b74e", 0x3E);
        INDEX_COLORS.put("739814", 0x13);
        INDEX_COLORS.put("a0c04c", 0x11);
        INDEX_COLORS.put("808080", 0x1);
        INDEX_COLORS.put("7f7f7f", 0x1); // Bitwig "default" color

        // Actual Hex
        INDEX_COLORS.put("ffffff", 0x3);
        INDEX_COLORS.put("ff6161", 0x5);
        INDEX_COLORS.put("dd6161", 0x6);
        INDEX_COLORS.put("b36161", 0x7);
        INDEX_COLORS.put("ffff61", 0xd);
        INDEX_COLORS.put("61ff61", 0x15);
        INDEX_COLORS.put("61ffcc", 0x1d);
        INDEX_COLORS.put("61eeff", 0x25);
        INDEX_COLORS.put("6161ff", 0x2d);
        INDEX_COLORS.put("ff61ff", 0x35);
        INDEX_COLORS.put("ff61c2", 0x39);
        INDEX_COLORS.put("cc61fc", 0x51);
        INDEX_COLORS.put("a17661", 0x53);
        INDEX_COLORS.put("ffa161", 0x54);
        INDEX_COLORS.put("aa6161", 0x79);
        INDEX_COLORS.put("f3ee61", 0x7c);
        INDEX_COLORS.put("b3a161", 0x7d);
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
        return INDEX_COLORS.computeIfAbsent(Utils.toHexString((byte)mRed, (byte)mGreen, (byte)mBlue), key -> {
           System.out.println("Missing color for " + key);
           return 0;
        });
    }
}
