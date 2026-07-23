package com.zhs.service;


import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.dto.BasketTeamJoinApplicationDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketTeamJoinApplicationService extends IService<BasketTeamJoinApplication> {
    R addOrUpdate(BasketTeamJoinApplicationDto basketTeamJoinApplicationDto);
}
