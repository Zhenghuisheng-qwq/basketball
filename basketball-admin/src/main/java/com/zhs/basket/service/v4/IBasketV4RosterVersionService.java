package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;

/**
 * 名单版本骨架。R1 不接入审核/EventTeam 主链。
 */
public interface IBasketV4RosterVersionService {
    BasketDivisionRosterVersion createVersion(Long divisionId, Long operatorId);

    BasketDivisionRosterVersion lockVersion(Long rosterVersionId, Long operatorId);

    BasketDivisionRosterVersion getCurrentLockedVersion(Long divisionId);

    BasketDivisionRosterVersion getById(Long rosterVersionId);

    void invalidate(Long rosterVersionId);
}
