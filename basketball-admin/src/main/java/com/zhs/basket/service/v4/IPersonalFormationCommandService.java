package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.PersonalFormationMoveReq;
import com.zhs.basket.dto.v4.PersonalFormationRenameReq;
import com.zhs.basket.dto.v4.PersonalFormationSwapReq;
import com.zhs.basket.vo.v4.PersonalFormationWorkspaceVo;
import com.zhs.basket.vo.v4.TeamRosterLockResultVo;

public interface IPersonalFormationCommandService {
    PersonalFormationWorkspaceVo getWorkspace(Long divisionId);

    void generate(Long divisionId, Long operatorId);

    void renameTeam(Long divisionId, Long drawTeamId, PersonalFormationRenameReq req);

    void moveMember(Long divisionId, PersonalFormationMoveReq req);

    void swapMembers(Long divisionId, PersonalFormationSwapReq req);

    TeamRosterLockResultVo lock(Long divisionId, Long operatorId);
}
