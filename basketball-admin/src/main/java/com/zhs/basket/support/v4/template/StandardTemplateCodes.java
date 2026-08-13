package com.zhs.basket.support.v4.template;

import java.util.List;
import java.util.Set;

public final class StandardTemplateCodes {
    public static final String LEAGUE_STANDARD_V1 = "LEAGUE_STANDARD_V1";
    public static final String ARENA_STANDARD_V1 = "ARENA_STANDARD_V1";
    public static final String CUP_STANDARD_V1 = "CUP_STANDARD_V1";
    public static final String OPEN_STANDARD_V1 = "OPEN_STANDARD_V1";

    public static final String STAGE_REGULAR = "REGULAR";
    public static final String STAGE_PLAYOFF = "PLAYOFF";
    public static final String STAGE_GROUP = "GROUP";
    public static final String STAGE_KNOCKOUT = "KNOCKOUT";

    private StandardTemplateCodes() {
    }

    public static Set<String> all() {
        return Set.of(LEAGUE_STANDARD_V1, ARENA_STANDARD_V1, CUP_STANDARD_V1, OPEN_STANDARD_V1);
    }

    public static boolean isStandard(String templateCode) {
        return templateCode != null && all().contains(templateCode);
    }

    public static List<String> coreStageCodes(String templateCode) {
        if (LEAGUE_STANDARD_V1.equals(templateCode)) {
            return List.of(STAGE_REGULAR, STAGE_PLAYOFF);
        }
        if (ARENA_STANDARD_V1.equals(templateCode) || OPEN_STANDARD_V1.equals(templateCode)) {
            return List.of(STAGE_KNOCKOUT);
        }
        if (CUP_STANDARD_V1.equals(templateCode)) {
            return List.of(STAGE_GROUP, STAGE_KNOCKOUT);
        }
        return List.of();
    }
}
