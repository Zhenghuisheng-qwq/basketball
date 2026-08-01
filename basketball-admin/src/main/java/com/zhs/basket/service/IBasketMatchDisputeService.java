package com.zhs.basket.service;

import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.query.MatchDisputeQuery;
import com.zhs.basket.req.MatchDisputeCorrectionReq;
import com.zhs.basket.req.MatchDisputeCreateReq;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.MatchDisputeHandleReq;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.vo.MatchDisputeDetailVo;
import com.zhs.basket.vo.MatchDisputeListVo;

import java.util.List;

public interface IBasketMatchDisputeService {
    List<MatchDisputeListVo> list(MatchDisputeQuery query);

    MatchDisputeDetailVo getDetail(Long id);

    BasketMatchDataDispute getById(Long id);

    Long create(MatchDisputeCreateReq req);

    void reject(Long id, MatchDisputeHandleReq req);

    BatchAuditResultVo batchReject(BatchAuditOperationReq req);

    void uphold(Long id, MatchDisputeHandleReq req);

    void upholdAndCorrect(Long id, MatchDisputeCorrectionReq req);
}
