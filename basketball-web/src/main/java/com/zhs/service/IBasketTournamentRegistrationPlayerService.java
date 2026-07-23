package com.zhs.service;


import com.zhs.model.BasketTournamentRegistrationPlayer;
import com.zhs.dto.BasketTournamentRegistrationPlayerDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentRegistrationPlayerService extends IService<BasketTournamentRegistrationPlayer> {
    R addOrUpdate(BasketTournamentRegistrationPlayerDto basketTournamentRegistrationPlayerDto);
}
