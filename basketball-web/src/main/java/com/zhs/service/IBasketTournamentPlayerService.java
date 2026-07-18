package com.zhs.service;


import com.zhs.model.BasketTournamentPlayer;
import com.zhs.dto.BasketTournamentPlayerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTournamentPlayerService extends IService<BasketTournamentPlayer> {
    R addOrUpdate(BasketTournamentPlayerDto basketTournamentPlayerDto);
}
