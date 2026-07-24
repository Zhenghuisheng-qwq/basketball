package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketStageStandingDao;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.model.BasketHonorRecord;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketStageStanding;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicSeasonItemVo;
import com.zhs.response.publicapi.PublicSeasonOverviewVo;
import com.zhs.response.publicapi.PublicSeasonPlacementResp;
import com.zhs.response.publicapi.PublicTournamentDetailVo;
import com.zhs.response.publicapi.PublicTournamentItemVo;
import com.zhs.service.IBasketPublicTournamentService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicHonorSupport;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BasketPublicTournamentServiceImpl implements IBasketPublicTournamentService {

    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketStageStandingDao standingDao;
    @Resource
    private BasketTeamSeasonProfileDao teamSeasonProfileDao;
    @Resource
    private BasketPublicSupport publicSupport;
    @Resource
    private BasketPublicHonorSupport honorSupport;

    @Override
    public IPage<PublicTournamentItemVo> pageTournaments(Integer currentPage, Integer pageSize,
                                                         Integer tournamentStatus, String keyword) {
        Page<BasketTournament> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketTournament> wrapper = new LambdaQueryWrapper<BasketTournament>()
                .eq(BasketTournament::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketTournament::getPublicStatus, BasketPublicConstants.PUBLIC)
                .eq(tournamentStatus != null, BasketTournament::getTournamentStatus, tournamentStatus)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(BasketTournament::getTournamentName, keyword.trim())
                        .or()
                        .like(BasketTournament::getShortName, keyword.trim())
                        .or()
                        .like(BasketTournament::getTournamentNo, keyword.trim()))
                .orderByAsc(BasketTournament::getDisplayOrder)
                .orderByDesc(BasketTournament::getUpdateTime)
                .orderByDesc(BasketTournament::getId);
        IPage<BasketTournament> entityPage = tournamentDao.selectPage(page, wrapper);
        List<BasketTournament> records = entityPage.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Set<Long> fileIds = new HashSet<>();
        for (BasketTournament item : records) {
            if (item.getLogoFileId() != null) {
                fileIds.add(item.getLogoFileId());
            }
            if (item.getCoverFileId() != null) {
                fileIds.add(item.getCoverFileId());
            }
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(fileIds);
        Page<PublicTournamentItemVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        List<PublicTournamentItemVo> vos = new ArrayList<>(records.size());
        for (BasketTournament item : records) {
            PublicTournamentItemVo vo = new PublicTournamentItemVo();
            vo.setTournamentId(item.getId());
            vo.setTournamentNo(item.getTournamentNo());
            vo.setTournamentName(item.getTournamentName());
            vo.setShortName(item.getShortName());
            vo.setTournamentType(item.getTournamentType());
            vo.setGameTerminal(item.getGameTerminal());
            vo.setFormatCode(item.getFormatCode());
            vo.setOrganizerName(item.getOrganizerName());
            vo.setTournamentStatus(item.getTournamentStatus());
            vo.setLogoUrl(urlMap.get(item.getLogoFileId()));
            vo.setCoverUrl(urlMap.get(item.getCoverFileId()));
            vo.setDisplayOrder(item.getDisplayOrder());
            vo.setUpdateTime(item.getUpdateTime());
            vos.add(vo);
        }
        result.setRecords(vos);
        return result;
    }

    @Override
    public PublicTournamentDetailVo getTournament(Long tournamentId) {
        BasketTournament item = publicSupport.requirePublicTournament(tournamentId);
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(List.of(
                item.getLogoFileId(), item.getCoverFileId()));
        PublicTournamentDetailVo vo = new PublicTournamentDetailVo();
        vo.setTournamentId(item.getId());
        vo.setTournamentNo(item.getTournamentNo());
        vo.setTournamentName(item.getTournamentName());
        vo.setShortName(item.getShortName());
        vo.setEnglishName(item.getEnglishName());
        vo.setTournamentType(item.getTournamentType());
        vo.setGameTerminal(item.getGameTerminal());
        vo.setFormatCode(item.getFormatCode());
        vo.setOrganizerName(item.getOrganizerName());
        vo.setDescription(item.getDescription());
        vo.setTournamentStatus(item.getTournamentStatus());
        vo.setLogoUrl(urlMap.get(item.getLogoFileId()));
        vo.setCoverUrl(urlMap.get(item.getCoverFileId()));
        return vo;
    }

    @Override
    public List<PublicSeasonItemVo> listSeasons(Long tournamentId) {
        publicSupport.requirePublicTournament(tournamentId);
        List<BasketTournamentSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getTournamentId, tournamentId)
                .eq(BasketTournamentSeason::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketTournamentSeason::getPublicStatus, BasketPublicConstants.PUBLIC)
                .orderByDesc(BasketTournamentSeason::getSeasonStartTime)
                .orderByDesc(BasketTournamentSeason::getId));
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyList();
        }
        BasketTournament tournament = publicSupport.requirePublicTournament(tournamentId);
        List<PublicSeasonItemVo> result = new ArrayList<>(seasons.size());
        for (BasketTournamentSeason season : seasons) {
            result.add(toSeasonItem(season, tournament.getTournamentName()));
        }
        return result;
    }

    @Override
    public PublicSeasonOverviewVo getSeasonOverview(Long seasonId) {
        BasketTournamentSeason season = publicSupport.requirePublicSeason(seasonId);
        BasketTournament tournament = publicSupport.requirePublicTournament(season.getTournamentId());

        Long teamCount = teamSeasonProfileDao.selectCount(new LambdaQueryWrapper<BasketTeamSeasonProfile>()
                .eq(BasketTeamSeasonProfile::getSeasonId, seasonId)
                .eq(BasketTeamSeasonProfile::getIsDelete, BasketPublicConstants.NOT_DELETED));
        Long finishedMatchCount = matchDao.selectCount(new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getSeasonId, seasonId)
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC)
                .eq(BasketMatch::getMatchStatus, BasketPublicConstants.MATCH_FINISHED)
                .eq(BasketMatch::getDataStatus, BasketPublicConstants.DATA_CONFIRMED));
        Long publicMatchCount = matchDao.selectCount(new LambdaQueryWrapper<BasketMatch>()
                .eq(BasketMatch::getSeasonId, seasonId)
                .eq(BasketMatch::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketMatch::getPublicStatus, BasketPublicConstants.PUBLIC));
        Long standingCount = standingDao.selectCount(new LambdaQueryWrapper<BasketStageStanding>()
                .eq(BasketStageStanding::getSeasonId, seasonId)
                .eq(BasketStageStanding::getIsDelete, BasketPublicConstants.NOT_DELETED));

        PublicSeasonOverviewVo vo = new PublicSeasonOverviewVo();
        vo.setSeasonId(season.getId());
        vo.setSeasonName(season.getSeasonName());
        vo.setTournamentId(tournament.getId());
        vo.setTournamentName(tournament.getTournamentName());
        vo.setSeasonStatus(season.getSeasonStatus());
        vo.setTeamCount(teamCount == null ? 0 : teamCount.intValue());
        vo.setFinishedMatchCount(finishedMatchCount == null ? 0 : finishedMatchCount.intValue());
        vo.setPublicMatchCount(publicMatchCount == null ? 0 : publicMatchCount.intValue());
        vo.setStandingCount(standingCount == null ? 0 : standingCount.intValue());
        vo.setDescription(season.getDescription());
        return vo;
    }

    @Override
    public PublicSeasonPlacementResp getSeasonPlacement(Long seasonId) {
        BasketTournamentSeason season = publicSupport.requirePublicSeason(seasonId);
        PublicSeasonPlacementResp resp = new PublicSeasonPlacementResp();
        resp.setSeasonId(season.getId());
        resp.setSeasonName(season.getSeasonName());
        resp.setPublished(false);

        BasketHonorRecord championHonor = honorSupport.findPublishedChampionHonor(seasonId);
        boolean hasChampionTeam = season.getChampionTeamId() != null;
        if (!hasChampionTeam || championHonor == null) {
            return resp;
        }

        resp.setPublished(true);
        resp.setChampionHonorId(championHonor.getId());
        resp.setChampionTeamId(season.getChampionTeamId());
        resp.setRunnerUpTeamId(season.getRunnerUpTeamId());

        Set<Long> teamIds = new HashSet<>();
        teamIds.add(season.getChampionTeamId());
        if (season.getRunnerUpTeamId() != null) {
            teamIds.add(season.getRunnerUpTeamId());
        }
        Map<Long, BasketTeam> teamMap = publicSupport.loadPublicTeamMap(teamIds);
        Set<Long> fileIds = new HashSet<>();
        for (BasketTeam team : teamMap.values()) {
            if (team.getLogoFileId() != null) {
                fileIds.add(team.getLogoFileId());
            }
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(fileIds);
        BasketTeam champion = teamMap.get(season.getChampionTeamId());
        if (champion != null) {
            resp.setChampionTeamName(champion.getTeamName());
            resp.setChampionTeamLogoUrl(urlMap.get(champion.getLogoFileId()));
        }
        BasketTeam runnerUp = teamMap.get(season.getRunnerUpTeamId());
        if (runnerUp != null) {
            resp.setRunnerUpTeamName(runnerUp.getTeamName());
            resp.setRunnerUpTeamLogoUrl(urlMap.get(runnerUp.getLogoFileId()));
        }
        return resp;
    }

    @Override
    public List<PublicHonorItemResp> listSeasonHonors(Long seasonId) {
        publicSupport.requirePublicSeason(seasonId);
        return honorSupport.listBySeason(seasonId);
    }

    private PublicSeasonItemVo toSeasonItem(BasketTournamentSeason season, String tournamentName) {
        PublicSeasonItemVo vo = new PublicSeasonItemVo();
        vo.setSeasonId(season.getId());
        vo.setSeasonNo(season.getSeasonNo());
        vo.setTournamentId(season.getTournamentId());
        vo.setTournamentName(tournamentName);
        vo.setSeasonName(season.getSeasonName());
        vo.setSeasonYear(season.getSeasonYear() == null ? null : String.valueOf(season.getSeasonYear()));
        vo.setSeasonStatus(season.getSeasonStatus());
        vo.setRegistrationStartTime(season.getRegistrationStartTime());
        vo.setRegistrationEndTime(season.getRegistrationEndTime());
        vo.setSeasonStartTime(season.getSeasonStartTime());
        vo.setSeasonEndTime(season.getSeasonEndTime());
        vo.setMaxTeamCount(season.getMaxTeamCount());
        return vo;
    }
}
