package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.enums.RosterChangeTypeEnum;
import com.zhs.basket.enums.RosterLockStatusEnum;
import com.zhs.basket.enums.RosterSourceTypeEnum;
import com.zhs.basket.enums.SeasonRosterStatusEnum;
import com.zhs.basket.mapper.BasketTeamSeasonProfileMapper;
import com.zhs.basket.mapper.BasketTournamentRosterChangeRecordMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.basket.query.SeasonRosterAdminQuery;
import com.zhs.basket.service.IBasketSeasonRosterAdminService;
import com.zhs.basket.vo.SeasonRosterChangeRecordVo;
import com.zhs.basket.vo.SeasonRosterListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BasketSeasonRosterAdminServiceImpl implements IBasketSeasonRosterAdminService {

    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketTournamentRosterChangeRecordMapper changeRecordMapper;
    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;

    @Override
    public List<SeasonRosterListVo> selectList(SeasonRosterAdminQuery query) {
        List<SeasonRosterListVo> list = seasonRosterMapper.selectAdminList(query);
        Map<Long, BasketTeamSeasonProfile> eventTeamMap = loadEventTeamMap(list);
        list.forEach(vo -> enrichListVo(vo, eventTeamMap.get(vo.getEventTeamId())));
        return list;
    }

    @Override
    public List<SeasonRosterChangeRecordVo> selectChangeHistory(Long playerId, Long seasonId) {
        return changeRecordMapper.selectByPlayerAndSeason(seasonId, playerId).stream()
                .peek(vo -> vo.setChangeTypeName(RosterChangeTypeEnum.fromCode(vo.getChangeType()).getDescription()))
                .collect(Collectors.toList());
    }

    private Map<Long, BasketTeamSeasonProfile> loadEventTeamMap(List<SeasonRosterListVo> list) {
        Map<Long, BasketTeamSeasonProfile> map = new HashMap<>();
        for (SeasonRosterListVo vo : list) {
            if (vo.getEventTeamId() == null || map.containsKey(vo.getEventTeamId())) {
                continue;
            }
            BasketTeamSeasonProfile profile = teamSeasonProfileMapper.selectById(vo.getEventTeamId());
            if (profile != null) {
                map.put(vo.getEventTeamId(), profile);
            }
        }
        return map;
    }

    private void enrichListVo(SeasonRosterListVo vo, BasketTeamSeasonProfile eventTeam) {
        vo.setRosterStatusName(SeasonRosterStatusEnum.fromCode(vo.getRosterStatus()).getDescription());
        vo.setSourceTypeName(RosterSourceTypeEnum.fromCode(vo.getSourceType()).getDescription());
        vo.setLockStatusName(resolvePlayerLockStatusName(vo.getLockStatus(), eventTeam));
    }

    private String resolvePlayerLockStatusName(Integer playerLockStatus, BasketTeamSeasonProfile eventTeam) {
        if (Integer.valueOf(RosterLockStatusEnum.LOCKED.getCode()).equals(playerLockStatus)) {
            return RosterLockStatusEnum.LOCKED.getDescription();
        }
        if (eventTeam != null && eventTeam.getApprovedTime() != null) {
            if (Integer.valueOf(RosterLockStatusEnum.LOCKED.getCode()).equals(eventTeam.getRosterLockStatus())) {
                return RosterLockStatusEnum.LOCKED.getDescription();
            }
            return "锁定异常";
        }
        return RosterLockStatusEnum.UNLOCKED.getDescription();
    }
}
