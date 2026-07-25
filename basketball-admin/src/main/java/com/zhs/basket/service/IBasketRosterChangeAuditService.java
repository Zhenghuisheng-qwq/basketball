package com.zhs.basket.service;

import com.zhs.basket.query.RosterChangeAuditQuery;
import com.zhs.basket.req.RosterChangeAuditApproveReq;
import com.zhs.basket.req.RosterChangeAuditRejectReq;
import com.zhs.basket.vo.RosterChangeAuditDetailVo;
import com.zhs.basket.vo.RosterChangeAuditListVo;
import java.util.List;

public interface IBasketRosterChangeAuditService {
    List<RosterChangeAuditListVo> selectList(RosterChangeAuditQuery query);
    RosterChangeAuditDetailVo selectDetail(Long id);
    void approve(Long id, RosterChangeAuditApproveReq req);
    void reject(Long id, RosterChangeAuditRejectReq req);
}
