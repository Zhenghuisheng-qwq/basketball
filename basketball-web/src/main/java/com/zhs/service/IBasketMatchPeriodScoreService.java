package com.zhs.service;


import com.zhs.model.BasketMatchPeriodScore;
import com.zhs.dto.BasketMatchPeriodScoreDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMatchPeriodScoreService extends IService<BasketMatchPeriodScore> {
    R addOrUpdate(BasketMatchPeriodScoreDto basketMatchPeriodScoreDto);
}
