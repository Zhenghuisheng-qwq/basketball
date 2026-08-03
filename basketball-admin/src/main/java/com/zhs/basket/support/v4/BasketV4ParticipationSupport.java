package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.BasketPlayerPlatformAccountMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.support.GameTerminalSupport;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BasketV4ParticipationSupport {

    private static final int TEAM_STATUS_ACTIVE = 1;
    private static final int PLATFORM_ACCOUNT_ACTIVE = 1;

    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketTeamMemberMapper teamMemberMapper;
    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired
    private BasketPlayerPlatformAccountMapper platformAccountMapper;

    public BasketTeam requireValidTeam(Long teamId) {
        if (teamId == null) {
            throw new ServiceException("teamId 不能为空");
        }
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        if (!Integer.valueOf(TEAM_STATUS_ACTIVE).equals(team.getTeamStatus())) {
            throw new ServiceException("球队状态无效");
        }
        if (!TeamAuditStatusEnum.APPROVED.getCode().equals(team.getAuditStatus())) {
            throw new ServiceException("球队尚未审核通过");
        }
        return team;
    }

    public BasketPlayerProfile requireCertifiedPlayer(Long playerId) {
        if (playerId == null) {
            throw new ServiceException("playerId 不能为空");
        }
        BasketPlayerProfile profile = playerProfileMapper.selectById(playerId);
        if (profile == null) {
            throw new ServiceException("球员不存在");
        }
        if (!Integer.valueOf(PlayerCertStatusEnum.APPROVED.getCode()).equals(profile.getCertificationStatus())) {
            throw new ServiceException("球员尚未完成认证");
        }
        return profile;
    }

    public void requireTeamMember(Long teamId, Long playerId) {
        if (teamMemberMapper.countActiveMemberByTeamAndPlayer(teamId, playerId) <= 0) {
            throw new ServiceException("球员不是该球队当前有效成员");
        }
    }

    public BasketPlayerPlatformAccount requirePlatformIdentity(Long playerId, String platformCode) {
        String normalized = normalizePlatformCode(platformCode);
        List<BasketPlayerPlatformAccount> accounts = platformAccountMapper.selectByPlayerId(playerId);
        for (BasketPlayerPlatformAccount account : accounts) {
            if (Integer.valueOf(PLATFORM_ACCOUNT_ACTIVE).equals(account.getAccountStatus())
                    && normalized.equals(normalizePlatformCode(account.getPlatform()))) {
                return account;
            }
        }
        throw new ServiceException("球员缺少对应平台身份：" + normalized);
    }

    public String normalizePlatformCode(String platformCode) {
        return GameTerminalSupport.normalize(platformCode);
    }

    public boolean isGrantUsable(BasketCaptainGrant grant) {
        if (grant == null) {
            return false;
        }
        if (!CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            return false;
        }
        if (StringUtils.isEmpty(grant.getActiveUniqueKey())) {
            return false;
        }
        Date now = new Date();
        if (grant.getValidFrom() != null && grant.getValidFrom().after(now)) {
            return false;
        }
        if (grant.getValidTo() != null && grant.getValidTo().before(now)) {
            return false;
        }
        return true;
    }

    public void rejectPersonalMode(String registrationMode) {
        if (RegistrationModeEnum.PERSONAL.getCode().equals(registrationMode)) {
            throw new ServiceException("PERSONAL 个人报名暂未开放");
        }
    }

    public void validateLineupSize(BasketDivision division, int lineupSize) {
        int min = division.getLineupMinCount() != null ? division.getLineupMinCount() : 5;
        int max = division.getLineupMaxCount() != null ? division.getLineupMaxCount() : 8;
        if (lineupSize < min) {
            throw new ServiceException("阵容人数不能少于 " + min + " 人");
        }
        if (lineupSize > max) {
            throw new ServiceException("阵容人数不能多于 " + max + " 人");
        }
    }

    public List<BasketRegistrationPlayer> buildRegistrationPlayers(BasketDivision division,
                                                                   Long registrationId,
                                                                   Long teamId,
                                                                   Long captainPlayerId,
                                                                   List<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            throw new ServiceException("阵容不能为空");
        }
        Set<Long> uniqueIds = new HashSet<>(playerIds);
        if (uniqueIds.size() != playerIds.size()) {
            throw new ServiceException("阵容中存在重复球员");
        }
        if (!uniqueIds.contains(captainPlayerId)) {
            throw new ServiceException("队长必须在报名阵容中");
        }
        validateLineupSize(division, playerIds.size());

        String platformCode = normalizePlatformCode(division.getPlatformCode());
        List<BasketRegistrationPlayer> rows = new ArrayList<>();
        Date now = new Date();
        int sort = 0;
        for (Long playerId : playerIds) {
            requireTeamMember(teamId, playerId);
            BasketPlayerProfile profile = requireCertifiedPlayer(playerId);
            BasketPlayerPlatformAccount account = requirePlatformIdentity(playerId, platformCode);
            BasketRegistrationPlayer row = new BasketRegistrationPlayer();
            row.setRegistrationId(registrationId);
            row.setDivisionId(division.getId());
            row.setSeasonId(division.getSeasonId());
            row.setTeamId(teamId);
            row.setPlayerId(playerId);
            row.setMemberId(profile.getMemberId());
            row.setPlatformIdentityId(account.getId());
            row.setPlatformCode(platformCode);
            row.setGameIdSnapshot(account.getDisplayGameId());
            row.setPlayerNameSnapshot(profile.getGameNickname());
            row.setPosition(profile.getPositionCode());
            row.setJerseyNo(null);
            row.setIsCaptain(playerId.equals(captainPlayerId) ? 1 : 0);
            row.setIsStarter(1);
            row.setSortOrder(sort++);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(0);
            rows.add(row);
        }
        return rows;
    }

    public String buildActiveUniqueKey(Long divisionId, Long teamId) {
        return divisionId + "_" + teamId;
    }
}
