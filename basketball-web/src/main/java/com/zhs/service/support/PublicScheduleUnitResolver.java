package com.zhs.service.support;

public final class PublicScheduleUnitResolver {

    public static final String MATCH = "MATCH";
    public static final String MATCH_SERIES = "MATCH_SERIES";

    private PublicScheduleUnitResolver() {
    }

    public static String unitType(Integer bestOf) {
        return exposeAsSeries(bestOf) ? MATCH_SERIES : MATCH;
    }

    public static boolean exposeAsSeries(Integer bestOf) {
        return bestOf != null && bestOf > 1;
    }
}
