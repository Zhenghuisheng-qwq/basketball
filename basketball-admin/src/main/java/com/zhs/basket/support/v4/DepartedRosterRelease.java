package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 开赛前，把已经离开永久球队、但仍留在报名名单上的球员释放。
 * 不把新队员写入报名名单，也不改已冻结赛事。
 */
@Component
public class DepartedRosterRelease {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private RosterEligibilityQueries eligibilityQueries;

    @Transactional(rollbackFor = Exception.class)
    public int release(Long registrationId) {
        if (registrationId == null) {
            return 0;
        }
        BasketRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            return 0;
        }
        String status = registration.getRegistrationStatus();
        if (!"APPROVED".equals(status) && !"PENDING_REVIEW".equals(status)) {
            return 0;
        }
        if (registration.getDivisionId() != null && eligibilityQueries.frozen(registration.getDivisionId())) {
            return 0;
        }
        if (registration.getDivisionId() != null) {
            divisionMapper.selectByIdForUpdate(registration.getDivisionId());
        }
        List<BasketRegistrationPlayer> players = registrationPlayerMapper.selectByRegistrationId(registrationId);
        int released = 0;
        for (BasketRegistrationPlayer player : players) {
            if (player.getPlayerId() == null) {
                continue;
            }
            if (registrationPlayerMapper.countActiveMember(registration.getTeamId(), player.getPlayerId()) > 0) {
                continue;
            }
            released += registrationPlayerMapper.softDelete(player.getId());
        }
        if (released <= 0) {
            return 0;
        }
        int remain = registrationPlayerMapper.countRosterPlayers(registrationId);
        int version = registration.getLineupVersion() == null ? 0 : registration.getLineupVersion();
        registration.setLineupCount(remain);
        registration.setLineupVersion(version + 1);
        registration.setUpdateTime(new Date());
        registrationMapper.updateById(registration);
        writeAudit(registration, released, remain);
        return released;
    }

    private void writeAudit(BasketRegistration registration, int released, int remain) {
        Date now = new Date();
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction("MEMBER_LEFT");
        audit.setBeforeStatus(registration.getRegistrationStatus());
        audit.setAfterStatus(registration.getRegistrationStatus());
        audit.setAuditRemark("已离队球员 " + released + " 人从开赛前报名名单释放，当前有效报名 " + remain + " 人");
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        try {
            audit.setAuditUserId(SecurityUtils.getUserId());
            audit.setAuditUserName(SecurityUtils.getUsername());
        } catch (Exception ignored) {
            audit.setAuditUserName("system");
        }
        registrationAuditMapper.insert(audit);
    }
}
