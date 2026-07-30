package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketMatchDao;
import com.zhs.enums.PublicPlayerRankTypeEnum;
import com.zhs.enums.PublicTeamRankTypeEnum;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.publicapi.PublicHomeVo;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.service.IBasketPublicAnnouncementService;
import com.zhs.service.IBasketPublicBannerService;
import com.zhs.service.IBasketPublicDataCenterService;
import com.zhs.service.IBasketPublicHomeService;
import com.zhs.service.IBasketPublicPartnerService;
import com.zhs.service.IBasketPublicSeasonStatService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicHomeServiceImpl implements IBasketPublicHomeService {

    /**
     * 热门球队无独立热度字段，取当前公开赛季球队榜 WIN_RATE 前 N，来源是赛季排名而非热度。
     */
    private static final String HOT_TEAM_SOURCE_NOTE =
            "热门球队来源：当前公开赛季球队排行榜 WIN_RATE 前 N（非热度字段）";

    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketPublicMatchServiceImpl matchService;
    @Resource
    private IBasketPublicDataCenterService dataCenterService;
    @Resource
    private IBasketPublicSeasonStatService seasonStatService;
    @Resource
    private IBasketPublicBannerService bannerService;
    @Resource
    private IBasketPublicAnnouncementService announcementService;
    @Resource
    private IBasketPublicPartnerService partnerService;

    @Override
    public PublicHomeVo getHome() {
        return publicSupport.cacheGetOrLoad("home:aggregate", () -> {
            PublicHomeVo vo = new PublicHomeVo();
            vo.setRecentMatches(safeList(() -> listRecentMatches(BasketPublicConstants.HOME_LIMIT)));
            vo.setTeamRankings(safeList(() -> listTeamRanking(BasketPublicConstants.HOME_LIMIT)));
            vo.setPlayerRankings(safeList(() -> listPlayerRanking(BasketPublicConstants.HOME_LIMIT)));
            try {
                vo.setSeasonSummary(getStatSummary());
            } catch (Exception ignored) {
                vo.setSeasonSummary(null);
            }
            vo.setHotTeamSourceNote(HOT_TEAM_SOURCE_NOTE);
            vo.setBanners(safeList(() -> bannerService.listBanners(null)));
            vo.setLatestAnnouncements(safeList(() ->
                    announcementService.listLatest(BasketPublicConstants.HOME_ANNOUNCEMENT_LIMIT)));
            vo.setPartners(safeList(partnerService::listPartners));
            return vo;
        });
    }

    private <T> List<T> safeList(java.util.function.Supplier<List<T>> supplier) {
        try {
            List<T> list = supplier.get();
            return list == null ? Collections.emptyList() : list;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<PublicMatchItemVo> listRecentMatches(Integer limit) {
        int size = normalizeLimit(limit);
        Date now = new Date();

        List<BasketMatch> upcoming = matchDao.selectList(new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .and(w -> w.eq(BasketMatch::getPublishStatus, BasketPublicConstants.PUBLISH_PUBLISHED)
                        .or(x -> x.eq(BasketMatch::getPublishStatus, 0).eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)))
                .in(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_SCHEDULED, 4)
                .ge(BasketMatch::getScheduledStartTime, now)
                .orderByAsc(BasketMatch::getScheduledStartTime)
                .orderByAsc(BasketMatch::getSortNo)
                .orderByAsc(BasketMatch::getId)
                .last("limit " + size));

        List<BasketMatch> finished = matchDao.selectList(new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .and(w -> w.eq(BasketMatch::getPublishStatus, BasketPublicConstants.PUBLISH_PUBLISHED)
                        .or(x -> x.eq(BasketMatch::getPublishStatus, 0).eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)))
                .eq(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_FINISHED)
                .orderByDesc(BasketMatch::getActualEndTime)
                .orderByDesc(BasketMatch::getScheduledStartTime)
                .orderByDesc(BasketMatch::getId)
                .last("limit " + size));

        List<BasketMatch> merged = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (BasketMatch match : upcoming) {
            if (seen.add(match.getId())) {
                merged.add(match);
            }
        }
        for (BasketMatch match : finished) {
            if (seen.add(match.getId()) && merged.size() < size) {
                merged.add(match);
            }
        }

        if (CollectionUtils.isEmpty(merged)) {
            return Collections.emptyList();
        }
        Set<Long> publicSeasonIds = publicSupport.loadSeasonMap(merged.stream()
                .map(BasketMatch::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet())).keySet();
        List<BasketMatch> filtered = merged.stream()
                .filter(m -> publicSeasonIds.contains(m.getSeasonId()))
                .limit(size)
                .collect(Collectors.toList());
        return matchService.enrichMatches(filtered);
    }

    @Override
    public List<PublicTeamRankItemVo> listTeamRanking(Integer limit) {
        // 无热度字段：使用当前公开赛季 WIN_RATE 排名前 N
        return dataCenterService.listTeamRanking(null, PublicTeamRankTypeEnum.WIN_RATE.getCode(), normalizeLimit(limit));
    }

    @Override
    public List<PublicPlayerRankItemVo> listPlayerRanking(Integer limit) {
        return dataCenterService.listPlayerRanking(null, PublicPlayerRankTypeEnum.AVG_POINTS.getCode(), normalizeLimit(limit));
    }

    @Override
    public PublicSeasonSummaryVo getStatSummary() {
        BasketTournamentSeason season = publicSupport.findCurrentPublicSeason();
        if (season == null) {
            return null;
        }
        return seasonStatService.getSummary(season.getId());
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return BasketPublicConstants.HOME_LIMIT;
        }
        return Math.min(limit, BasketPublicConstants.MAX_SIZE);
    }
}
