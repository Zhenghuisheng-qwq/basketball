package com.zhs.basket.service;

import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.req.AdminMatchDataSubmissionApproveReq;
import com.zhs.basket.req.AdminMatchDataSubmissionPageReq;
import com.zhs.basket.req.AdminMatchDataSubmissionRejectReq;
import com.zhs.basket.vo.MatchDataAuditListVo;
import com.zhs.basket.vo.MatchDataSubmissionDetailVo;
import com.zhs.basket.vo.MatchDataSubmissionListVo;
import com.zhs.basket.vo.MatchDataSubmissionVersionVo;

import java.util.List;

public interface IBasketMatchDataAuditService {
    List<MatchDataAuditListVo> listPending(MatchDataAuditQuery query);

    List<MatchDataSubmissionListVo> pageSubmissions(AdminMatchDataSubmissionPageReq query);

    MatchDataSubmissionDetailVo getSubmissionDetail(Long submissionId);

    void approveSubmission(Long submissionId, AdminMatchDataSubmissionApproveReq req);

    void rejectSubmission(Long submissionId, AdminMatchDataSubmissionRejectReq req);

    List<MatchDataSubmissionVersionVo> listSubmissionVersions(Long matchId);
}
