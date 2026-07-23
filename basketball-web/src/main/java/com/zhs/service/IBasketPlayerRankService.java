package com.zhs.service;


import com.zhs.model.BasketPlayerRank;
import com.zhs.dto.BasketPlayerRankDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPlayerRankService extends IService<BasketPlayerRank> {
    R addOrUpdate(BasketPlayerRankDto basketPlayerRankDto);
}
