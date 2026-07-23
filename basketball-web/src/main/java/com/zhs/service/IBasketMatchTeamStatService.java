package com.zhs.service;


import com.zhs.model.BasketMatchTeamStat;
import com.zhs.dto.BasketMatchTeamStatDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMatchTeamStatService extends IService<BasketMatchTeamStat> {
    R addOrUpdate(BasketMatchTeamStatDto basketMatchTeamStatDto);
}
