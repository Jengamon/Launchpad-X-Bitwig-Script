package io.github.jengamon.novation;

import com.bitwig.extension.api.Color;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    private static final Color[] NOVATION_COLORS = new Color[] {
            // 00 - 07
            Color.fromRGBA255(0, 0, 0, 0),
            Color.fromRGB255(0xb3, 0xb3, 0xb3),
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

    private static final Map<Integer, Byte> INDEX_COLORS = new HashMap<>();

    static {
        INDEX_COLORS.put(0xff5706, (byte) 0x54);
        INDEX_COLORS.put(0xd99d10, (byte) 0x3D);
        INDEX_COLORS.put(0x545454, (byte) 0x75);
        INDEX_COLORS.put(0x7a7a7a, (byte) 0x76);
        INDEX_COLORS.put(0xc9c9c9, (byte) 0x77);
        INDEX_COLORS.put(0x8689ac, (byte) 0x74);
        INDEX_COLORS.put(0xa37943, (byte) 0x3D);
        INDEX_COLORS.put(0xc69f70, (byte) 0x7E);
        INDEX_COLORS.put(0x00a694, (byte) 0x41);
        INDEX_COLORS.put(0x5761c6, (byte) 0x2D);
        INDEX_COLORS.put(0x848ae0, (byte) 0x2C);
        INDEX_COLORS.put(0x9549cb, (byte) 0x36);
        INDEX_COLORS.put(0xbc76f0, (byte) 0x35);
        INDEX_COLORS.put(0x0099d9, (byte) 0x27);
        INDEX_COLORS.put(0x44c8ff, (byte) 0x25);
        INDEX_COLORS.put(0x43d2b9, (byte) 0x21);
        INDEX_COLORS.put(0x009d47, (byte) 0x1B);
        INDEX_COLORS.put(0x3ebb62, (byte) 0x19);
        INDEX_COLORS.put(0xd93871, (byte) 0x39);
        INDEX_COLORS.put(0xe16691, (byte) 0x38);
        INDEX_COLORS.put(0xd92e24, (byte) 0x6A);
        INDEX_COLORS.put(0xec6157, (byte) 0x6B);
        INDEX_COLORS.put(0xff833e, (byte) 0x6C);
        INDEX_COLORS.put(0xe4b74e, (byte) 0x3E);
        INDEX_COLORS.put(0x739814, (byte) 0x13);
        INDEX_COLORS.put(0xa0c04c, (byte) 0x11);
        INDEX_COLORS.put(0x808080, (byte) 0x1);
        INDEX_COLORS.put(0x7f7f7f, (byte) 0x1);
    }

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
                .map(color -> Math.sqrt(Math.pow(c.getRed() - color.getRed(), 2) + Math.pow(c.getGreen() - color.getGreen(), 2) + Math.pow(c.getBlue() - color.getBlue(), 2)))
                .collect(Collectors.toList());
        return (byte)colorDistance.indexOf(Collections.min(colorDistance));
    }

    // Use nicer approximations of Bitwig fixed colors
    public static byte toNovation(Color c) {
        int color_index = (c.getRed255() << 16) | (c.getGreen255() << 8) | (c.getBlue255());
        return INDEX_COLORS.computeIfAbsent(color_index, ci -> toNovationApprox(c));
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
