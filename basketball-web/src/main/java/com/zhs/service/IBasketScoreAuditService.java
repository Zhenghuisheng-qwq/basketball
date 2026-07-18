package com.zhs.service;


import com.zhs.model.BasketScoreAudit;
import com.zhs.dto.BasketScoreAuditDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-18
 */

public interface IBasketScoreAuditService extends IService<BasketScoreAudit> {
    R addOrUpdate(BasketScoreAuditDto basketScoreAuditDto);
}
