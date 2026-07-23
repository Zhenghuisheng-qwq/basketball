package com.zhs.service;


import com.zhs.model.BasketTournamentRegistration;
import com.zhs.dto.BasketTournamentRegistrationDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentRegistrationService extends IService<BasketTournamentRegistration> {
    R addOrUpdate(BasketTournamentRegistrationDto basketTournamentRegistrationDto);
}
