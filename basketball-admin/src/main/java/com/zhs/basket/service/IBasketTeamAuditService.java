package com.zhs.basket.service;

import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.vo.TeamAuditDetailVo;
import com.zhs.basket.vo.TeamAuditListVo;
import com.zhs.basket.vo.TeamAuditRecordVo;
import java.util.List;

public interface IBasketTeamAuditService {
    List<TeamAuditListVo> selectList(TeamAuditQuery query);
    TeamAuditDetailVo selectDetail(Long teamId);
    List<TeamAuditRecordVo> selectAuditRecords(Long teamId);
    void startReview(Long teamId);
    void approve(Long teamId, TeamAuditApproveReq req);
    void reject(Long teamId, TeamAuditRejectReq req);
}
