package io.github.jengamon.novation;

import com.bitwig.extension.api.Color;

public class Utils {
    public static String printColor(Color c) {
        return "R" + c.getRed255() + "G" + c.getGreen255() + "B" + c.getBlue255() + "A" + c.getAlpha255();
    }

    public static ColorTag toTag(Color c) {
        return new ColorTag(c.getRed255(), c.getGreen255(), c.getBlue255());
    }

    public static String toHexString(byte... data) {
        StringBuilder builder = new StringBuilder();
        for(byte dp : data) {
            builder.append(Character.forDigit((dp >> 4) & 0xF, 16));
            builder.append(Character.forDigit(dp & 0xF, 16));
        }
        return builder.toString();
    }

    public static byte[] parseSysex(String sysex) {
        String message = sysex.substring(12, sysex.length() - 2);
//        System.out.println(sysex);
        byte[] bytes = new byte[message.length() / 2];
        for(int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)(Integer.parseInt(message.substring(i * 2, i * 2 + 2), 16) & 0xff);
        }
        return bytes;
    }
}
