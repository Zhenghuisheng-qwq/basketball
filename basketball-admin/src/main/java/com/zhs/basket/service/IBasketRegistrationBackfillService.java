package com.zhs.basket.service;

import com.zhs.basket.vo.RegistrationBackfillResultVo;

public interface IBasketRegistrationBackfillService {
    RegistrationBackfillResultVo backfillApprovedRegistrations(Long operatorId);

    RegistrationBackfillResultVo backfillRegistration(Long registrationId, Long operatorId);
}
