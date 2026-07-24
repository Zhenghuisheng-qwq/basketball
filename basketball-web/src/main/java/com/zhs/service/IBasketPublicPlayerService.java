package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerDetailVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;

import java.util.List;

public interface IBasketPublicPlayerService {

    PublicPlayerDetailVo getPlayer(Long playerId);

    List<PublicPlayerSeasonStatVo> listSeasonStat(Long playerId, Long seasonId);

    IPage<PublicMatchItemVo> pageMatches(Long playerId, Integer currentPage, Integer pageSize, Long seasonId);

    List<PublicPlayerRankItemVo> listRanking(Long playerId, Long seasonId, String rankType);
}
