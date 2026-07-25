package com.zhs.basket.service;

public interface IBasketParticipationGenerationService {
    Long generateFromTeamRegistration(Long registrationId, Long auditorId, String remark);
}
