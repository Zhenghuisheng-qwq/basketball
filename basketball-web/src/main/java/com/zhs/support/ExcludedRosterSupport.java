package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketDivisionRosterEntryDao;
import com.zhs.dao.v4.BasketDivisionRosterVersionDao;
import com.zhs.model.v4.BasketDivisionRosterEntry;
import com.zhs.model.v4.BasketDivisionRosterVersion;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 最终名单已锁定，且这支球队被标成「本届不参赛」。
 * 锁定前的分配还可以改，不能据此放人。
 */
@Component
public class ExcludedRosterSupport {

    public static final String EXCLUDED = "EXCLUDED";
    private static final String LOCKED = "LOCKED";
    private static final Integer NOT_DELETED = 0;
    private static final int CURRENT = 1;

    @Resource
    private BasketDivisionRosterVersionDao rosterVersionDao;
    @Resource
    private BasketDivisionRosterEntryDao rosterEntryDao;

    public boolean isExcluded(Long teamId, Long divisionId) {
        if (teamId == null || divisionId == null) {
            return false;
        }
        BasketDivisionRosterVersion version = rosterVersionDao.selectOne(new LambdaQueryWrapper<BasketDivisionRosterVersion>()
                .eq(BasketDivisionRosterVersion::getDivisionId, divisionId)
                .eq(BasketDivisionRosterVersion::getIsCurrent, CURRENT)
                .eq(BasketDivisionRosterVersion::getStatus, LOCKED)
                .eq(BasketDivisionRosterVersion::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (version == null || version.getId() == null) {
            return false;
        }
        Long count = rosterEntryDao.selectCount(new LambdaQueryWrapper<BasketDivisionRosterEntry>()
                .eq(BasketDivisionRosterEntry::getRosterVersionId, version.getId())
                .eq(BasketDivisionRosterEntry::getTeamId, teamId)
                .eq(BasketDivisionRosterEntry::getParticipationRole, EXCLUDED)
                .eq(BasketDivisionRosterEntry::getIsDelete, NOT_DELETED));
        return count != null && count > 0;
    }
}
