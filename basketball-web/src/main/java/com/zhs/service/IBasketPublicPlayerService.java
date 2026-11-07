package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerDetailVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;

import java.util.List;

public interface IBasketPublicPlayerService {

    PublicPlayerDetailVo getPlayer(Long playerId);

    /** 登录账号查看自己的已发布表现，不走公开隐私拦截。 */
    PublicPlayerDetailVo getOwnPlayer(Long memberId);

    List<PublicPlayerSeasonStatVo> listSeasonStat(Long playerId, Long seasonId);

    List<PublicPlayerSeasonStatVo> listOwnSeasonStat(Long memberId, Long seasonId);

    IPage<PublicMatchItemVo> pageMatches(Long playerId, Integer currentPage, Integer pageSize, Long seasonId);

    List<PublicPlayerRankItemVo> listRanking(Long playerId, Long seasonId, String rankType);

    IPage<PublicHonorItemResp> pageHonors(Long playerId, Integer currentPage, Integer pageSize);

    IPage<PublicHonorItemResp> pageOwnHonors(Long memberId, Integer currentPage, Integer pageSize);
}
