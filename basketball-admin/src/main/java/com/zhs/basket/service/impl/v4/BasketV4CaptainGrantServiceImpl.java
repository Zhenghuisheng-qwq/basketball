package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.service.v4.IBasketV4CaptainGrantService;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.vo.v4.BasketCaptainGrantListVo;
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

    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;

    @Override
    public List<BasketCaptainGrantListVo> selectList(BasketCaptainGrantQuery query) {
        return captainGrantMapper.selectVoList(query);
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

        participationSupport.requireValidTeam(req.getTeamId());
        BasketPlayerProfile player = participationSupport.requireCertifiedPlayer(req.getPlayerId());
        participationSupport.requireTeamMember(req.getTeamId(), req.getPlayerId());
        participationSupport.requirePlatformIdentity(req.getPlayerId(), division.getPlatformCode());

        if (req.getMemberId() != null && !req.getMemberId().equals(player.getMemberId())) {
            throw new ServiceException("memberId 与球员档案不匹配");
        }

        BasketCaptainGrant existing = captainGrantMapper.selectActiveByDivisionTeam(req.getDivisionId(), req.getTeamId());
        if (existing != null && participationSupport.isGrantUsable(existing)) {
            throw new ServiceException("该 Division 下该球队已有有效队长授权");
        }

        Date now = new Date();
        BasketCaptainGrant entity = new BasketCaptainGrant();
        entity.setDivisionId(division.getId());
        entity.setSeasonId(division.getSeasonId());
        entity.setTeamId(req.getTeamId());
        entity.setPlayerId(req.getPlayerId());
        entity.setMemberId(player.getMemberId());
        entity.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        entity.setActiveUniqueKey(participationSupport.buildActiveUniqueKey(division.getId(), req.getTeamId()));
        entity.setValidFrom(req.getValidFrom() != null ? req.getValidFrom() : now);
        entity.setValidTo(req.getValidTo());
        entity.setGrantedBy(SecurityUtils.getUserId());
        entity.setRemark(req.getRemark());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setIsDelete(NOT_DELETED);
        captainGrantMapper.insert(entity);
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
    }

    private void validateCreateReq(BasketCaptainGrantCreateReq req) {
        if (req == null) {
            throw new ServiceException("请求不能为空");
        }
        if (req.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        if (req.getTeamId() == null) {
            throw new ServiceException("teamId 不能为空");
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
