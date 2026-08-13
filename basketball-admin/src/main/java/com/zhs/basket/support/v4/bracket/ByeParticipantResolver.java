package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class ByeParticipantResolver implements ParticipantSourceResolver {

    @Override
    public boolean supports(String sourceType) {
        return ParticipantSourceTypeEnum.BYE.getCode().equals(sourceType);
    }

    @Override
    public Long resolve(BracketSlot slot, ParticipantResolveContext context) {
        return null;
    }
}
