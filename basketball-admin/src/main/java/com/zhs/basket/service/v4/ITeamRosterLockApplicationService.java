package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.TeamRosterLockResultVo;
import com.zhs.basket.vo.v4.TeamRosterVersionDetailVo;
import com.zhs.basket.vo.v4.TeamRosterWorkspaceVo;

public interface ITeamRosterLockApplicationService {
    TeamRosterWorkspaceVo getWorkspace(Long divisionId);

    TeamRosterLockResultVo lock(Long divisionId, Long operatorId);

    TeamRosterVersionDetailVo getVersion(Long divisionId, Long rosterVersionId);

    java.util.List<com.zhs.basket.vo.v4.RosterVersionHistoryItemVo> listVersions(Long divisionId);
}
