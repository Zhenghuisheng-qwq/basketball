package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentOpenRegistrationAudit;
import java.util.List;

public interface BasketTournamentOpenRegistrationAuditMapper {
    int insert(BasketTournamentOpenRegistrationAudit audit);
    List<BasketTournamentOpenRegistrationAudit> selectByRegistrationId(Long registrationId);
}
