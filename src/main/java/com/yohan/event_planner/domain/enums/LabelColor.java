package com.yohan.event_planner.domain.enums;

/**
 * Predefined color palette for labels.
 * Each color includes a base version (default display),
 * a pastel version (used for incomplete events),
 * and a metallic version (used for completed events).
 *
 * These values are used to maintain consistent visual styling
 * across event statuses and UI themes.
 */
public enum LabelColor {

    RED("#FF4D4F", "#FFD6D7", "#D72631"),
    ORANGE("#FA8C16", "#FFE0B2", "#D46B08"),
    YELLOW("#FADB14", "#FFF7AE", "#D4B106"),
    GREEN("#52C41A", "#C7EFCF", "#237804"),
    TEAL("#13C2C2", "#A6E6E6", "#08979C"),
    BLUE("#1890FF", "#A3D3FF", "#0050B3"),
    PURPLE("#722ED1", "#D3C6F1", "#391085"),
    PINK("#EB2F96", "#FDCFE8", "#C41D7F"),
    GRAY("#8C8C8C", "#D9D9D9", "#595959");

    private final String baseHex;
    private final String pastelHex;
    private final String metallicHex;

    LabelColor(String baseHex, String pastelHex, String metallicHex) {
        this.baseHex = baseHex;
        this.pastelHex = pastelHex;
        this.metallicHex = metallicHex;
    }

    /**
     * @return the base hex color (used by default)
     */
    public String getBaseHex() {
        return baseHex;
    }

    /**
     * @return the pastel hex variant (used for incomplete events)
     */
    public String getPastelHex() {
        return pastelHex;
    }

    /**
     * @return the metallic hex variant (used for completed events)
     */
    public String getMetallicHex() {
        return metallicHex;
    }
}