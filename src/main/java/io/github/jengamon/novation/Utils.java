package io.github.jengamon.novation;

import com.bitwig.extension.api.Color;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    private static final Color[] NOVATION_COLORS = new Color[]{
            // 00 - 07
            Color.fromRGBA255(0, 0, 0, 0),
            Color.fromRGB255(0x50, 0x50, 0x50),
            Color.fromRGB255(0xdd, 0xdd, 0xdd),
            Color.fromRGB255(0xff, 0xff, 0xff),
            Color.fromRGB255(0xf9, 0xb3, 0xb2),
            Color.fromRGB255(0xf5, 0x5f, 0x5e),
            Color.fromRGB255(0xd5, 0x60, 0x5f),
            Color.fromRGB255(0xad, 0x60, 0x60),
            // 08 - 0F
            Color.fromRGB255(0xfe, 0xf3, 0xd4),
            Color.fromRGB255(0xf9, 0xb2, 0x5a),
            Color.fromRGB255(0xd7, 0x8b, 0x5d),
            Color.fromRGB255(0xae, 0x76, 0x5f),
            Color.fromRGB255(0xfd, 0xee, 0x9d),
            Color.fromRGB255(0xfe, 0xff, 0x52),
            Color.fromRGB255(0xdc, 0xdd, 0x57),
            Color.fromRGB255(0xb2, 0xb3, 0x5c),
            // 10 - 17
            Color.fromRGB255(0xdf, 0xff, 0x9c),
            Color.fromRGB255(0xc7, 0xff, 0x54),
            Color.fromRGB255(0xa7, 0xdd, 0x59),
            Color.fromRGB255(0x86, 0xb3, 0x5d),
            Color.fromRGB255(0xc8, 0xff, 0xb0),
            Color.fromRGB255(0x79, 0xff, 0x56),
            Color.fromRGB255(0x72, 0xdd, 0x59),
            Color.fromRGB255(0x6b, 0xb3, 0x5d),
            // 18 - 1F
            Color.fromRGB255(0xc7, 0xfe, 0xbf),
            Color.fromRGB255(0x7a, 0xff, 0x87),
            Color.fromRGB255(0x73, 0xdd, 0x71),
            Color.fromRGB255(0x6b, 0xb3, 0x68),
            Color.fromRGB255(0xc8, 0xff, 0xca),
            Color.fromRGB255(0x7b, 0xff, 0xcb),
            Color.fromRGB255(0x72, 0xdb, 0x9e),
            Color.fromRGB255(0x6b, 0xb3, 0x7f),
            // 20 - 27
            Color.fromRGB255(0xc9, 0xff, 0xf3),
            Color.fromRGB255(0x7c, 0xff, 0xe9),
            Color.fromRGB255(0x74, 0xdd, 0xc2),
            Color.fromRGB255(0x6c, 0xb3, 0x95),
            Color.fromRGB255(0xc8, 0xf3, 0xff),
            Color.fromRGB255(0x79, 0xef, 0xff),
            Color.fromRGB255(0x71, 0xc7, 0xde),
            Color.fromRGB255(0x6a, 0xa1, 0xb4),
            // 28 - 2F
            Color.fromRGB255(0xc5, 0xdd, 0xff),
            Color.fromRGB255(0x72, 0xc8, 0xff),
            Color.fromRGB255(0x6b, 0xa2, 0xdf),
            Color.fromRGB255(0x66, 0x81, 0xb5),
            Color.fromRGB255(0xa1, 0x8d, 0xff),
            Color.fromRGB255(0x65, 0x63, 0xff),
            Color.fromRGB255(0x64, 0x62, 0xe0),
            Color.fromRGB255(0x63, 0x62, 0xb5),
            // 30 - 37
            Color.fromRGB255(0xcb, 0xb3, 0xff),
            Color.fromRGB255(0x9f, 0x62, 0xff),
            Color.fromRGB255(0x80, 0x62, 0xe0),
            Color.fromRGB255(0x75, 0x62, 0xb5),
            Color.fromRGB255(0xfa, 0xb3, 0xff),
            Color.fromRGB255(0xf7, 0x61, 0xff),
            Color.fromRGB255(0xd6, 0x61, 0xe0),
            Color.fromRGB255(0xae, 0x61, 0xb5),
            // 38 - 3F
            Color.fromRGB255(0xf9, 0xb3, 0xd6),
            Color.fromRGB255(0xf6, 0x60, 0xc3),
            Color.fromRGB255(0xd5, 0x60, 0xa2),
            Color.fromRGB255(0xae, 0x61, 0x8d),
            Color.fromRGB255(0xf6, 0x75, 0x5d),
            Color.fromRGB255(0xe4, 0xb2, 0x5a),
            Color.fromRGB255(0xda, 0xc2, 0x5a),
            Color.fromRGB255(0xa0, 0xa1, 0x5d),
            // 40 - 47
            Color.fromRGB255(0x6b, 0xb3, 0x5d),
            Color.fromRGB255(0x6c, 0xb3, 0x8b),
            Color.fromRGB255(0x68, 0x8d, 0xd7),
            Color.fromRGB255(0x65, 0x63, 0xff),
            Color.fromRGB255(0x6c, 0xb3, 0xb4),
            Color.fromRGB255(0x8b, 0x62, 0xf7),
            Color.fromRGB255(0xca, 0xb3, 0xc2),
            Color.fromRGB255(0x8a, 0x76, 0x81),
            // 48 - 4F
            Color.fromRGB255(0xf5, 0x5f, 0x5e),
            Color.fromRGB255(0xf3, 0xff, 0x9c),
            Color.fromRGB255(0xee, 0xfc, 0x53),
            Color.fromRGB255(0xd0, 0xff, 0x54),
            Color.fromRGB255(0x83, 0xdd, 0x59),
            Color.fromRGB255(0x7b, 0xff, 0xcb),
            Color.fromRGB255(0x78, 0xea, 0xff),
            Color.fromRGB255(0x6c, 0xa2, 0xff),
            // 50 - 57
            Color.fromRGB255(0x8b, 0x62, 0xff),
            Color.fromRGB255(0xc7, 0x62, 0xff),
            Color.fromRGB255(0xe8, 0x8c, 0xdf),
            Color.fromRGB255(0x9d, 0x76, 0x5f),
            Color.fromRGB255(0xf8, 0xa0, 0x5b),
            Color.fromRGB255(0xdf, 0xf9, 0x54),
            Color.fromRGB255(0xd8, 0xff, 0x85),
            Color.fromRGB255(0x79, 0xff, 0x56),
            // 58 - 5F
            Color.fromRGB255(0xbb, 0xff, 0x9d),
            Color.fromRGB255(0xd1, 0xfc, 0xd4),
            Color.fromRGB255(0xbc, 0xff, 0xf6),
            Color.fromRGB255(0xcf, 0xe4, 0xff),
            Color.fromRGB255(0xa5, 0xc2, 0xf8),
            Color.fromRGB255(0xd4, 0xc2, 0xfb),
            Color.fromRGB255(0xf2, 0x8c, 0xff),
            Color.fromRGB255(0xf6, 0x60, 0xce),
            // 60 - 67
            Color.fromRGB255(0xf9, 0xc1, 0x59),
            Color.fromRGB255(0xf1, 0xee, 0x55),
            Color.fromRGB255(0xe5, 0xff, 0x53),
            Color.fromRGB255(0xdb, 0xcc, 0x59),
            Color.fromRGB255(0xb1, 0xa1, 0x5d),
            Color.fromRGB255(0x6c, 0xba, 0x73),
            Color.fromRGB255(0x7f, 0xc2, 0x8a),
            Color.fromRGB255(0x81, 0x81, 0xa2),
            // 68 - 6F
            Color.fromRGB255(0x83, 0x8c, 0xce),
            Color.fromRGB255(0xc9, 0xaa, 0x7f),
            Color.fromRGB255(0xd5, 0x60, 0x5f),
            Color.fromRGB255(0xf3, 0xb3, 0x9f),
            Color.fromRGB255(0xf3, 0xb9, 0x71),
            Color.fromRGB255(0xfd, 0xf3, 0x85),
            Color.fromRGB255(0xea, 0xf9, 0x9c),
            Color.fromRGB255(0xd6, 0xee, 0x6e),
            // 70 - 77
            Color.fromRGB255(0x81, 0x81, 0xa2),
            Color.fromRGB255(0xf9, 0xf9, 0xd3),
            Color.fromRGB255(0xe0, 0xfc, 0xe3),
            Color.fromRGB255(0xe9, 0xe9, 0xff),
            Color.fromRGB255(0xe3, 0xd5, 0xff),
            Color.fromRGB255(0xb3, 0xb3, 0xb3),
            Color.fromRGB255(0xd5, 0xd5, 0xd5),
            Color.fromRGB255(0xfa, 0xff, 0xff),
            // 78 - 7F
            Color.fromRGB255(0xe0, 0x60, 0x5f),
            Color.fromRGB255(0xa5, 0x60, 0x60),
            Color.fromRGB255(0x8f, 0xf6, 0x56),
            Color.fromRGB255(0x6b, 0xb3, 0x5d),
            Color.fromRGB255(0xf1, 0xee, 0x55),
            Color.fromRGB255(0xb1, 0xa1, 0x5d),
            Color.fromRGB255(0xea, 0xc1, 0x59),
            Color.fromRGB255(0xbc, 0x75, 0x5f)
    };

    public static String printColor(Color c) {
        return "R" + c.getRed255() + "G" + c.getGreen255() + "B" + c.getBlue255() + "A" + c.getAlpha255();
    }

    public static Color fromNovation(byte i) {
        return NOVATION_COLORS[i];
    }

    // Approximates to the closest valid color
    private static byte toNovationApprox(Color c) {
        List<Color> colors = Arrays.asList(NOVATION_COLORS);
        List<Double> colorDistance = colors.stream()
                .map(color -> {
                    // redmean calc from https://www.compuphase.com/cmetric.htm
                    int rbar = (c.getRed255() + color.getRed255()) / 2;
                    return Math.sqrt(
                            (2 + (rbar / 256.0)) * Math.pow(c.getRed255() - color.getRed255(), 2) +
                                    4 * Math.pow(c.getGreen255() - color.getGreen255(), 2) +
                                    (2 + ((255 - rbar) / 256.0)) * Math.pow(c.getBlue255() - color.getBlue255(), 2)
                    );
                })
                .collect(Collectors.toList());
        return (byte) colorDistance.indexOf(Collections.min(colorDistance));
    }

    // Use nicer approximations of Bitwig fixed colors
    public static byte toNovation(Color c) {
        return toNovationApprox(c);
    }

    public static String toHexString(byte... data) {
        StringBuilder builder = new StringBuilder();
        for (byte dp : data) {
            builder.append(Character.forDigit((dp >> 4) & 0xF, 16));
            builder.append(Character.forDigit(dp & 0xF, 16));
        }
        return builder.toString();
    }

    public static byte[] parseSysex(String sysex) {
        String message = sysex.substring(12, sysex.length() - 2);
//        System.out.println(sysex);
        byte[] bytes = new byte[message.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (Integer.parseInt(message.substring(i * 2, i * 2 + 2), 16) & 0xff);
        }
        return bytes;
    }
}
