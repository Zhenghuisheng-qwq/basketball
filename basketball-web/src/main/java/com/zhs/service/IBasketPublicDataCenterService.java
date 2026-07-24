package com.zhs.service;

import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;

import java.util.List;

public interface IBasketPublicDataCenterService {

    List<PublicSeasonItemVo> listSeasons();

    List<PublicTeamRankItemVo> listTeamRanking(Long seasonId, String rankType, Integer limit);

    List<PublicPlayerRankItemVo> listPlayerRanking(Long seasonId, String rankType, Integer limit);

    PublicSeasonSummaryVo getSeasonSummary(Long seasonId);
}
