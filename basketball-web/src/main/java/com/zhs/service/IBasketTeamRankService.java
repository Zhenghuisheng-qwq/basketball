package com.zhs.service;


import com.zhs.model.BasketTeamRank;
import com.zhs.dto.BasketTeamRankDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamRankService extends IService<BasketTeamRank> {
    R addOrUpdate(BasketTeamRankDto basketTeamRankDto);
}
