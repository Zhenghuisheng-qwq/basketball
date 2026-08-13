package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.TeamRosterPromoteReq;
import com.zhs.basket.vo.v4.TeamRosterLockResultVo;

public interface ITeamRosterCandidatePromotionService {
    TeamRosterLockResultVo promote(Long divisionId, TeamRosterPromoteReq req, Long operatorId);
}
