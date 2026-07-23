package com.zhs.service;


import com.zhs.model.BasketTournamentGroupTeam;
import com.zhs.dto.BasketTournamentGroupTeamDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTournamentGroupTeamService extends IService<BasketTournamentGroupTeam> {
    R addOrUpdate(BasketTournamentGroupTeamDto basketTournamentGroupTeamDto);
}
