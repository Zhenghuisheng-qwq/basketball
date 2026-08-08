package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class TeamRegistrationEligibilitySpecification {

    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;

    public void validateDivisionForTeam(BasketDivision division) {
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        participationSupport.rejectPersonalModeDivision(division.getRegistrationMode());
        if (!RegistrationModeEnum.TEAM.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("该 Division 不支持球队报名");
        }
    }

    public void validateRegistration(BasketDivision division,
                                     BasketRegistration registration,
                                     BasketCaptainGrant grant,
                                     List<BasketRegistrationPlayer> players) {
        validateDivisionForTeam(division);
        if (registration == null) {
            throw new ServiceException("报名不存在");
        }
        if (grant == null || !participationSupport.isGrantUsable(grant)) {
            throw new ServiceException("无有效队长授权");
        }
        if (!Objects.equals(registration.getCaptainPlayerId(), grant.getPlayerId())) {
            throw new ServiceException("报名队长与授权球员不一致");
        }
        grantRuntimeSupport.assertTeamBindingForRegistration(grant, registration.getTeamId());
        participationSupport.requireValidTeam(registration.getTeamId());

        BasketRegistration existingTeamReg = registrationMapper.selectByDivisionTeam(division.getId(), registration.getTeamId());
        if (existingTeamReg != null
                && !existingTeamReg.getId().equals(registration.getId())
                && isBlockingStatus(existingTeamReg.getRegistrationStatus())) {
            throw new ServiceException("该球队已在该 Division 提交报名");
        }

        if (players == null || players.isEmpty()) {
            throw new ServiceException("阵容不能为空");
        }
        participationSupport.validateLineupSize(division, players.size());

        Set<Long> playerIds = new HashSet<>();
        for (BasketRegistrationPlayer player : players) {
            if (!playerIds.add(player.getPlayerId())) {
                throw new ServiceException("阵容中存在重复球员");
            }
            if (registrationPlayerMapper.countActivePlayerInDivision(division.getId(), player.getPlayerId(), registration.getId()) > 0) {
                throw new ServiceException("球员已在该 Division 参与其他报名");
            }
            participationSupport.requireTeamMember(registration.getTeamId(), player.getPlayerId());
            participationSupport.requireCertifiedPlayer(player.getPlayerId());
            participationSupport.requirePlatformIdentity(player.getPlayerId(), division.getPlatformCode());
        }
        boolean captainInLineup = players.stream()
                .anyMatch(p -> Integer.valueOf(1).equals(p.getIsCaptain())
                        && registration.getCaptainPlayerId().equals(p.getPlayerId()));
        if (!captainInLineup) {
            throw new ServiceException("队长必须在报名阵容中");
        }
    }

    private boolean isBlockingStatus(String status) {
        return RegistrationStatusEnum.DRAFT.getCode().equals(status)
                || RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(status)
                || RegistrationStatusEnum.APPROVED.getCode().equals(status)
                || RegistrationStatusEnum.REJECTED.getCode().equals(status);
    }
}
