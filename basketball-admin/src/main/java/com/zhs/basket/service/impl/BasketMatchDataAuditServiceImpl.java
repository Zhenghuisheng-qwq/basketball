package com.zhs.basket.service.impl;

import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataAuditMapper;
import com.zhs.basket.query.MatchDataAuditQuery;
import com.zhs.basket.req.AdminMatchDataSubmissionApproveReq;
import com.zhs.basket.req.AdminMatchDataSubmissionPageReq;
import com.zhs.basket.req.AdminMatchDataSubmissionRejectReq;
import com.zhs.basket.service.IBasketMatchDataAuditService;
import com.zhs.basket.service.IBasketMatchDataSubmissionAuditService;
import com.zhs.basket.vo.MatchDataAuditListVo;
import com.zhs.basket.vo.MatchDataSubmissionDetailVo;
import com.zhs.basket.vo.MatchDataSubmissionListVo;
import com.zhs.basket.vo.MatchDataSubmissionVersionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketMatchDataAuditServiceImpl implements IBasketMatchDataAuditService {

    @Autowired
    private BasketMatchDataAuditMapper auditMapper;

    @Autowired
    private IBasketMatchDataSubmissionAuditService submissionAuditService;

    @Override
    public List<MatchDataAuditListVo> listPending(MatchDataAuditQuery query) {
        if (query == null) {
            query = new MatchDataAuditQuery();
        }
        if (query.getDataStatus() == null) {
            query.setDataStatus(MatchDataStatusEnum.SUBMITTED.getCode());
        }
        List<MatchDataAuditListVo> list = auditMapper.selectList(query);
        for (MatchDataAuditListVo vo : list) {
            vo.setDataStatusName(MatchDataStatusEnum.fromCode(vo.getDataStatus()).getDescription());
        }
        return list;
    }

    @Override
    public List<MatchDataSubmissionListVo> pageSubmissions(AdminMatchDataSubmissionPageReq query) {
        return submissionAuditService.pageSubmissions(query);
    }

    @Override
    public MatchDataSubmissionDetailVo getSubmissionDetail(Long submissionId) {
        return submissionAuditService.getDetail(submissionId);
    }

    @Override
    public void approveSubmission(Long submissionId, AdminMatchDataSubmissionApproveReq req) {
        submissionAuditService.approve(submissionId, req);
    }

    @Override
    public void rejectSubmission(Long submissionId, AdminMatchDataSubmissionRejectReq req) {
        submissionAuditService.reject(submissionId, req);
    }

    @Override
    public List<MatchDataSubmissionVersionVo> listSubmissionVersions(Long matchId) {
        return submissionAuditService.listVersions(matchId);
    }
}
