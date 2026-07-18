package com.zhs.service;


import com.zhs.model.BasketMatchTeam;
import com.zhs.dto.BasketMatchTeamDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketMatchTeamService extends IService<BasketMatchTeam> {
    R addOrUpdate(BasketMatchTeamDto basketMatchTeamDto);
}
