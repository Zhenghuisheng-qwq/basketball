package com.zhs.basket.support;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.support.restriction.EffectiveDisciplineRestriction;
import com.zhs.basket.support.restriction.EffectiveDisciplineRestrictionResolver;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BasketDisciplineRestrictionSupport {

    @Autowired private EffectiveDisciplineRestrictionResolver restrictionResolver;
    @Autowired private BasketPlayerProfileMapper playerProfileMapper;

    public void assertAllowRegistration(String subjectType, Long subjectId, Long seasonId) {
        assertAllowed(subjectType, subjectId, seasonId, "allowRegistration", "当前处于报名限制期，无法报名");
    }

    public void assertAllowTransfer(String subjectType, Long subjectId, Long seasonId) {
        assertAllowed(subjectType, subjectId, seasonId, "allowTransfer", "当前处于转会限制期，无法转会");
    }

    public void assertAllowCaptain(String subjectType, Long subjectId, Long seasonId) {
        assertAllowed(subjectType, subjectId, seasonId, "allowCaptain", "当前处于队长限制期，无法担任队长");
    }

    public void assertAllowLineup(Long memberId, Long seasonId) {
        String reason = checkLineup(memberId, seasonId);
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    public String checkRegistration(String subjectType, Long subjectId, Long seasonId) {
        return checkAllowed(subjectType, subjectId, seasonId, "allowRegistration", "当前处于报名限制期，无法报名");
    }

    public String checkTransfer(String subjectType, Long subjectId, Long seasonId) {
        return checkAllowed(subjectType, subjectId, seasonId, "allowTransfer", "当前处于转会限制期，无法转会");
    }

    public String checkCaptain(String subjectType, Long subjectId, Long seasonId) {
        return checkAllowed(subjectType, subjectId, seasonId, "allowCaptain", "当前处于队长限制期，无法担任队长");
    }

    public String checkLineup(Long memberId, Long seasonId) {
        if (memberId == null) {
            return null;
        }
        BasketPlayerProfile profile = playerProfileMapper.selectByMemberId(memberId);
        if (profile != null) {
            String playerReason = checkAllowed("PLAYER", profile.getId(), seasonId, "allowLineup", "当前处于比赛名单限制期，无法进入名单");
            if (playerReason != null) {
                return playerReason;
            }
            if (hasRemainingSuspension("PLAYER", profile.getId(), seasonId)) {
                return "当前处于禁赛期，剩余禁赛场次未消耗完毕";
            }
        }
        return checkAllowed("ACCOUNT", memberId, seasonId, "allowLineup", "当前处于比赛名单限制期，无法进入名单");
    }

    private String checkAllowed(String subjectType, Long subjectId, Long seasonId, String flag, String message) {
        if (subjectId == null) {
            return null;
        }
        EffectiveDisciplineRestriction restriction = restrictionResolver.resolve(subjectType, subjectId, seasonId);
        if (restriction == null || restriction.isUnrestricted()) {
            return null;
        }
        if (!readFlag(restriction, flag)) {
            return message;
        }
        return null;
    }

    private void assertAllowed(String subjectType, Long subjectId, Long seasonId, String flag, String message) {
        String reason = checkAllowed(subjectType, subjectId, seasonId, flag, message);
        if (reason != null) {
            throw new ServiceException(reason);
        }
    }

    private boolean hasRemainingSuspension(String subjectType, Long subjectId, Long seasonId) {
        EffectiveDisciplineRestriction restriction = restrictionResolver.resolve(subjectType, subjectId, seasonId);
        return restriction != null && restriction.getRemainingMatchSuspensions() > 0;
    }

    private boolean readFlag(EffectiveDisciplineRestriction restriction, String flag) {
        switch (flag) {
            case "allowRegistration":
                return restriction.isAllowRegistration();
            case "allowTransfer":
                return restriction.isAllowTransfer();
            case "allowCaptain":
                return restriction.isAllowCaptain();
            case "allowLineup":
                return restriction.isAllowLineup();
            case "allowParticipation":
                return restriction.isAllowParticipation();
            default:
                return true;
        }
    }
}
