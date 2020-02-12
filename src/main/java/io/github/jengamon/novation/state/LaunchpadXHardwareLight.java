package io.github.jengamon.novation.state;

import com.bitwig.extension.api.Color;
import com.bitwig.extension.controller.api.HardwareLightVisualState;
import com.bitwig.extension.controller.api.InternalHardwareLightState;
import com.bitwig.extension.controller.api.RangedValue;
import io.github.jengamon.novation.Utils;

public class LaunchpadXHardwareLight extends InternalHardwareLightState {
    private ColorTag solidColor;
    private ColorTag blinkColor;
    private ColorTag pulseColor;
    private RangedValue bpm;

    public LaunchpadXHardwareLight(Color sc, Color bc, Color pc, RangedValue _bpm) {
        solidColor = Utils.toTag(sc);
        blinkColor = Utils.toTag(bc);
        pulseColor = Utils.toTag(pc);
        bpm = _bpm;
    }

    @Override
    public HardwareLightVisualState getVisualState() {
        System.out.println(pulseColor + " " + blinkColor + " " + solidColor);
        System.out.println(bpm.getRaw());
        System.out.println(60.0 / bpm.getRaw());
        if(!pulseColor.equals(ColorTag.NULL_TAG)) {
            return HardwareLightVisualState.createBlinking(pulseColor.asColor(), Color.mix(pulseColor.asColor(), Color.blackColor(), 0.7), 60.0 / bpm.getRaw(), 60.0 / bpm.getRaw());
        } else {
            if(!blinkColor.equals(ColorTag.NULL_TAG)) {
                return HardwareLightVisualState.createBlinking(blinkColor.asColor(), solidColor.asColor(), 30.0 / bpm.getRaw(), 30.0 / bpm.getRaw());
            } else {
                return HardwareLightVisualState.createForColor(solidColor.asColor());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        LaunchpadXHardwareLight light = (LaunchpadXHardwareLight)obj;
        if(light == null) return false;
        if(light.solidColor != solidColor) {
            return false;
        } else {
            if(light.blinkColor != blinkColor) {
                return false;
            } else {
                return light.pulseColor == pulseColor;
            }
        }
    }


    @Override
    public String toString() {
        return "Solid " + solidColor + " Blink " + blinkColor + " Pulse " + pulseColor;
    }

    public ColorTag pulseColor() { return pulseColor; }
    public ColorTag blinkColor() { return blinkColor; }
    public ColorTag baseColor() { return solidColor; }
}
