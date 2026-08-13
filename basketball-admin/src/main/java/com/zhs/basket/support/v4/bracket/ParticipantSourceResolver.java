package com.zhs.basket.support.v4.bracket;

public interface ParticipantSourceResolver {

    boolean supports(String sourceType);

    Long resolve(BracketSlot slot, ParticipantResolveContext context);
}
