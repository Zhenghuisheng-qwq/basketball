package com.zhs.support.v4;

import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketEventTeam;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class EventTeamCaptainAuthorization {

    public boolean isEventTeamCaptain(BasketEventTeam eventTeam, Long playerId) {
        return eventTeam != null && playerId != null && playerId.equals(eventTeam.getCaptainPlayerId());
    }

    public boolean canSubmitAsCaptain(BasketEventTeam eventTeam, Long playerId, BasketCaptainGrant grant) {
        if (!isEventTeamCaptain(eventTeam, playerId)) {
            return false;
        }
        if (eventTeam.isPersonalFormationTeam()) {
            return true;
        }
        if (grant == null) {
            return false;
        }
        if (!CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            return false;
        }
        return grant.getTeamId() == null || Objects.equals(grant.getTeamId(), eventTeam.getSourceTeamId());
    }
}
