package com.zhs.service;


import com.zhs.model.BasketMemberAgreementRecord;
import com.zhs.dto.BasketMemberAgreementRecordDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-07-24
 */

public interface IBasketMemberAgreementRecordService extends IService<BasketMemberAgreementRecord> {
    R addOrUpdate(BasketMemberAgreementRecordDto basketMemberAgreementRecordDto);
}
