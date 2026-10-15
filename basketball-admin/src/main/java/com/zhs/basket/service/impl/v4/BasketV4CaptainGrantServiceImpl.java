package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.dto.v4.BasketCaptainGrantAdjustReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantSyncReq;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.v4.CaptainGrantSourceEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantValidToModeEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.service.v4.IBasketV4CaptainGrantService;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.basket.support.CaptainGrantSeasonSupport;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.support.v4.CaptainGrantActiveKeySupport;
import com.zhs.basket.support.v4.CaptainGrantEventSyncSupport;
import com.zhs.basket.support.v4.CaptainGrantRuntimeSupport;
import com.zhs.basket.vo.v4.BasketCaptainGrantListVo;
import com.zhs.basket.vo.v4.BasketCaptainGrantSyncPreviewVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4CaptainGrantServiceImpl implements IBasketV4CaptainGrantService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer LEGACY_ACTIVE = 1;

    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Autowired
    private CaptainGrantLogSupport grantLogSupport;
    @Autowired
    private CaptainGrantEventSyncSupport grantEventSyncSupport;

    @Override
    public List<BasketCaptainGrantListVo> selectList(BasketCaptainGrantQuery query) {
        return captainGrantMapper.selectVoList(query);
    }

    @Override
    public Date resolveValidTo(Long divisionId) {
        return grantRuntimeSupport.resolveValidTo(divisionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BasketCaptainGrantCreateReq req) {
        validateCreateReq(req);
        BasketDivision division = requireDivision(req.getDivisionId());
        participationSupport.rejectPersonalMode(division.getRegistrationMode());
        if (!RegistrationModeEnum.TEAM.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("当前仅支持 TEAM 报名授权");
        }
        CaptainGrantSeasonSupport.assertGrantableSeason(requireSeason(division.getSeasonId()));

        BasketPlayerProfile player = participationSupport.requireCertifiedPlayer(req.getPlayerId());
        participationSupport.requirePlatformIdentity(req.getPlayerId(), division.getPlatformCode());

        if (req.getMemberId() != null && !req.getMemberId().equals(player.getMemberId())) {
            throw new ServiceException("memberId 与球员档案不匹配");
        }

        BasketCaptainGrant existing = captainGrantMapper.selectActiveByDivisionPlayer(req.getDivisionId(), req.getPlayerId());
        if (existing != null && participationSupport.isGrantUsable(existing)) {
            throw new ServiceException("该 Division 下该球员已有有效队长授权");
        }

        Date now = new Date();
        Date validFrom = req.getValidFrom() != null ? req.getValidFrom() : now;
        if (validFrom.after(now)) {
            // 允许晚生效，但不能无意义地早于创建时刻太多——此处仅校验 from<=to
        }
        Date validTo = grantRuntimeSupport.resolveValidTo(division.getId());
        if (validFrom.after(validTo)) {
            throw new ServiceException("生效时间不能晚于赛事结束时间");
        }

        BasketCaptainGrant entity = new BasketCaptainGrant();
        entity.setDivisionId(division.getId());
        entity.setSeasonId(division.getSeasonId());
        entity.setTeamId(null);
        entity.setPlayerId(req.getPlayerId());
        entity.setMemberId(player.getMemberId());
        entity.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        entity.setActiveUniqueKey(CaptainGrantActiveKeySupport.buildActiveUniqueKey(division.getId(), req.getPlayerId()));
        entity.setValidFrom(validFrom);
        entity.setValidTo(validTo);
        entity.setValidToMode(CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
        entity.setGrantedBy(SecurityUtils.getUserId());
        entity.setRemark(req.getRemark());
        entity.setGrantSource(CaptainGrantSourceEnum.MANUAL.getCode());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setIsDelete(NOT_DELETED);
        captainGrantMapper.insert(entity);
        grantLogSupport.writeGrantLog(entity, CaptainGrantLogActionTypeEnum.GRANT,
                null, LEGACY_ACTIVE, null, req.getRemark(), "ADMIN");
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        BasketCaptainGrant grant = requireGrant(id);
        if (!CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            throw new ServiceException("仅 ACTIVE 授权可撤销");
        }
        if (captainGrantMapper.revokeById(id) != 1) {
            throw new ServiceException("撤销授权失败");
        }
        grantLogSupport.writeGrantLog(grant, CaptainGrantLogActionTypeEnum.REVOKE,
                LEGACY_ACTIVE, 2, null, grant.getRemark(), "ADMIN");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustValidTo(Long id, BasketCaptainGrantAdjustReq req) {
        if (req == null || req.getValidToMode() == null) {
            throw new ServiceException("请选择授权到期方式");
        }
        BasketCaptainGrant grant = requireGrant(id);
        grantEventSyncSupport.assertActiveGrant(grant);
        BasketDivision division = requireDivision(grant.getDivisionId());
        BasketSeason season = requireSeason(division.getSeasonId());
        grantEventSyncSupport.assertEventStillOpen(division, season);
        Date eventEnd = grantEventSyncSupport.requireEventEnd(division.getId());
        CaptainGrantValidToModeEnum mode = CaptainGrantValidToModeEnum.fromCode(req.getValidToMode());
        if (mode == null) {
            throw new ServiceException("授权到期方式不正确");
        }
        Date target;
        String reason;
        if (mode == CaptainGrantValidToModeEnum.AUTO_EVENT) {
            target = eventEnd;
            reason = "恢复跟随赛事结束时间";
        } else {
            if (req.getValidTo() == null) {
                throw new ServiceException("请填写授权截止时间");
            }
            target = req.getValidTo();
            if (grant.getValidFrom() != null && !target.after(grant.getValidFrom())) {
                throw new ServiceException("授权截止时间必须晚于生效时间");
            }
            if (target.after(eventEnd)) {
                throw new ServiceException("授权截止时间不能晚于赛事结束时间");
            }
            reason = "手动指定授权截止时间";
        }
        grantEventSyncSupport.applyWindow(grant, target, mode.getCode(), reason,
                CaptainGrantLogActionTypeEnum.ADJUST_VALID_TO);
    }

    @Override
    public BasketCaptainGrantSyncPreviewVo previewSync(BasketCaptainGrantSyncReq req) {
        BasketCaptainGrantSyncPreviewVo vo = new BasketCaptainGrantSyncPreviewVo();
        for (Long id : requireIds(req)) {
            vo.getItems().add(buildSyncItem(id));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketCaptainGrantSyncPreviewVo syncEventTime(BasketCaptainGrantSyncReq req) {
        BasketCaptainGrantSyncPreviewVo vo = new BasketCaptainGrantSyncPreviewVo();
        for (Long id : requireIds(req)) {
            BasketCaptainGrantSyncPreviewVo.Item item = buildSyncItem(id);
            if (item.isApplicable()) {
                BasketCaptainGrant grant = captainGrantMapper.selectById(id);
                try {
                    grantEventSyncSupport.applyWindow(grant, item.getTargetValidTo(),
                            CaptainGrantValidToModeEnum.AUTO_EVENT.getCode(),
                            "管理员同步赛事结束时间", CaptainGrantLogActionTypeEnum.SYNC_EVENT_TIME);
                    item.setMessage(item.getMessage() == null ? "已同步" : item.getMessage());
                    item.setValidToMode(CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
                } catch (ServiceException ex) {
                    item.setApplicable(false);
                    item.setMessage(ex.getMessage());
                }
            }
            vo.getItems().add(item);
        }
        return vo;
    }

    private void validateCreateReq(BasketCaptainGrantCreateReq req) {
        if (req == null) {
            throw new ServiceException("请求不能为空");
        }
        if (req.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        if (req.getPlayerId() == null) {
            throw new ServiceException("playerId 不能为空");
        }
        if (req.getValidFrom() != null && req.getValidTo() != null && req.getValidFrom().after(req.getValidTo())) {
            throw new ServiceException("生效时间不能晚于失效时间");
        }
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketSeason requireSeason(Long seasonId) {
        BasketSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private List<Long> requireIds(BasketCaptainGrantSyncReq req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new ServiceException("请选择要同步的授权");
        }
        return req.getIds();
    }

    private BasketCaptainGrantSyncPreviewVo.Item buildSyncItem(Long id) {
        BasketCaptainGrantSyncPreviewVo.Item item = new BasketCaptainGrantSyncPreviewVo.Item();
        item.setId(id);
        BasketCaptainGrant grant = id == null ? null : captainGrantMapper.selectById(id);
        if (grant == null) {
            item.setApplicable(false);
            item.setMessage("队长授权不存在");
            return item;
        }
        item.setDivisionId(grant.getDivisionId());
        item.setGrantStatus(grant.getGrantStatus());
        item.setValidToMode(grant.getValidToMode());
        item.setCurrentValidTo(grant.getValidTo());
        if (!CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            item.setApplicable(false);
            item.setMessage(CaptainGrantStatusEnum.REVOKED.getCode().equals(grant.getGrantStatus())
                    ? "已撤销授权不能同步"
                    : "已过期或非生效授权不能同步，请重新授权");
            return item;
        }
        BasketDivision division = divisionMapper.selectById(grant.getDivisionId());
        if (division == null) {
            item.setApplicable(false);
            item.setMessage("赛区不存在");
            return item;
        }
        item.setDivisionMatchEndTime(division.getMatchEndTime());
        BasketSeason season = seasonMapper.selectById(division.getSeasonId());
        if (season != null) {
            item.setSeasonEndTime(season.getSeasonEndTime());
        }
        try {
            grantEventSyncSupport.assertEventStillOpen(division, season);
            Date target = grantEventSyncSupport.requireEventEnd(division.getId());
            item.setTargetValidTo(target);
            item.setEventEndSource(grantEventSyncSupport.eventEndSource(division));
            item.setApplicable(true);
            if ("DIVISION".equals(item.getEventEndSource())
                    && season != null
                    && season.getSeasonEndTime() != null
                    && division.getMatchEndTime().before(season.getSeasonEndTime())) {
                item.setMessage("赛区比赛结束时间早于赛季结束时间。同步将使用赛区结束时间；如需延到赛季结束，请先修改赛区比赛结束时间。");
            }
        } catch (ServiceException ex) {
            item.setApplicable(false);
            item.setMessage(ex.getMessage());
        }
        return item;
    }

    private BasketCaptainGrant requireGrant(Long id) {
        if (id == null) {
            throw new ServiceException("授权 ID 不能为空");
        }
        BasketCaptainGrant grant = captainGrantMapper.selectById(id);
        if (grant == null) {
            throw new ServiceException("队长授权不存在");
        }
        return grant;
    }
}
