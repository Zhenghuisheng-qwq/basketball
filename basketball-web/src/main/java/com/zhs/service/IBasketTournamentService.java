package com.zhs.service;


import com.zhs.model.BasketTournament;
import com.zhs.dto.BasketTournamentDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTournamentService extends IService<BasketTournament> {
    R addOrUpdate(BasketTournamentDto basketTournamentDto);
}
