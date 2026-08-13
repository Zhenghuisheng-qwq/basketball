package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketRegistration;

public interface IBasketV4EventSnapshotService {
    void generateFromApprovedRegistration(BasketRegistration registration);

    void generateFromFormalRegistration(BasketRegistration registration, Long rosterVersionId);

    com.zhs.basket.domain.v4.BasketEventTeam createTemporaryEventTeam(
            com.zhs.basket.domain.v4.BasketDivision division,
            com.zhs.basket.domain.v4.BasketDivisionRosterVersion rosterVersion,
            com.zhs.basket.domain.v4.BasketPersonalDrawTeam drawTeam,
            java.util.List<com.zhs.basket.domain.v4.BasketPersonalDrawAssignment> assignments,
            java.util.Map<Long, com.zhs.basket.domain.v4.BasketPersonalRegistration> registrationById,
            Long captainPlayerId,
            Long captainMemberId);
}
