package com.zhs.basket.service;

import com.zhs.basket.query.TeamMembershipChangeQuery;
import com.zhs.basket.req.TeamMembershipChangeRejectReq;
import com.zhs.basket.vo.TeamMembershipChangeDetailVo;
import com.zhs.basket.vo.TeamMembershipChangeListVo;

import java.util.List;

public interface IBasketTeamMembershipChangeService {
    List<TeamMembershipChangeListVo> selectList(TeamMembershipChangeQuery query);

    TeamMembershipChangeDetailVo selectDetail(Long id);

    void approve(Long id, String auditRemark);

    void reject(Long id, TeamMembershipChangeRejectReq req);
}
