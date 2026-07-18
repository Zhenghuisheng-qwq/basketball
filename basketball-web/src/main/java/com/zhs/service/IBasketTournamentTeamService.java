package com.zhs.service;


import com.zhs.model.BasketTournamentTeam;
import com.zhs.dto.BasketTournamentTeamDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTournamentTeamService extends IService<BasketTournamentTeam> {
    R addOrUpdate(BasketTournamentTeamDto basketTournamentTeamDto);
}
