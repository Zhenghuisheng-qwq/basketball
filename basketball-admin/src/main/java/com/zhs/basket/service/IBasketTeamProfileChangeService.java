package com.zhs.basket.service;

import com.zhs.basket.query.TeamProfileChangeQuery;
import com.zhs.basket.req.TeamProfileChangeApproveReq;
import com.zhs.basket.req.TeamProfileChangeRejectReq;
import com.zhs.basket.vo.TeamProfileChangeDetailVo;
import com.zhs.basket.vo.TeamProfileChangeListVo;

import java.util.List;

public interface IBasketTeamProfileChangeService {

    List<TeamProfileChangeListVo> selectList(TeamProfileChangeQuery query);

    TeamProfileChangeDetailVo selectDetail(Long id);

    void approve(Long id, TeamProfileChangeApproveReq req);

    void reject(Long id, TeamProfileChangeRejectReq req);
}
