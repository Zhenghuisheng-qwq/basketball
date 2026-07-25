package com.zhs.basket.service.impl;

import com.zhs.basket.mapper.BasketBusinessAuditMapper;
import com.zhs.basket.query.BusinessAuditQuery;
import com.zhs.basket.service.IBasketBusinessAuditService;
import com.zhs.basket.vo.BusinessAuditDetailVo;
import com.zhs.basket.vo.BusinessAuditListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketBusinessAuditServiceImpl implements IBasketBusinessAuditService {

    @Autowired private BasketBusinessAuditMapper auditMapper;

    @Override
    public List<BusinessAuditListVo> list(BusinessAuditQuery query) {
        return auditMapper.selectList(query != null ? query : new BusinessAuditQuery());
    }

    @Override
    public BusinessAuditDetailVo detail(Long id) {
        BusinessAuditDetailVo detail = auditMapper.selectById(id);
        if (detail == null) {
            throw new ServiceException("审计记录不存在");
        }
        detail.setFields(auditMapper.selectFieldsByAuditId(id));
        return detail;
    }
}
