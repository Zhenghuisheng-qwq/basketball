package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicTeamCompareVo;
import com.zhs.response.publicapi.PublicTeamDetailVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.response.publicapi.PublicTeamRosterItemVo;
import com.zhs.response.publicapi.PublicTeamSeasonHistoryVo;
import com.zhs.response.publicapi.PublicTeamSeasonStatVo;

import java.util.List;

public interface IBasketPublicTeamService {

    PublicTeamDetailVo getTeam(Long teamId);

    List<PublicTeamSeasonHistoryVo> listSeasonHistory(Long teamId);

    IPage<PublicMatchItemVo> pageMatches(Long teamId, Integer currentPage, Integer pageSize, Long seasonId);

    List<PublicTeamRosterItemVo> listRoster(Long teamId);

    List<PublicTeamSeasonStatVo> listStat(Long teamId, Long seasonId);

    List<PublicTeamRankItemVo> listRanking(Long teamId, Long seasonId, String rankType);

    PublicTeamCompareVo compare(Long leftTeamId, Long rightTeamId, Long seasonId);

    IPage<PublicHonorItemResp> pageHonors(Long teamId, Integer currentPage, Integer pageSize);
}
