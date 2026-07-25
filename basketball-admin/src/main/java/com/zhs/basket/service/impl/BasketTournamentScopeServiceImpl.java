package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentAdminScope;
import com.zhs.basket.mapper.BasketTournamentAdminScopeMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.req.TournamentScopeGrantReq;
import com.zhs.basket.service.IBasketTournamentScopeService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.vo.TournamentScopeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketTournamentScopeServiceImpl implements IBasketTournamentScopeService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentAdminScopeMapper scopeMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketBusinessAuditWriter auditWriter;

    @Override
    public List<TournamentScopeListVo> list(Long tournamentId, Long adminUserId) {
        if (!SecurityUtils.isAdmin()) {
            throw new ServiceException("仅系统管理员可查看赛事授权");
        }
        return scopeMapper.selectList(tournamentId, adminUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long grant(TournamentScopeGrantReq req) {
        if (!SecurityUtils.isAdmin()) {
            throw new ServiceException("仅系统管理员可授权赛事");
        }
        if (req == null || req.getAdminUserId() == null || req.getTournamentId() == null) {
            throw new ServiceException("管理员与赛事不能为空");
        }
        if (tournamentMapper.selectById(req.getTournamentId()) == null) {
            throw new ServiceException("赛事不存在");
        }
        String scopeCode = StringUtils.isNotEmpty(req.getScopeCode()) ? req.getScopeCode() : "MANAGE";
        if (scopeMapper.countActiveGrant(req.getAdminUserId(), req.getTournamentId(), scopeCode) > 0) {
            throw new ServiceException("该管理员已拥有相同赛事授权");
        }
        Date now = new Date();
        BasketTournamentAdminScope row = new BasketTournamentAdminScope();
        row.setAdminUserId(req.getAdminUserId());
        row.setTournamentId(req.getTournamentId());
        row.setScopeCode(scopeCode);
        row.setGrantedBy(SecurityUtils.getUserId());
        row.setGrantedTime(now);
        row.setExpireTime(req.getExpireTime());
        row.setActiveFlag(1);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        scopeMapper.insert(row);
        auditWriter.writeAudit("TOURNAMENT_SCOPE", "TOURNAMENT_ADMIN_SCOPE", row.getId(), "GRANT",
                "授予赛事管理权限", null, null);
        return row.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        if (!SecurityUtils.isAdmin()) {
            throw new ServiceException("仅系统管理员可撤销赛事授权");
        }
        if (scopeMapper.deactivateById(id) != 1) {
            throw new ServiceException("撤销失败，授权记录不存在或已失效");
        }
        auditWriter.writeAudit("TOURNAMENT_SCOPE", "TOURNAMENT_ADMIN_SCOPE", id, "REVOKE", "撤销赛事管理权限", null, null);
    }
}
