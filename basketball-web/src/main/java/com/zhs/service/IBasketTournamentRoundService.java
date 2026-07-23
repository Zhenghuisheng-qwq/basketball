package com.zhs.service;


import com.zhs.model.BasketTournamentRound;
import com.zhs.dto.BasketTournamentRoundDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentRoundService extends IService<BasketTournamentRound> {
    R addOrUpdate(BasketTournamentRoundDto basketTournamentRoundDto);
}
