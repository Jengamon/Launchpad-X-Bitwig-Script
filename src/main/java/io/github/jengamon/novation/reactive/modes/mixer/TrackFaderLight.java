package io.github.jengamon.novation.reactive.modes.mixer;

import io.github.jengamon.novation.ColorTag;
import io.github.jengamon.novation.Utils;
import io.github.jengamon.novation.reactive.FaderSendable;
import io.github.jengamon.novation.reactive.atomics.BooleanSyncWrapper;
import io.github.jengamon.novation.reactive.atomics.ColorSyncWrapper;

public class TrackFaderLight extends FaderSendable {
    protected BooleanSyncWrapper mTrackExists;
    protected ColorSyncWrapper mTrackColor;

    public TrackFaderLight(BooleanSyncWrapper trackExists, ColorSyncWrapper trackColor) {
        mTrackExists = trackExists;
        mTrackColor = trackColor;
    }

    @Override
    public ColorTag faderColor() {
        if(mTrackExists.get()) {
            ColorTag color = Utils.toTag(mTrackColor.get());
            if(color.equals(ColorTag.NULL_COLOR)) {
                return new ColorTag(0xff, 0xff, 0xff);
            } else {
                return color;
            }
        } else {
            return ColorTag.NULL_COLOR;
        }
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }
}
