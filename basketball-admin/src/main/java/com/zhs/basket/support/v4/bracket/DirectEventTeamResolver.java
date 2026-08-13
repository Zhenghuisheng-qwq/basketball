package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

@Component
public class DirectEventTeamResolver implements ParticipantSourceResolver {

    @Override
    public boolean supports(String sourceType) {
        return ParticipantSourceTypeEnum.DIRECT_EVENT_TEAM.getCode().equals(sourceType);
    }

    @Override
    public Long resolve(BracketSlot slot, ParticipantResolveContext context) {
        Long teamId = slot.getResolvedEventTeamId() != null ? slot.getResolvedEventTeamId() : slot.getEventTeamId();
        if (teamId == null) {
            throw new ServiceException("DIRECT_EVENT_TEAM 缺少 eventTeamId");
        }
        return teamId;
    }
}
