package com.zhs.service;


import com.zhs.model.BasketTournamentSeason;
import com.zhs.dto.BasketTournamentSeasonDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentSeasonService extends IService<BasketTournamentSeason> {
    R addOrUpdate(BasketTournamentSeasonDto basketTournamentSeasonDto);
}
