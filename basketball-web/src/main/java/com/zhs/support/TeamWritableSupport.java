package com.zhs.support;

import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketDivision;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 赛事球队写操作门禁：ACTIVE + 绑定 Division + Division 未结束。
 */
@Component
public class TeamWritableSupport {

    public static final String LEGACY_TEAM_READONLY_MESSAGE =
            "历史球队不能直接参与新赛事操作，请在当前赛事重新创建球队";
    public static final String TEAM_NOT_WRITABLE_MESSAGE = "当前球队不可操作";
    public static final String DIVISION_ENDED_MESSAGE = "当前赛事已结束，无法继续操作球队";

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketDivisionDao divisionDao;

    public void assertTeamWritable(BasketTeam team) {
        requireWritableTeam(team);
    }

    public BasketTeam requireWritableTeam(BasketTeam team) {
        if (team == null || !Objects.equals(NOT_DELETED, team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException(LEGACY_TEAM_READONLY_MESSAGE, 403);
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException(TEAM_NOT_WRITABLE_MESSAGE, 403);
        }
        assertDivisionOpenForTeamOps(team.getDivisionId());
        return team;
    }

    public BasketDivision requireDivisionForTeamCreate(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division == null || !Objects.equals(NOT_DELETED, division.getIsDelete())) {
            throw new ServiceException("Division 不存在");
        }
        assertDivisionOpenForTeamOps(division);
        return division;
    }

    public void assertDivisionOpenForTeamOps(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException(LEGACY_TEAM_READONLY_MESSAGE, 403);
        }
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division == null || !Objects.equals(NOT_DELETED, division.getIsDelete())) {
            throw new ServiceException("Division 不存在");
        }
        assertDivisionOpenForTeamOps(division);
    }

    public void assertDivisionOpenForTeamOps(BasketDivision division) {
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        String status = division.getDivisionStatus();
        if (DivisionStatusEnum.FINISHED.getCode().equals(status)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(status)) {
            throw new ServiceException(DIVISION_ENDED_MESSAGE, 403);
        }
    }

    public boolean isLegacyTeam(BasketTeam team) {
        return team == null || team.getDivisionId() == null;
    }
}
