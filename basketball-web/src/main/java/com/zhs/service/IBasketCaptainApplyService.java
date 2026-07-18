package com.zhs.service;


import com.zhs.model.BasketCaptainApply;
import com.zhs.dto.BasketCaptainApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketCaptainApplyService extends IService<BasketCaptainApply> {
    R addOrUpdate(BasketCaptainApplyDto basketCaptainApplyDto);
}
