package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicHallOverviewResp;
import com.zhs.response.publicapi.PublicHonorDetailResp;
import com.zhs.response.publicapi.PublicHonorItemResp;

public interface IBasketPublicHallOfFameService {

    PublicHallOverviewResp getOverview();

    IPage<PublicHonorItemResp> pageTournamentHonors(Integer currentPage, Integer pageSize,
                                                    Long tournamentId, Long seasonId, String keyword,
                                                    String sortField, String sortOrder, String honorCodes);

    IPage<PublicHonorItemResp> pagePlayerHonors(Integer currentPage, Integer pageSize,
                                                Long tournamentId, Long seasonId, String keyword,
                                                String sortField, String sortOrder, String honorCodes);

    IPage<PublicHonorItemResp> pageTeamHonors(Integer currentPage, Integer pageSize,
                                              Long tournamentId, Long seasonId, String keyword,
                                              String sortField, String sortOrder, String honorCodes);

    PublicHonorDetailResp getHonorDetail(Long honorId);
}
