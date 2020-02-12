package io.github.jengamon.novation.state;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.RangedValue;
import io.github.jengamon.novation.Utils;

import java.util.function.Function;

public class Colorer implements Function<Integer, Function<Color, InternalHardwareLightState>> {
    private Color[] mBaseColors;
    private RangedValue mBPM;
//    private int mSize;

    public Colorer(int size, RangedValue bpm) {
        if(size <= 0) {
            throw new RuntimeException("Cannot have a colorer of size 0 or below");
        }

        mBPM = bpm;

        mBaseColors = new Color[size];
        for(int i = 0; i < size; i++) {
            mBaseColors[i] = Color.nullColor();
        }
    }

    @Override
    public Function<Color, InternalHardwareLightState> apply(Integer i) {
        return color -> {
            System.out.println(Utils.printColor(color));
            Color s = Color.fromRGB(color.getRed(), color.getGreen(), color.getBlue());
            System.out.println(Utils.printColor(s));
            if(color.getAlpha() > 2.0 / 3.0) {
                return new LaunchpadXHardwareLight(Color.nullColor(), Color.nullColor(), s, mBPM);
            } else if (color.getAlpha() > 1.0 / 3.0) {
                return new LaunchpadXHardwareLight(mBaseColors[i], s, Color.nullColor(), mBPM);
            } else {
                mBaseColors[i] = s;
                return new LaunchpadXHardwareLight(s, Color.nullColor(), Color.nullColor(), mBPM);
            }
        };
    }
}
