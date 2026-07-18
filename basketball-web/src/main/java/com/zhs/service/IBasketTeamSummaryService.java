package com.zhs.service;


import com.zhs.model.BasketTeamSummary;
import com.zhs.dto.BasketTeamSummaryDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketTeamSummaryService extends IService<BasketTeamSummary> {
    R addOrUpdate(BasketTeamSummaryDto basketTeamSummaryDto);
}
