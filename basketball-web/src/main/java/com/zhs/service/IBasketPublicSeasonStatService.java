package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamSeasonStatVo;

public interface IBasketPublicSeasonStatService {

    IPage<PublicTeamSeasonStatVo> pageTeamStat(Long seasonId, Integer currentPage, Integer pageSize,
                                               Long stageId, String sortField, String sortOrder);

    IPage<PublicPlayerSeasonStatVo> pagePlayerStat(Long seasonId, Integer currentPage, Integer pageSize,
                                                   Long stageId, Long teamId, String sortField, String sortOrder);

    PublicSeasonSummaryVo getSummary(Long seasonId);
}
