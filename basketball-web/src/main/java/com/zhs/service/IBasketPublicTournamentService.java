package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicSeasonItemVo;
import com.zhs.response.publicapi.PublicSeasonOverviewVo;
import com.zhs.response.publicapi.PublicSeasonPlacementResp;
import com.zhs.response.publicapi.PublicTournamentDetailVo;
import com.zhs.response.publicapi.PublicTournamentItemVo;

import java.util.List;

public interface IBasketPublicTournamentService {

    IPage<PublicTournamentItemVo> pageTournaments(Integer currentPage, Integer pageSize,
                                                  Integer tournamentStatus, String keyword);

    PublicTournamentDetailVo getTournament(Long tournamentId);

    List<PublicSeasonItemVo> listSeasons(Long tournamentId);

    PublicSeasonOverviewVo getSeasonOverview(Long seasonId);

    PublicSeasonPlacementResp getSeasonPlacement(Long seasonId);

    List<PublicHonorItemResp> listSeasonHonors(Long seasonId);
}
