package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRegistrationAudit;
import java.util.List;

public interface BasketTournamentRegistrationAuditMapper {
    int insert(BasketTournamentRegistrationAudit record);
    List<BasketTournamentRegistrationAudit> selectByRegistrationId(Long registrationId);
}
