package com.zhs.basket.service;

import com.zhs.basket.req.AdminMatchDataSubmissionApproveReq;
import com.zhs.basket.req.AdminMatchDataSubmissionPageReq;
import com.zhs.basket.req.AdminMatchDataSubmissionRejectReq;
import com.zhs.basket.vo.MatchDataSubmissionDetailVo;
import com.zhs.basket.vo.MatchDataSubmissionListVo;
import com.zhs.basket.vo.MatchDataSubmissionVersionVo;

import java.util.List;

public interface IBasketMatchDataSubmissionAuditService {
    List<MatchDataSubmissionListVo> pageSubmissions(AdminMatchDataSubmissionPageReq query);

    MatchDataSubmissionDetailVo getDetail(Long submissionId);

    void approve(Long submissionId, AdminMatchDataSubmissionApproveReq req);

    void reject(Long submissionId, AdminMatchDataSubmissionRejectReq req);

    List<MatchDataSubmissionVersionVo> listVersions(Long matchId);
}
