package com.zhs.service;


import com.zhs.model.BasketTeam;
import com.zhs.dto.BasketTeamDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamService extends IService<BasketTeam> {
    R addOrUpdate(BasketTeamDto basketTeamDto);
}
