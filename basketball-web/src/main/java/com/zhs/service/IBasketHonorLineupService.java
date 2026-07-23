package com.zhs.service;


import com.zhs.model.BasketHonorLineup;
import com.zhs.dto.BasketHonorLineupDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketHonorLineupService extends IService<BasketHonorLineup> {
    R addOrUpdate(BasketHonorLineupDto basketHonorLineupDto);
}
