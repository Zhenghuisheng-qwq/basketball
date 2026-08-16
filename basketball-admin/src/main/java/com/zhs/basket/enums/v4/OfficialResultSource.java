package com.zhs.basket.enums.v4;

/**
 * Official match result source. Stored in business audit field resultSource, not a new Match column.
 */
public enum OfficialResultSource {
    SUBMISSION("SUBMISSION", "captain submission"),
    ADMIN_DIRECT("ADMIN_DIRECT", "admin direct"),
    ADMIN_CORRECTION("ADMIN_CORRECTION", "admin correction"),
    FORFEIT("FORFEIT", "match forfeit"),
    DISQUALIFICATION("DISQUALIFICATION", "event disqualification override");

    private final String code;
    private final String description;

    OfficialResultSource(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
