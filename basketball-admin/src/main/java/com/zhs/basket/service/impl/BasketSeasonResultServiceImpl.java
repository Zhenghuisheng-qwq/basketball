package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketHonorCategory;
import com.zhs.basket.domain.BasketHonorLineup;
import com.zhs.basket.domain.BasketHonorRecord;
import com.zhs.basket.domain.BasketStatChangeRecord;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.dto.SeasonChampionDto;
import com.zhs.basket.enums.HonorCodeEnum;
import com.zhs.basket.enums.HonorDisplayStatusEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.enums.SeasonResultStatusEnum;
import com.zhs.basket.enums.SeasonStatusEnum;
import com.zhs.basket.mapper.BasketConfirmedMatchAdminMapper;
import com.zhs.basket.mapper.BasketHonorCategoryMapper;
import com.zhs.basket.mapper.BasketHonorLineupMapper;
import com.zhs.basket.mapper.BasketHonorRecordMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketStatChangeRecordMapper;
import com.zhs.basket.mapper.BasketTeamSeasonStatAdminMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentRegistrationMapper;
import com.zhs.basket.mapper.BasketTournamentRegistrationPlayerMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.req.SeasonResultConfirmReq;
import com.zhs.basket.req.SeasonResultRevokeReq;
import com.zhs.basket.resp.HonorLineupResp;
import com.zhs.basket.resp.HonorRecordListResp;
import com.zhs.basket.resp.SeasonCandidateTeamResp;
import com.zhs.basket.resp.SeasonEndCheckResp;
import com.zhs.basket.resp.SeasonResultDetailResp;
import com.zhs.basket.resp.SeasonResultListResp;
import com.zhs.basket.service.IBasketSeasonResultService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.vo.AvailableMatchPlayerVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketSeasonResultServiceImpl implements IBasketSeasonResultService {

    private static final String SOURCE_SEASON_RESULT = "SEASON_RESULT";
    private static final String BIZ_SEASON_RESULT = "SEASON_RESULT";
    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired private BasketConfirmedMatchAdminMapper confirmedMatchAdminMapper;
    @Autowired private BasketTeamSeasonStatAdminMapper teamSeasonStatAdminMapper;
    @Autowired private BasketHonorCategoryMapper honorCategoryMapper;
    @Autowired private BasketHonorRecordMapper honorRecordMapper;
    @Autowired private BasketHonorLineupMapper honorLineupMapper;
    @Autowired private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired private BasketStatChangeRecordMapper statChangeRecordMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<SeasonResultListResp> list(Long tournamentId) {
        List<SeasonResultListResp> list = seasonMapper.selectSeasonResultList(tournamentId);
        for (SeasonResultListResp item : list) {
            fillListStatus(item);
        }
        return list;
    }

    @Override
    public SeasonResultDetailResp getDetail(Long seasonId) {
        BasketTournamentSeason season = requireSeason(seasonId);
        SeasonChampionDto champion = seasonMapper.selectChampionFields(seasonId);
        SeasonResultDetailResp resp = new SeasonResultDetailResp();
        resp.setSeasonId(season.getId());
        resp.setSeasonNo(season.getSeasonNo());
        resp.setSeasonName(season.getSeasonName());
        resp.setSeasonYear(season.getSeasonYear());
        resp.setTournamentId(season.getTournamentId());
        resp.setSeasonStatus(season.getSeasonStatus());
        resp.setSeasonStatusName(SeasonStatusEnum.fromCode(season.getSeasonStatus()).getDescription());
        resp.setUpdateTime(season.getUpdateTime());
        if (season.getTournamentId() != null) {
            BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
            if (tournament != null) {
                resp.setTournamentName(tournament.getTournamentName());
            }
        }
        if (champion != null) {
            resp.setChampionTeamId(champion.getChampionTeamId());
            resp.setChampionTeamName(champion.getChampionTeamName());
            resp.setRunnerUpTeamId(champion.getRunnerUpTeamId());
            resp.setRunnerUpTeamName(champion.getRunnerUpTeamName());
        }
        List<HonorRecordListResp> honors = honorRecordMapper.selectBySeasonAndSource(seasonId, SOURCE_SEASON_RESULT);
        resp.setHonors(honors);
        for (HonorRecordListResp honor : honors) {
            if (HonorCodeEnum.THIRD_PLACE.getCode().equals(honor.getHonorCode())) {
                resp.setThirdPlaceTeamId(honor.getTeamId());
                resp.setThirdPlaceTeamName(honor.getTeamName());
            }
            if (HonorCodeEnum.CHAMPION.getCode().equals(honor.getHonorCode())) {
                List<HonorLineupResp> lineup = honorLineupMapper.selectByHonorRecordId(honor.getId());
                resp.setChampionLineup(lineup);
            }
        }
        SeasonResultStatusEnum resultStatus = resolveResultStatus(season, honors);
        resp.setResultStatus(resultStatus.getCode());
        resp.setResultStatusName(resultStatus.getDescription());
        resp.setConfirmedMatchCount(confirmedMatchAdminMapper.countBySeasonAndStatuses(
                seasonId, MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode()));
        resp.setUnfinishedMatchCount(confirmedMatchAdminMapper.countUnfinished(seasonId));
        resp.setReopenedMatchCount(confirmedMatchAdminMapper.countReopened(seasonId));
        resp.setFinishedUnconfirmedCount(confirmedMatchAdminMapper.countFinishedUnconfirmed(seasonId));
        resp.setTeamSeasonStatCount(teamSeasonStatAdminMapper.countBySeason(seasonId));
        return resp;
    }

    @Override
    public List<SeasonCandidateTeamResp> listCandidateTeams(Long seasonId) {
        requireSeason(seasonId);
        return registrationMapper.selectCandidateTeamsBySeasonId(seasonId);
    }

    @Override
    public SeasonEndCheckResp endCheck(Long seasonId) {
        requireSeason(seasonId);
        SeasonEndCheckResp resp = buildEndCheck(seasonId);
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season.getChampionTeamId() != null) {
            resp.setCanConfirm(false);
            resp.getBlockers().add("赛季已确认冠亚军，请先撤销后再确认");
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long seasonId, SeasonResultConfirmReq req) {
        if (req == null || req.getChampionTeamId() == null || req.getRunnerUpTeamId() == null) {
            throw new ServiceException("冠军和亚军不能为空");
        }
        if (Objects.equals(req.getChampionTeamId(), req.getRunnerUpTeamId())) {
            throw new ServiceException("冠军和亚军不能为同一支球队");
        }
        if (req.getThirdPlaceTeamId() != null
                && (Objects.equals(req.getThirdPlaceTeamId(), req.getChampionTeamId())
                || Objects.equals(req.getThirdPlaceTeamId(), req.getRunnerUpTeamId()))) {
            throw new ServiceException("季军不能与冠军或亚军相同");
        }

        BasketTournamentSeason season = requireSeason(seasonId);
        if (season.getChampionTeamId() != null) {
            throw new ServiceException("赛季已有冠军，请先撤销后再确认");
        }

        SeasonEndCheckResp check = buildEndCheck(seasonId);
        if (!check.isCanConfirm()) {
            throw new ServiceException(String.join("；", check.getBlockers()));
        }
        if (check.isRequireConfirmRemark() && StringUtils.isEmpty(req.getConfirmRemark())) {
            throw new ServiceException("无已确认比赛时，确认说明不能为空");
        }

        requireApprovedTeam(seasonId, req.getChampionTeamId(), "冠军");
        requireApprovedTeam(seasonId, req.getRunnerUpTeamId(), "亚军");
        if (req.getThirdPlaceTeamId() != null) {
            requireApprovedTeam(seasonId, req.getThirdPlaceTeamId(), "季军");
        }

        int updated = seasonMapper.updateChampionIfAbsent(seasonId, req.getChampionTeamId(), req.getRunnerUpTeamId());
        if (updated != 1) {
            throw new ServiceException("确认失败，可能已被其他人确认，请刷新后重试");
        }

        upsertSeasonHonor(season, HonorCodeEnum.CHAMPION, req.getChampionTeamId(), req.getConfirmRemark());
        upsertSeasonHonor(season, HonorCodeEnum.RUNNER_UP, req.getRunnerUpTeamId(), req.getConfirmRemark());
        if (req.getThirdPlaceTeamId() != null) {
            upsertSeasonHonor(season, HonorCodeEnum.THIRD_PLACE, req.getThirdPlaceTeamId(), req.getConfirmRemark());
        } else {
            revokeThirdPlaceDraft(seasonId);
        }

        BasketHonorRecord championHonor = honorRecordMapper.selectBySeasonCategorySource(
                seasonId, requireCategory(HonorCodeEnum.CHAMPION).getId(), SOURCE_SEASON_RESULT);
        if (championHonor != null) {
            rewriteChampionLineup(seasonId, req.getChampionTeamId(), championHonor.getId());
        }

        writeAudit(seasonId, "champion_team_id", null, String.valueOf(req.getChampionTeamId()),
                StringUtils.isEmpty(req.getConfirmRemark()) ? "确认赛季名次" : req.getConfirmRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long seasonId) {
        BasketTournamentSeason season = requireSeason(seasonId);
        if (season.getChampionTeamId() == null) {
            throw new ServiceException("请先确认赛季名次再发布");
        }
        int rows = honorRecordMapper.updateDisplayStatusBySeasonSource(
                seasonId, SOURCE_SEASON_RESULT,
                Arrays.asList(HonorDisplayStatusEnum.DRAFT.getCode()),
                HonorDisplayStatusEnum.PUBLISHED.getCode());
        if (rows < 1) {
            throw new ServiceException("没有可发布的赛季荣誉（需为草稿状态）");
        }
        writeAudit(seasonId, "display_status",
                String.valueOf(HonorDisplayStatusEnum.DRAFT.getCode()),
                String.valueOf(HonorDisplayStatusEnum.PUBLISHED.getCode()),
                "发布赛季名次荣誉");
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long seasonId, SeasonResultRevokeReq req) {
        if (req == null || StringUtils.isEmpty(req.getRevokeReason())) {
            throw new ServiceException("撤销原因不能为空");
        }
        BasketTournamentSeason season = requireSeason(seasonId);
        if (season.getChampionTeamId() == null) {
            throw new ServiceException("当前赛季未确认冠亚军，无需撤销");
        }
        Long championTeamId = season.getChampionTeamId();
        Long runnerUpTeamId = season.getRunnerUpTeamId();

        honorRecordMapper.updateDisplayStatusBySeasonSource(
                seasonId, SOURCE_SEASON_RESULT,
                Arrays.asList(HonorDisplayStatusEnum.DRAFT.getCode(), HonorDisplayStatusEnum.PUBLISHED.getCode(),
                        HonorDisplayStatusEnum.OFFLINE.getCode()),
                HonorDisplayStatusEnum.REVOKED.getCode());

        int cleared = seasonMapper.clearChampionIfMatch(seasonId, championTeamId, runnerUpTeamId);
        if (cleared != 1) {
            throw new ServiceException("撤销失败，名次可能已变更，请刷新后重试");
        }
        writeAudit(seasonId, "champion_team_id", String.valueOf(championTeamId), null, req.getRevokeReason());
        publicCacheCleaner.clearAfterCommit();
    }

    private SeasonEndCheckResp buildEndCheck(Long seasonId) {
        SeasonEndCheckResp resp = new SeasonEndCheckResp();
        resp.setSeasonId(seasonId);
        int confirmed = confirmedMatchAdminMapper.countBySeasonAndStatuses(
                seasonId, MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode());
        int unfinished = confirmedMatchAdminMapper.countUnfinished(seasonId);
        int reopened = confirmedMatchAdminMapper.countReopened(seasonId);
        int finishedUnconfirmed = confirmedMatchAdminMapper.countFinishedUnconfirmed(seasonId);
        int teamStatCount = teamSeasonStatAdminMapper.countBySeason(seasonId);
        resp.setConfirmedMatchCount(confirmed);
        resp.setUnfinishedMatchCount(unfinished);
        resp.setReopenedMatchCount(reopened);
        resp.setFinishedUnconfirmedCount(finishedUnconfirmed);
        resp.setTeamSeasonStatCount(teamStatCount);

        boolean canConfirm = true;
        if (finishedUnconfirmed > 0) {
            canConfirm = false;
            resp.getBlockers().add("存在已结束但未确认的比赛，无法确认名次");
        }
        if (reopened > 0) {
            canConfirm = false;
            resp.getBlockers().add("存在重开中的比赛数据，无法确认名次");
        }
        if (unfinished > 0) {
            canConfirm = false;
            resp.getBlockers().add("存在未完成的比赛，无法确认名次");
        }
        if (confirmed == 0) {
            resp.setRequireConfirmRemark(true);
            resp.getWarnings().add("当前无已确认比赛，确认时需填写说明");
            if (teamStatCount <= 0) {
                resp.getWarnings().add("当前亦无赛季统计数据");
            }
        } else if (teamStatCount <= 0) {
            resp.getWarnings().add("赛季统计尚未生成，建议先执行数据结算");
        }
        resp.setCanConfirm(canConfirm);
        return resp;
    }

    private void upsertSeasonHonor(BasketTournamentSeason season, HonorCodeEnum code, Long teamId, String remark) {
        BasketHonorCategory category = requireCategory(code);
        BasketHonorRecord existing = honorRecordMapper.selectBySeasonCategorySource(
                season.getId(), category.getId(), SOURCE_SEASON_RESULT);
        Date now = new Date();
        if (existing != null) {
            existing.setTeamId(teamId);
            existing.setHonorTitle(buildHonorTitle(season, code));
            existing.setHonorSubtitle(season.getSeasonName());
            existing.setAwardDate(now);
            existing.setDisplayStatus(HonorDisplayStatusEnum.DRAFT.getCode());
            existing.setRemark(remark);
            existing.setSourceType(SOURCE_SEASON_RESULT);
            existing.setSourceId(season.getId());
            honorRecordMapper.updateById(existing);
            return;
        }
        BasketHonorRecord record = new BasketHonorRecord();
        record.setTournamentId(season.getTournamentId());
        record.setSeasonId(season.getId());
        record.setHonorCategoryId(category.getId());
        record.setTeamId(teamId);
        record.setHonorTitle(buildHonorTitle(season, code));
        record.setHonorSubtitle(season.getSeasonName());
        record.setAwardDate(now);
        record.setSourceType(SOURCE_SEASON_RESULT);
        record.setSourceId(season.getId());
        record.setDisplayStatus(HonorDisplayStatusEnum.DRAFT.getCode());
        record.setDisplayOrder(category.getDisplayOrder() == null ? 0 : category.getDisplayOrder());
        record.setRemark(remark);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        honorRecordMapper.insert(record);
    }

    private void revokeThirdPlaceDraft(Long seasonId) {
        BasketHonorCategory category = honorCategoryMapper.selectByCode(HonorCodeEnum.THIRD_PLACE.getCode());
        if (category == null) {
            return;
        }
        BasketHonorRecord existing = honorRecordMapper.selectBySeasonCategorySource(
                seasonId, category.getId(), SOURCE_SEASON_RESULT);
        if (existing == null) {
            return;
        }
        HonorDisplayStatusEnum status = HonorDisplayStatusEnum.fromCode(existing.getDisplayStatus());
        if (status == HonorDisplayStatusEnum.DRAFT || status == HonorDisplayStatusEnum.OFFLINE) {
            honorRecordMapper.updateDisplayStatusIfMatch(existing.getId(), existing.getDisplayStatus(),
                    HonorDisplayStatusEnum.REVOKED.getCode());
        }
    }

    private void rewriteChampionLineup(Long seasonId, Long championTeamId, Long honorRecordId) {
        honorLineupMapper.softDeleteByHonorRecordId(honorRecordId);
        List<AvailableMatchPlayerVo> players =
                registrationPlayerMapper.selectAvailableBySeasonAndTeam(seasonId, championTeamId);
        if (players == null || players.isEmpty()) {
            return;
        }
        Date now = new Date();
        java.util.ArrayList<BasketHonorLineup> rows = new java.util.ArrayList<>();
        int sort = 1;
        for (AvailableMatchPlayerVo player : players) {
            Long memberId = player.getMemberId();
            if (memberId == null && player.getPlayerId() != null) {
                var profile = playerProfileMapper.selectById(player.getPlayerId());
                if (profile != null) {
                    memberId = profile.getMemberId();
                }
            }
            if (memberId == null || player.getPlayerId() == null) {
                continue;
            }
            BasketHonorLineup lineup = new BasketHonorLineup();
            lineup.setHonorRecordId(honorRecordId);
            lineup.setTeamId(championTeamId);
            lineup.setPlayerId(player.getPlayerId());
            lineup.setMemberId(memberId);
            lineup.setRoleType(StringUtils.isEmpty(player.getRosterRole()) ? "PLAYER" : player.getRosterRole());
            lineup.setJerseyNo(player.getJerseyNo());
            var profile = playerProfileMapper.selectById(player.getPlayerId());
            if (profile != null) {
                lineup.setPositionCode(profile.getPositionCode());
            }
            lineup.setSortNo(sort++);
            lineup.setCreateTime(now);
            lineup.setUpdateTime(now);
            lineup.setIsDelete(NOT_DELETED);
            rows.add(lineup);
        }
        if (!rows.isEmpty()) {
            honorLineupMapper.batchInsert(rows);
        }
    }

    private String buildHonorTitle(BasketTournamentSeason season, HonorCodeEnum code) {
        return season.getSeasonName() + " " + code.getName();
    }

    private BasketHonorCategory requireCategory(HonorCodeEnum code) {
        BasketHonorCategory category = honorCategoryMapper.selectByCode(code.getCode());
        if (category == null) {
            throw new ServiceException("荣誉分类不存在: " + code.getCode());
        }
        return category;
    }

    private void requireApprovedTeam(Long seasonId, Long teamId, String label) {
        if (registrationMapper.selectApprovedBySeasonAndTeam(seasonId, teamId) == null) {
            throw new ServiceException(label + "球队未通过当前赛季报名审核");
        }
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季ID不能为空");
        }
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("赛季不存在或已删除");
        }
        return season;
    }

    private void fillListStatus(SeasonResultListResp item) {
        try {
            item.setSeasonStatusName(SeasonStatusEnum.fromCode(item.getSeasonStatus()).getDescription());
        } catch (Exception ignored) {
            // ignore
        }
        BasketTournamentSeason season = new BasketTournamentSeason();
        season.setId(item.getSeasonId());
        season.setChampionTeamId(item.getChampionTeamId());
        List<HonorRecordListResp> honors =
                honorRecordMapper.selectBySeasonAndSource(item.getSeasonId(), SOURCE_SEASON_RESULT);
        SeasonResultStatusEnum status = resolveResultStatus(season, honors);
        item.setResultStatus(status.getCode());
        item.setResultStatusName(status.getDescription());
    }

    private SeasonResultStatusEnum resolveResultStatus(BasketTournamentSeason season, List<HonorRecordListResp> honors) {
        if (season.getChampionTeamId() == null) {
            if (honors != null) {
                boolean revoked = honors.stream()
                        .anyMatch(h -> Objects.equals(h.getDisplayStatus(), HonorDisplayStatusEnum.REVOKED.getCode()));
                if (revoked) {
                    return SeasonResultStatusEnum.REVOKED;
                }
            }
            return SeasonResultStatusEnum.DRAFT;
        }
        boolean published = honors != null && honors.stream()
                .anyMatch(h -> Objects.equals(h.getDisplayStatus(), HonorDisplayStatusEnum.PUBLISHED.getCode()));
        if (published) {
            return SeasonResultStatusEnum.PUBLISHED;
        }
        return SeasonResultStatusEnum.CONFIRMED;
    }

    private void writeAudit(Long seasonId, String fieldName, String before, String after, String reason) {
        BasketStatChangeRecord record = new BasketStatChangeRecord();
        record.setBizType(BIZ_SEASON_RESULT);
        record.setBizId(seasonId);
        record.setFieldName(fieldName);
        record.setBeforeValue(before);
        record.setAfterValue(after);
        record.setChangeReason(reason);
        try {
            record.setOperatorId(SecurityUtils.getUserId());
            record.setOperatorName(SecurityUtils.getUsername());
        } catch (Exception ex) {
            record.setOperatorId(0L);
            record.setOperatorName("system");
        }
        record.setApprovalStatus(0);
        Date now = new Date();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        statChangeRecordMapper.insert(record);
    }
}
