package com.zhs.service;


import com.zhs.model.BasketRenameApply;
import com.zhs.dto.BasketRenameApplyDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketRenameApplyService extends IService<BasketRenameApply> {
    R addOrUpdate(BasketRenameApplyDto basketRenameApplyDto);
}
