package com.zhs.service;


import com.zhs.model.BasketPlayerCertAudit;
import com.zhs.dto.BasketPlayerCertAuditDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketPlayerCertAuditService extends IService<BasketPlayerCertAudit> {
    R addOrUpdate(BasketPlayerCertAuditDto basketPlayerCertAuditDto);
}
