package com.zhs.service;


import com.zhs.model.BasketPlayerSummary;
import com.zhs.dto.BasketPlayerSummaryDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketPlayerSummaryService extends IService<BasketPlayerSummary> {
    R addOrUpdate(BasketPlayerSummaryDto basketPlayerSummaryDto);
}
