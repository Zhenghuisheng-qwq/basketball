package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.TeamRosterAllocationReq;
import com.zhs.basket.dto.v4.TeamRosterCandidateOrderReq;

public interface ITeamRosterAllocationService {
    void assign(Long divisionId, TeamRosterAllocationReq req, Long operatorId);

    void autoAllocate(Long divisionId, Long operatorId);

    void moveCandidate(Long divisionId, TeamRosterCandidateOrderReq req, Long operatorId);
}
