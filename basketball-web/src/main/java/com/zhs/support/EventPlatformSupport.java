package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketTournamentSeasonPlatformDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTournamentSeasonPlatform;
import com.zhs.response.team.SeasonPlatformItemResp;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 赛事赛季开放平台校验。
 */
@Component
public class EventPlatformSupport {

    private static final Integer NOT_DELETED = 0;
    private static final int PLATFORM_OPEN = 1;

    @Resource
    private BasketTournamentSeasonPlatformDao seasonPlatformDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;

    public String requireOpenPlatform(Long seasonId, String platformCode) {
        String normalized = platformTeamMembershipSupport.requirePlatformCode(platformCode);
        BasketTournamentSeasonPlatform openPlatform = seasonPlatformDao.selectOne(new LambdaQueryWrapper<BasketTournamentSeasonPlatform>()
                .eq(BasketTournamentSeasonPlatform::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonPlatform::getPlatformCode, normalized)
                .eq(BasketTournamentSeasonPlatform::getPlatformStatus, PLATFORM_OPEN)
                .eq(BasketTournamentSeasonPlatform::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (openPlatform == null) {
            throw new ServiceException("该赛事未开放 " + normalized + " 平台报名");
        }
        return normalized;
    }

    public boolean isPlatformOpen(Long seasonId, String platformCode) {
        if (seasonId == null || !StringUtils.hasText(platformCode)) {
            return false;
        }
        try {
            requireOpenPlatform(seasonId, platformCode);
            return true;
        } catch (ServiceException ex) {
            return false;
        }
    }

    public List<SeasonPlatformItemResp> listSeasonPlatforms(Long seasonId) {
        List<SeasonPlatformItemResp> items = new ArrayList<>();
        if (seasonId == null) {
            return items;
        }
        List<BasketTournamentSeasonPlatform> openPlatforms = seasonPlatformDao.selectList(
                new LambdaQueryWrapper<BasketTournamentSeasonPlatform>()
                        .eq(BasketTournamentSeasonPlatform::getSeasonId, seasonId)
                        .eq(BasketTournamentSeasonPlatform::getPlatformStatus, PLATFORM_OPEN)
                        .eq(BasketTournamentSeasonPlatform::getIsDelete, NOT_DELETED));
        for (String code : Arrays.asList("PC", "PS", "XBOX")) {
            SeasonPlatformItemResp item = new SeasonPlatformItemResp();
            item.setPlatformCode(code);
            item.setPlatformName(resolvePlatformName(code));
            boolean open = openPlatforms.stream()
                    .anyMatch(platform -> code.equalsIgnoreCase(platform.getPlatformCode()));
            item.setOpen(open);
            item.setDisabledReason(open ? null : "该赛事未开放 " + resolvePlatformName(code) + " 平台组");
            items.add(item);
        }
        return items;
    }

    public String resolvePlatformName(String platformCode) {
        if (!StringUtils.hasText(platformCode)) {
            return platformCode;
        }
        return switch (platformCode.trim().toUpperCase()) {
            case "PC" -> "PC";
            case "PS", "PS5", "PSN" -> "PlayStation";
            case "XBOX" -> "Xbox";
            default -> platformCode.trim().toUpperCase();
        };
    }
}
