package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.enums.ParticipationStatusEnum;
import com.zhs.basket.enums.RosterLockStatusEnum;
import com.zhs.basket.mapper.BasketTeamSeasonProfileMapper;
import com.zhs.basket.query.EventTeamAdminQuery;
import com.zhs.basket.req.EventTeamParticipationStatusReq;
import com.zhs.basket.service.IBasketEventTeamAdminService;
import com.zhs.basket.vo.EventTeamDetailVo;
import com.zhs.basket.vo.EventTeamListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketEventTeamAdminServiceImpl implements IBasketEventTeamAdminService {

    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;

    @Override
    public List<EventTeamListVo> selectList(EventTeamAdminQuery query) {
        return teamSeasonProfileMapper.selectAdminList(query).stream().peek(this::enrichListVo).collect(Collectors.toList());
    }

    @Override
    public EventTeamDetailVo selectDetail(Long id) {
        EventTeamDetailVo vo = teamSeasonProfileMapper.selectDetailVo(id);
        if (vo == null) throw new ServiceException("赛事球队不存在");
        enrichDetailVo(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockRoster(Long id) {
        requireProfile(id);
        int rows = teamSeasonProfileMapper.updateRosterLockStatus(id, RosterLockStatusEnum.LOCKED.getCode(), new Date());
        if (rows != 1) throw new ServiceException("锁定阵容失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockRoster(Long id) {
        requireProfile(id);
        int rows = teamSeasonProfileMapper.updateRosterLockStatus(id, RosterLockStatusEnum.UNLOCKED.getCode(), null);
        if (rows != 1) throw new ServiceException("解锁阵容失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateParticipationStatus(Long id, EventTeamParticipationStatusReq req) {
        requireProfile(id);
        if (req == null || req.getParticipationStatus() == null) {
            throw new ServiceException("参赛状态不能为空");
        }
        ParticipationStatusEnum.fromCode(req.getParticipationStatus());
        int rows = teamSeasonProfileMapper.updateParticipationStatus(id, req.getParticipationStatus());
        if (rows != 1) throw new ServiceException("更新参赛状态失败");
    }

    private BasketTeamSeasonProfile requireProfile(Long id) {
        BasketTeamSeasonProfile profile = teamSeasonProfileMapper.selectById(id);
        if (profile == null) throw new ServiceException("赛事球队不存在");
        return profile;
    }

    private void enrichListVo(EventTeamListVo vo) {
        vo.setParticipationStatusName(ParticipationStatusEnum.fromCode(vo.getParticipationStatus()).getDescription());
        vo.setRosterLockStatusName(RosterLockStatusEnum.fromCode(vo.getRosterLockStatus()).getDescription());
    }

    private void enrichDetailVo(EventTeamDetailVo vo) {
        vo.setParticipationStatusName(ParticipationStatusEnum.fromCode(vo.getParticipationStatus()).getDescription());
        vo.setRosterLockStatusName(RosterLockStatusEnum.fromCode(vo.getRosterLockStatus()).getDescription());
    }
}
