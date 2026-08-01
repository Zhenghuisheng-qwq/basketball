package com.zhs.basket.service;

import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.vo.RegistrationAuditDetailVo;
import com.zhs.basket.vo.RegistrationAuditListVo;
import com.zhs.basket.vo.RegistrationAuditRecordVo;
import java.util.List;

public interface IBasketRegistrationAuditService {
    List<RegistrationAuditListVo> selectList(RegistrationAuditQuery query);
    RegistrationAuditDetailVo selectDetail(Long id);
    void approve(Long id, RegistrationAuditApproveReq req);
    void reject(Long id, RegistrationAuditRejectReq req);
    BatchAuditResultVo batchApprove(BatchAuditOperationReq req);
    BatchAuditResultVo batchReject(BatchAuditOperationReq req);
    List<RegistrationAuditRecordVo> selectAuditRecords(Long id);
}
