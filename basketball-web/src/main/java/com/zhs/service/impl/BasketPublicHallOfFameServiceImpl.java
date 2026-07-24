package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.exception.ServiceException;
import com.zhs.response.publicapi.PublicHallOverviewResp;
import com.zhs.response.publicapi.PublicHonorDetailResp;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.service.IBasketPublicHallOfFameService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicHonorSupport;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class BasketPublicHallOfFameServiceImpl implements IBasketPublicHallOfFameService {

    private static final int OVERVIEW_LIMIT = 5;

    @Resource
    private BasketPublicHonorSupport honorSupport;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public PublicHallOverviewResp getOverview() {
        return publicSupport.cacheGetOrLoad("hall:overview", () -> {
            PublicHallOverviewResp vo = new PublicHallOverviewResp();
            vo.setTournamentHonorCount(honorSupport.countPublishedByScope(BasketPublicConstants.HONOR_SCOPE_TOURNAMENT));
            vo.setPlayerHonorCount(honorSupport.countPublishedByScope(BasketPublicConstants.HONOR_SCOPE_PLAYER));
            vo.setTeamHonorCount(honorSupport.countPublishedByScope(BasketPublicConstants.HONOR_SCOPE_TEAM));
            vo.setLatestTournamentHonors(honorSupport.toItemList(
                    honorSupport.listLatestByScope(BasketPublicConstants.HONOR_SCOPE_TOURNAMENT, OVERVIEW_LIMIT)));
            vo.setLatestPlayerHonors(honorSupport.toItemList(
                    honorSupport.listLatestByScope(BasketPublicConstants.HONOR_SCOPE_PLAYER, OVERVIEW_LIMIT)));
            vo.setLatestTeamHonors(honorSupport.toItemList(
                    honorSupport.listLatestByScope(BasketPublicConstants.HONOR_SCOPE_TEAM, OVERVIEW_LIMIT)));
            return vo;
        });
    }

    @Override
    public IPage<PublicHonorItemResp> pageTournamentHonors(Integer currentPage, Integer pageSize,
                                                           Long tournamentId, Long seasonId, String keyword,
                                                           String sortField, String sortOrder) {
        return honorSupport.pageByScope(BasketPublicConstants.HONOR_SCOPE_TOURNAMENT,
                currentPage, pageSize, tournamentId, seasonId, keyword, sortField, sortOrder);
    }

    @Override
    public IPage<PublicHonorItemResp> pagePlayerHonors(Integer currentPage, Integer pageSize,
                                                       Long tournamentId, Long seasonId, String keyword,
                                                       String sortField, String sortOrder) {
        return honorSupport.pageByScope(BasketPublicConstants.HONOR_SCOPE_PLAYER,
                currentPage, pageSize, tournamentId, seasonId, keyword, sortField, sortOrder);
    }

    @Override
    public IPage<PublicHonorItemResp> pageTeamHonors(Integer currentPage, Integer pageSize,
                                                     Long tournamentId, Long seasonId, String keyword,
                                                     String sortField, String sortOrder) {
        return honorSupport.pageByScope(BasketPublicConstants.HONOR_SCOPE_TEAM,
                currentPage, pageSize, tournamentId, seasonId, keyword, sortField, sortOrder);
    }

    @Override
    public PublicHonorDetailResp getHonorDetail(Long honorId) {
        PublicHonorDetailResp detail = honorSupport.getPublishedDetail(honorId);
        if (detail == null) {
            throw new ServiceException("荣誉不存在或未发布");
        }
        return detail;
    }
}
