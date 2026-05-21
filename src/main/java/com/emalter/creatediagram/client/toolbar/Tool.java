package com.emalter.creatediagram.client.toolbar;

import com.emalter.creatediagram.logic.EmiHelper;
import dev.emi.emi.api.stack.EmiStack;

public enum Tool {
    PEN("create:super_glue", "P"),
    LINE("minecraft:blaze_rod", "L"),
    ERASER("create:dough", "E");

    private final String toolTypeID;
    private final String shortLabel;

    Tool(String toolType, String shortLabel) {
        this.toolTypeID = toolType;
        this.shortLabel = shortLabel;
    }

    public String getToolTypeID() { return this.toolTypeID; }
    public String getShortLabel() { return this.shortLabel; }

    /*
    Returns the EmiStack associated with this tool, or null if it cannot be found.
     */
    public EmiStack getEmiStack() {
        try {
            return EmiHelper.getStack(toolTypeID);
        } catch (Exception e) {
            return null;
        }
    }

    /*
    Returns an array of all the tool IDs
     */
    public static String[] getAllToolsIDs() {
        Tool[] tools = Tool.values();
        String[] types = new String[tools.length];
        for (int i = 0; i < tools.length; i++) {
            types[i] = tools[i].getToolTypeID();
        }
        return types;
    }
}