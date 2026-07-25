package com.zhs.basket.service;

import com.zhs.basket.query.BusinessAuditQuery;
import com.zhs.basket.vo.BusinessAuditDetailVo;
import com.zhs.basket.vo.BusinessAuditListVo;

import java.util.List;

public interface IBasketBusinessAuditService {
    List<BusinessAuditListVo> list(BusinessAuditQuery query);

    BusinessAuditDetailVo detail(Long id);
}
