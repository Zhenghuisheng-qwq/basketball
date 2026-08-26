package com.zhs.service;

import com.zhs.response.publicapi.PublicHomeDataFocusVo;
import com.zhs.response.publicapi.PublicHomeVo;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;

import java.util.List;

public interface IBasketPublicHomeService {

    PublicHomeVo getHome();

    List<PublicMatchItemVo> listRecentMatches(Integer limit);

    List<PublicTeamRankItemVo> listTeamRanking(Integer limit);

    List<PublicPlayerRankItemVo> listPlayerRanking(Integer limit);

    PublicSeasonSummaryVo getStatSummary();

    PublicHomeDataFocusVo getDataFocus(String seriesCode);
}
