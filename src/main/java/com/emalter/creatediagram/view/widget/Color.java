package com.emalter.creatediagram.view.widget;

/**
 * Predefined palette colors used by the UI. Each value maps to a dye item id and a display hex color.
 */
public enum Color {
    WHITE("minecraft:white_dye", 0xFFFFFFFF),
    YELLOW("minecraft:yellow_dye", 0xFFFFFF55),
    RED("minecraft:red_dye", 0xFFFF5555),
    BLUE("minecraft:blue_dye", 0xFF5555FF),
    GREEN("minecraft:green_dye", 0xFF55FF55),
    ORANGE("minecraft:orange_dye", 0xFFFFAA00);

    private final String dyeId;
    private final int hexValue;

    Color(String dyeId, int hexValue) {
        this.dyeId = dyeId;
        this.hexValue = hexValue;
    }

    /** Returns the associated dye item id for this color. */
    public String getDyeId() { return this.dyeId; }
    /** Returns the ARGB hex value used for rendering this color. */
    public int getHexValue() { return this.hexValue; }

    /** Returns an array with all dye item ids represented by the enum. */
    public static String[] getAllDyeIds() {
        Color[] colors = Color.values();
        String[] ids = new String[colors.length];
        for (int i = 0; i < colors.length; i++) {
            ids[i] = colors[i].getDyeId();
        }
        return ids;
    }

    /** Returns an array with all hex color values represented by the enum. */
    public static int[] getAllHexValues() {
        Color[] colors = Color.values();
        int[] values = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            values[i] = colors[i].getHexValue();
        }
        return values;
    }
}