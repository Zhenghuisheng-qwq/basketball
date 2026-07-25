package com.zhs.basket.service;

import com.zhs.basket.query.SeasonRosterAdminQuery;
import com.zhs.basket.vo.SeasonRosterChangeRecordVo;
import com.zhs.basket.vo.SeasonRosterListVo;
import java.util.List;

public interface IBasketSeasonRosterAdminService {
    List<SeasonRosterListVo> selectList(SeasonRosterAdminQuery query);
    List<SeasonRosterChangeRecordVo> selectChangeHistory(Long playerId, Long seasonId);
}
