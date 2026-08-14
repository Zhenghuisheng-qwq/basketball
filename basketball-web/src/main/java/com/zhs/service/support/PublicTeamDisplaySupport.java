package com.zhs.service.support;

import com.zhs.model.v4.BasketEventTeam;

/**
 * Public EventTeam display: temporary PERSONAL teams use snapshot, not long-term basket_team.
 */
public final class PublicTeamDisplaySupport {

    private PublicTeamDisplaySupport() {
    }

    public static boolean isTemporary(BasketEventTeam team) {
        return team != null && team.isPersonalFormationTeam();
    }

    public static String publicTeamName(BasketEventTeam team) {
        if (team == null) {
            return null;
        }
        return team.getTeamNameSnapshot();
    }
}
