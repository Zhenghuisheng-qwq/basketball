package com.zhs.service;


import com.zhs.model.BasketTournamentStanding;
import com.zhs.dto.BasketTournamentStandingDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTournamentStandingService extends IService<BasketTournamentStanding> {
    R addOrUpdate(BasketTournamentStandingDto basketTournamentStandingDto);
}
