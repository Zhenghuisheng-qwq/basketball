package com.zhs.basket.service.impl;

import com.zhs.basket.enums.RosterChangeTypeEnum;
import com.zhs.basket.enums.RosterSourceTypeEnum;
import com.zhs.basket.enums.SeasonRosterStatusEnum;
import com.zhs.basket.mapper.BasketTournamentRosterChangeRecordMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.basket.query.SeasonRosterAdminQuery;
import com.zhs.basket.service.IBasketSeasonRosterAdminService;
import com.zhs.basket.vo.SeasonRosterChangeRecordVo;
import com.zhs.basket.vo.SeasonRosterListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketSeasonRosterAdminServiceImpl implements IBasketSeasonRosterAdminService {

    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketTournamentRosterChangeRecordMapper changeRecordMapper;

    @Override
    public List<SeasonRosterListVo> selectList(SeasonRosterAdminQuery query) {
        return seasonRosterMapper.selectAdminList(query).stream().peek(this::enrichListVo).collect(Collectors.toList());
    }

    @Override
    public List<SeasonRosterChangeRecordVo> selectChangeHistory(Long playerId, Long seasonId) {
        return changeRecordMapper.selectByPlayerAndSeason(seasonId, playerId).stream()
                .peek(vo -> vo.setChangeTypeName(RosterChangeTypeEnum.fromCode(vo.getChangeType()).getDescription()))
                .collect(Collectors.toList());
    }

    private void enrichListVo(SeasonRosterListVo vo) {
        vo.setRosterStatusName(SeasonRosterStatusEnum.fromCode(vo.getRosterStatus()).getDescription());
        vo.setSourceTypeName(RosterSourceTypeEnum.fromCode(vo.getSourceType()).getDescription());
    }
}
