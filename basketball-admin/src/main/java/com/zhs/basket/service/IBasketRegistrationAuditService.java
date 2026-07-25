package com.zhs.basket.service;

import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.vo.RegistrationAuditDetailVo;
import com.zhs.basket.vo.RegistrationAuditListVo;
import com.zhs.basket.vo.RegistrationAuditRecordVo;
import java.util.List;

public interface IBasketRegistrationAuditService {
    List<RegistrationAuditListVo> selectList(RegistrationAuditQuery query);
    RegistrationAuditDetailVo selectDetail(Long id);
    void approve(Long id, RegistrationAuditApproveReq req);
    void reject(Long id, RegistrationAuditRejectReq req);
    List<RegistrationAuditRecordVo> selectAuditRecords(Long id);
}
