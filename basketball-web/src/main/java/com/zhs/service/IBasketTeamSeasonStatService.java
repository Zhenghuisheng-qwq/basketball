package com.zhs.service;


import com.zhs.model.BasketTeamSeasonStat;
import com.zhs.dto.BasketTeamSeasonStatDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamSeasonStatService extends IService<BasketTeamSeasonStat> {
    R addOrUpdate(BasketTeamSeasonStatDto basketTeamSeasonStatDto);
}
