package com.zhs.basket.service;

import com.zhs.basket.query.OpenRegistrationAuditQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.OpenRegistrationAuditApproveReq;
import com.zhs.basket.req.OpenRegistrationAuditRejectReq;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.vo.OpenRegistrationAuditDetailVo;
import com.zhs.basket.vo.OpenRegistrationAuditListVo;
import com.zhs.basket.vo.OpenRegistrationAuditRecordVo;
import java.util.List;

public interface IBasketOpenRegistrationAuditService {
    List<OpenRegistrationAuditListVo> selectList(OpenRegistrationAuditQuery query);
    OpenRegistrationAuditDetailVo selectDetail(Long id);
    void startReview(Long id);
    void approve(Long id, OpenRegistrationAuditApproveReq req);
    void reject(Long id, OpenRegistrationAuditRejectReq req);
    BatchAuditResultVo batchApprove(BatchAuditOperationReq req);
    BatchAuditResultVo batchReject(BatchAuditOperationReq req);
    List<OpenRegistrationAuditRecordVo> selectAuditRecords(Long id);
}
