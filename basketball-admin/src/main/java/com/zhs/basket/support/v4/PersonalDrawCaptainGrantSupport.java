package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketPersonalDrawTeam;
import com.zhs.basket.enums.v4.CaptainGrantSourceEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketPersonalDrawTeamMapper;
import com.zhs.basket.support.CaptainGrantSeasonSupport;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 个人抽签成队场景下的队长授权辅助。
 */
@Component
public class PersonalDrawCaptainGrantSupport {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private BasketPersonalDrawTeamMapper drawTeamMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private CaptainGrantRuntimeSupport grantRuntimeSupport;

    public void ensureGrantOnBindTeam(BasketPersonalDrawTeam drawTeam, BasketDivision division) {
        if (drawTeam == null || division == null || drawTeam.getCaptainPlayerId() == null) {
            return;
        }
        BasketPlayerProfile player = participationSupport.requireCertifiedPlayer(drawTeam.getCaptainPlayerId());

        List<BasketCaptainGrant> existing = captainGrantMapper.selectBySourceDrawTeamId(drawTeam.getId());
        for (BasketCaptainGrant grant : existing) {
            if (CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())
                    && participationSupport.isGrantUsable(grant)) {
                bindGrantToLongTermTeamIfNeeded(grant, drawTeam.getLongTermTeamId());
                return;
            }
        }

        BasketCaptainGrant active = captainGrantMapper.selectActiveByDivisionPlayer(
                division.getId(), drawTeam.getCaptainPlayerId());
        if (active != null && participationSupport.isGrantUsable(active)) {
            bindGrantToLongTermTeamIfNeeded(active, drawTeam.getLongTermTeamId());
            return;
        }

        Date now = new Date();
        Date validTo = grantRuntimeSupport.resolveValidTo(division.getId());
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setDivisionId(division.getId());
        grant.setSeasonId(division.getSeasonId());
        grant.setTeamId(drawTeam.getLongTermTeamId());
        grant.setPlayerId(drawTeam.getCaptainPlayerId());
        grant.setMemberId(player.getMemberId());
        grant.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        grant.setActiveUniqueKey(CaptainGrantActiveKeySupport.buildActiveUniqueKey(division.getId(), drawTeam.getCaptainPlayerId()));
        grant.setValidFrom(now);
        grant.setValidTo(validTo);
        grant.setGrantedBy(SecurityUtils.getUserId());
        grant.setRemark("个人抽签成队自动授权");
        grant.setGrantSource(CaptainGrantSourceEnum.PERSONAL_DRAW.getCode());
        grant.setSourceDrawTeamId(drawTeam.getId());
        grant.setCreateTime(now);
        grant.setUpdateTime(now);
        grant.setIsDelete(NOT_DELETED);
        if (captainGrantMapper.insert(grant) != 1) {
            throw new ServiceException("创建个人抽签队长授权失败");
        }
    }

    private void bindGrantToLongTermTeamIfNeeded(BasketCaptainGrant grant, Long longTermTeamId) {
        if (grant == null || longTermTeamId == null) {
            return;
        }
        grantRuntimeSupport.bindTeamIfAbsent(grant, longTermTeamId, "ADMIN");
    }

    public void revokeGrantsOnBatchCancel(Long drawBatchId) {
        if (drawBatchId == null) {
            return;
        }
        for (BasketPersonalDrawTeam team : drawTeamMapper.selectByBatchId(drawBatchId)) {
            revokeGrantsOnDrawTeamCancel(team.getId());
        }
    }

    public void revokeGrantsOnDrawTeamCancel(Long drawTeamId) {
        if (drawTeamId == null) {
            return;
        }
        captainGrantMapper.revokeBySourceDrawTeamId(drawTeamId);
    }
}
