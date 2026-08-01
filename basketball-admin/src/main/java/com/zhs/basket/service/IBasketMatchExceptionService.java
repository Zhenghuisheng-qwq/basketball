package com.zhs.basket.service;

import com.zhs.basket.domain.BasketMatchExceptionApplication;
import com.zhs.basket.query.MatchExceptionQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.MatchExceptionDecideReq;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.vo.MatchExceptionDetailVo;
import com.zhs.basket.vo.MatchExceptionListVo;

import java.util.List;

public interface IBasketMatchExceptionService {
    List<MatchExceptionListVo> list(MatchExceptionQuery query);

    MatchExceptionDetailVo getDetail(Long applicationId);

    void reject(Long applicationId, String rejectReason, String auditRemark);

    void decide(Long applicationId, MatchExceptionDecideReq req);

    BatchAuditResultVo batchReject(BatchAuditOperationReq req);

    BatchAuditResultVo batchDecide(BatchAuditOperationReq req);
}
