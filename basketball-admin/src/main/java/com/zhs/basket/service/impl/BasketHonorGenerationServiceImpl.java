package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketHonorCategory;
import com.zhs.basket.domain.BasketHonorGenerationBatch;
import com.zhs.basket.domain.BasketHonorRecord;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.HonorCodeEnum;
import com.zhs.basket.enums.HonorDisplayStatusEnum;
import com.zhs.basket.enums.PlayerRankTypeEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.mapper.BasketHonorGenerationBatchMapper;
import com.zhs.basket.mapper.BasketHonorRecordMapper;
import com.zhs.basket.mapper.BasketPlayerRankAdminMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.req.HonorGenerationConfirmReq;
import com.zhs.basket.resp.HonorGenerationBatchResp;
import com.zhs.basket.resp.HonorGenerationPreviewItemResp;
import com.zhs.basket.resp.HonorGenerationPreviewResp;
import com.zhs.basket.service.IBasketHonorGenerationService;
import com.zhs.basket.support.BasketHonorCategoryCatalog;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.vo.PlayerRankListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class BasketHonorGenerationServiceImpl implements IBasketHonorGenerationService {

    private static final String SOURCE_AUTO = "AUTO_SETTLEMENT";
    private static final String BATCH_STATUS_RUNNING = "RUNNING";
    private static final String BATCH_STATUS_COMPLETED = "COMPLETED";
    private static final String BATCH_STATUS_REVOKED = "REVOKED";
    private static final Integer NOT_DELETED = 0;

    private static final HonorCodeEnum[] PLAYER_HONORS = {
            HonorCodeEnum.SCORING_CHAMPION,
            HonorCodeEnum.REBOUND_CHAMPION,
            HonorCodeEnum.ASSIST_CHAMPION,
            HonorCodeEnum.STEAL_CHAMPION,
            HonorCodeEnum.BLOCK_CHAMPION
    };

    @Autowired private BasketHonorGenerationBatchMapper generationBatchMapper;
    @Autowired private BasketHonorRecordMapper honorRecordMapper;
    @Autowired private BasketHonorCategoryCatalog honorCategoryCatalog;
    @Autowired private BasketPlayerRankAdminMapper playerRankAdminMapper;
    @Autowired private BasketSeasonMapper seasonMapper;
    @Autowired private BasketSeriesMapper seriesMapper;
    @Autowired private BasketDivisionMapper divisionMapper;
    @Autowired private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired private BasketEventTeamMapper eventTeamMapper;
    @Autowired private BasketV4MatchMapper matchMapper;
    @Autowired private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public HonorGenerationPreviewResp preview(Long seasonId) {
        BasketSeason season = requireSeason(seasonId);
        HonorGenerationPreviewResp resp = new HonorGenerationPreviewResp();
        resp.setSeasonId(season.getId());
        resp.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
        resp.setTournamentId(season.getSeriesId());
        if (season.getSeriesId() != null) {
            BasketSeries series = seriesMapper.selectById(season.getSeriesId());
            if (series != null) {
                resp.setTournamentName(series.getSeriesName());
            }
        }
        BasketHonorGenerationBatch current = generationBatchMapper.selectCurrentBySeason(seasonId);
        if (current != null) {
            HonorGenerationBatchResp batchResp = new HonorGenerationBatchResp();
            batchResp.setId(current.getId());
            batchResp.setBatchNo(current.getBatchNo());
            batchResp.setSeasonId(current.getSeasonId());
            batchResp.setBatchStatus(current.getBatchStatus());
            batchResp.setHonorCount(current.getHonorCount());
            batchResp.setCurrentFlag(current.getCurrentFlag());
            batchResp.setCreateTime(current.getCreateTime());
            resp.setCurrentBatch(batchResp);
        }
        List<HonorGenerationPreviewItemResp> items = new ArrayList<>();
        appendPlacementItems(season, items);
        for (HonorCodeEnum honorCode : PLAYER_HONORS) {
            items.add(buildPreviewItem(season, honorCode));
        }
        resp.setItems(items);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HonorGenerationBatchResp confirm(HonorGenerationConfirmReq req) {
        if (req == null || req.getSeasonId() == null) {
            throw new ServiceException("赛季不能为空");
        }
        Long seasonId = req.getSeasonId();
        BasketSeason season = requireSeason(seasonId);
        HonorGenerationPreviewResp preview = preview(seasonId);
        List<HonorGenerationPreviewItemResp> selected = preview.getItems().stream()
                .filter(HonorGenerationPreviewItemResp::isSelected)
                .toList();
        if (selected.isEmpty()) {
            HonorGenerationBatchResp skipped = new HonorGenerationBatchResp();
            skipped.setSeasonId(seasonId);
            skipped.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
            skipped.setBatchStatus("SKIPPED");
            skipped.setHonorCount(0);
            skipped.setCurrentFlag(0);
            return skipped;
        }

        Date now = new Date();
        generationBatchMapper.clearCurrentBySeason(seasonId);
        BasketHonorGenerationBatch batch = new BasketHonorGenerationBatch();
        batch.setBatchNo(generateBatchNo());
        batch.setSeasonId(seasonId);
        batch.setBatchStatus(BATCH_STATUS_RUNNING);
        batch.setHonorCount(0);
        try {
            batch.setOperatorId(SecurityUtils.getUserId());
        } catch (Exception ex) {
            batch.setOperatorId(0L);
        }
        batch.setCurrentFlag(1);
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        generationBatchMapper.insert(batch);

        int created = 0;
        for (HonorGenerationPreviewItemResp item : selected) {
            BasketHonorCategory category = honorCategoryCatalog.requireByCode(item.getHonorCode());
            if (placementExists(seasonId, category.getId(), item)) {
                continue;
            }
            boolean placement = isPlacement(item.getHonorCode());
            BasketHonorRecord record = new BasketHonorRecord();
            record.setSeriesId(season.getSeriesId());
            record.setSeasonId(seasonId);
            record.setHonorCategoryId(category.getId());
            record.setTeamId(item.getTeamId());
            record.setPlayerId(item.getPlayerId());
            record.setHonorTitle(category.getHonorName());
            record.setHonorValue(item.getStatValue() == null ? null : item.getStatValue().toPlainString());
            record.setAwardDate(placement ? item.getAwardDate() : now);
            record.setSourceType(SOURCE_AUTO);
            record.setSourceId(placement && item.getSourceId() != null ? item.getSourceId() : batch.getId());
            record.setGenerationBatchId(batch.getId());
            record.setDisplayStatus(HonorDisplayStatusEnum.DRAFT.getCode());
            record.setDisplayOrder(0);
            record.setRemark(placement
                    ? "赛后候选 " + batch.getBatchNo() + "，来自已确认晋级结果 " + item.getSourceId()
                    : "自动生成批次 " + batch.getBatchNo());
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setIsDelete(NOT_DELETED);
            honorRecordMapper.insert(record);
            created++;
        }
        generationBatchMapper.updateStatus(batch.getId(), BATCH_STATUS_COMPLETED, created, null, 1);
        publicCacheCleaner.clearAfterCommit();

        HonorGenerationBatchResp resp = new HonorGenerationBatchResp();
        resp.setId(batch.getId());
        resp.setBatchNo(batch.getBatchNo());
        resp.setSeasonId(seasonId);
        resp.setSeasonName(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
        resp.setBatchStatus(BATCH_STATUS_COMPLETED);
        resp.setHonorCount(created);
        resp.setCurrentFlag(1);
        resp.setCreateTime(now);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeBatch(Long batchId) {
        BasketHonorGenerationBatch batch = generationBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new ServiceException("荣誉生成批次不存在");
        }
        honorRecordMapper.revokeByGenerationBatch(batchId);
        generationBatchMapper.updateStatus(batchId, BATCH_STATUS_REVOKED, batch.getHonorCount(), null, 0);
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    public List<HonorGenerationBatchResp> listBatches(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季不能为空");
        }
        return generationBatchMapper.selectBySeason(seasonId);
    }

    private HonorGenerationPreviewItemResp buildPreviewItem(BasketSeason season, HonorCodeEnum honorCode) {
        HonorGenerationPreviewItemResp item = new HonorGenerationPreviewItemResp();
        item.setHonorCode(honorCode.getCode());
        item.setHonorName(honorCode.getName());
        BasketHonorCategory category = honorCategoryCatalog.requireByCode(honorCode.getCode());
        item.setHonorCategoryId(category.getId());
        PlayerRankListVo top;
        try {
            top = findTopPlayer(season.getId(), honorCode);
        } catch (DataAccessException ex) {
            item.setSkipReason("球员榜暂不可用，不影响已确认的冠亚军候选");
            item.setSelected(false);
            return item;
        }
        if (top == null || top.getPlayerId() == null) {
            item.setSkipReason("球员榜暂无数据");
            item.setSelected(false);
            return item;
        }
        int exists = honorRecordMapper.countActiveUnique(season.getId(), category.getId(), top.getTeamId(), top.getPlayerId(), null);
        if (exists > 0) {
            item.setSkipReason("该球员已有同类型荣誉，不重复生成");
            item.setSelected(false);
            return item;
        }
        item.setPlayerId(top.getPlayerId());
        item.setPlayerName(top.getDisplayName());
        item.setTeamId(top.getTeamId());
        item.setTeamName(top.getTeamName());
        item.setStatValue(top.getStatValue());
        item.setRankNo(top.getRankNo());
        item.setSelected(true);
        return item;
    }

    private PlayerRankListVo findTopPlayer(Long seasonId, HonorCodeEnum honorCode) {
        RankingQuery query = new RankingQuery();
        query.setSeasonId(seasonId);
        query.setRankType(mapHonorToRankType(honorCode).getCode());
        List<PlayerRankListVo> ranks = playerRankAdminMapper.selectLatestList(query);
        if (ranks == null || ranks.isEmpty()) {
            return null;
        }
        return ranks.get(0);
    }

    private PlayerRankTypeEnum mapHonorToRankType(HonorCodeEnum honorCode) {
        return switch (honorCode) {
            case SCORING_CHAMPION -> PlayerRankTypeEnum.AVG_POINTS;
            case REBOUND_CHAMPION -> PlayerRankTypeEnum.AVG_REBOUNDS;
            case ASSIST_CHAMPION -> PlayerRankTypeEnum.AVG_ASSISTS;
            case STEAL_CHAMPION -> PlayerRankTypeEnum.AVG_STEALS;
            case BLOCK_CHAMPION -> PlayerRankTypeEnum.AVG_BLOCKS;
            default -> throw new ServiceException("不支持的荣誉类型: " + honorCode.getCode());
        };
    }

    private void appendPlacementItems(BasketSeason season, List<HonorGenerationPreviewItemResp> items) {
        List<BasketDivisionStageQualification> champions = qualificationMapper.selectConfirmedChampionsBySeasonId(season.getId());
        Map<Long, BasketDivision> divisions = new HashMap<>();
        List<BasketDivision> divisionRows = divisionMapper.selectBySeasonId(season.getId());
        if (divisionRows != null) for (BasketDivision division : divisionRows) {
            if (division != null && division.getId() != null) {
                divisions.put(division.getId(), division);
            }
        }
        if (champions == null || champions.isEmpty()) {
            items.add(skippedPlacement(HonorCodeEnum.CHAMPION, "没有已结束赛区的已确认冠军，不能用积分榜推断"));
            items.add(skippedPlacement(HonorCodeEnum.RUNNER_UP, "没有已确认的决赛冠军，不能生成亚军"));
            items.add(skippedPlacement(HonorCodeEnum.THIRD_PLACE, "没有季军赛或正式季军认定，不自动生成"));
            return;
        }
        Map<Long, List<BasketDivisionStageQualification>> byDivision = new LinkedHashMap<>();
        for (BasketDivisionStageQualification row : champions) {
            byDivision.computeIfAbsent(row.getDivisionId(), key -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<Long, List<BasketDivisionStageQualification>> entry : byDivision.entrySet()) {
            BasketDivision division = divisions.get(entry.getKey());
            String divisionName = division == null ? null : division.getDivisionName();
            if (entry.getValue().size() != 1) {
                HonorGenerationPreviewItemResp ambiguous = skippedPlacement(HonorCodeEnum.CHAMPION, "该赛区已确认冠军不唯一，不能自动选择");
                ambiguous.setDivisionId(entry.getKey());
                ambiguous.setDivisionName(divisionName);
                items.add(ambiguous);
                continue;
            }
            BasketDivisionStageQualification champion = entry.getValue().get(0);
            HonorGenerationPreviewItemResp championItem = placementItem(season, HonorCodeEnum.CHAMPION, champion, divisionName);
            items.add(championItem);
            items.add(runnerUpItem(season, champion, divisionName, championItem.isSelected()));
        }
        items.add(skippedPlacement(HonorCodeEnum.THIRD_PLACE, "没有季军赛或正式季军认定，不自动生成"));
    }

    private HonorGenerationPreviewItemResp placementItem(BasketSeason season, HonorCodeEnum honorCode,
                                                         BasketDivisionStageQualification qualification, String divisionName) {
        HonorGenerationPreviewItemResp item = basePlacement(honorCode);
        item.setDivisionId(qualification.getDivisionId());
        item.setDivisionName(divisionName);
        item.setEventTeamId(qualification.getEventTeamId());
        item.setSourceId(qualification.getId());
        item.setAwardDate(qualification.getConfirmedTime());
        BasketEventTeam eventTeam = eventTeamMapper.selectById(qualification.getEventTeamId());
        if (eventTeam == null || !Objects.equals(eventTeam.getIsDelete(), NOT_DELETED) || eventTeam.getSourceTeamId() == null) {
            item.setSkipReason("冠军参赛球队没有可关联的长期球队");
            item.setSelected(false);
            return item;
        }
        BasketHonorCategory category = honorCategoryCatalog.requireByCode(honorCode.getCode());
        if (honorRecordMapper.countAnyTeamHonor(season.getId(), category.getId(), eventTeam.getSourceTeamId()) > 0) {
            item.setTeamId(eventTeam.getSourceTeamId());
            item.setTeamName(eventTeam.getTeamNameSnapshot());
            item.setSkipReason("该赛季已有相同获奖球队的荣誉，不覆盖、不重复生成");
            item.setSelected(false);
            return item;
        }
        item.setTeamId(eventTeam.getSourceTeamId());
        item.setTeamName(eventTeam.getTeamNameSnapshot());
        item.setSelected(true);
        if (item.getAwardDate() == null) {
            item.setSkipReason("已确认冠军，但没有确认时间。保存后授予日期为空，需管理员补录");
        }
        return item;
    }

    private HonorGenerationPreviewItemResp runnerUpItem(BasketSeason season, BasketDivisionStageQualification champion,
                                                        String divisionName, boolean championReady) {
        HonorGenerationPreviewItemResp item = basePlacement(HonorCodeEnum.RUNNER_UP);
        item.setDivisionId(champion.getDivisionId());
        item.setDivisionName(divisionName);
        if (!championReady) {
            item.setSkipReason("冠军候选不可用，亚军不单独生成");
            item.setSelected(false);
            return item;
        }
        Long loserEventTeamId = resolveFinalLoser(champion);
        if (loserEventTeamId == null) {
            item.setSkipReason("决赛负方不明确，亚军保持人工维护");
            item.setSelected(false);
            return item;
        }
        BasketEventTeam loser = eventTeamMapper.selectById(loserEventTeamId);
        if (loser == null || !Objects.equals(loser.getIsDelete(), NOT_DELETED) || loser.getSourceTeamId() == null) {
            item.setSkipReason("决赛负方没有可关联的长期球队");
            item.setSelected(false);
            return item;
        }
        BasketHonorCategory category = honorCategoryCatalog.requireByCode(HonorCodeEnum.RUNNER_UP.getCode());
        item.setTeamId(loser.getSourceTeamId());
        item.setTeamName(loser.getTeamNameSnapshot());
        item.setEventTeamId(loser.getId());
        item.setSourceId(champion.getId());
        item.setAwardDate(champion.getConfirmedTime());
        if (honorRecordMapper.countAnyTeamHonor(season.getId(), category.getId(), loser.getSourceTeamId()) > 0) {
            item.setSkipReason("该赛季已有相同亚军球队的荣誉，不覆盖、不重复生成");
            item.setSelected(false);
            return item;
        }
        item.setSelected(true);
        return item;
    }

    private Long resolveFinalLoser(BasketDivisionStageQualification champion) {
        if (champion.getSourceSeriesId() != null) {
            BasketMatchSeries series = matchSeriesMapper.selectById(champion.getSourceSeriesId());
            if (series == null || !Objects.equals(series.getWinnerEventTeamId(), champion.getEventTeamId())) {
                return null;
            }
            if (Objects.equals(series.getHomeEventTeamId(), champion.getEventTeamId())) {
                return series.getAwayEventTeamId();
            }
            if (Objects.equals(series.getAwayEventTeamId(), champion.getEventTeamId())) {
                return series.getHomeEventTeamId();
            }
            return null;
        }
        List<BasketV4Match> finished = matchMapper.selectFinishedByStageId(champion.getStageId());
        if (finished == null || finished.isEmpty()) {
            return null;
        }
        Long loser = null;
        for (BasketV4Match match : finished) {
            if (!Objects.equals(match.getWinnerEventTeamId(), champion.getEventTeamId()) || match.getLoserEventTeamId() == null) {
                continue;
            }
            if (!MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())
                    && !MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus())) {
                continue;
            }
            if (loser != null && !Objects.equals(loser, match.getLoserEventTeamId())) {
                return null;
            }
            loser = match.getLoserEventTeamId();
        }
        return loser;
    }

    private HonorGenerationPreviewItemResp skippedPlacement(HonorCodeEnum honorCode, String reason) {
        HonorGenerationPreviewItemResp item = basePlacement(honorCode);
        item.setSkipReason(reason);
        item.setSelected(false);
        return item;
    }

    private HonorGenerationPreviewItemResp basePlacement(HonorCodeEnum honorCode) {
        HonorGenerationPreviewItemResp item = new HonorGenerationPreviewItemResp();
        item.setHonorCode(honorCode.getCode());
        item.setHonorName(honorCode.getName());
        BasketHonorCategory category = honorCategoryCatalog.requireByCode(honorCode.getCode());
        item.setHonorCategoryId(category.getId());
        item.setSelected(false);
        return item;
    }

    private boolean isPlacement(String honorCode) {
        return HonorCodeEnum.CHAMPION.getCode().equals(honorCode)
                || HonorCodeEnum.RUNNER_UP.getCode().equals(honorCode)
                || HonorCodeEnum.THIRD_PLACE.getCode().equals(honorCode);
    }

    private boolean placementExists(Long seasonId, Long categoryId, HonorGenerationPreviewItemResp item) {
        if (isPlacement(item.getHonorCode())) {
            return item.getTeamId() != null
                    && honorRecordMapper.countAnyTeamHonor(seasonId, categoryId, item.getTeamId()) > 0;
        }
        return honorRecordMapper.countActiveUnique(seasonId, categoryId, item.getTeamId(), item.getPlayerId(), null) > 0;
    }

    private BasketSeason requireSeason(Long seasonId) {
        BasketSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private String generateBatchNo() {
        String prefix = "HGB" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int seq = generationBatchMapper.countByNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }
}
