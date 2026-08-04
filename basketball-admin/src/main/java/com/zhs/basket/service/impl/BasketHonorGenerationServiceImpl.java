package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketHonorCategory;
import com.zhs.basket.domain.BasketHonorGenerationBatch;
import com.zhs.basket.domain.BasketHonorRecord;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.enums.HonorCodeEnum;
import com.zhs.basket.enums.HonorDisplayStatusEnum;
import com.zhs.basket.enums.PlayerRankTypeEnum;
import com.zhs.basket.mapper.BasketHonorCategoryMapper;
import com.zhs.basket.mapper.BasketHonorGenerationBatchMapper;
import com.zhs.basket.mapper.BasketHonorRecordMapper;
import com.zhs.basket.mapper.BasketPlayerRankAdminMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.req.HonorGenerationConfirmReq;
import com.zhs.basket.resp.HonorGenerationBatchResp;
import com.zhs.basket.resp.HonorGenerationPreviewItemResp;
import com.zhs.basket.resp.HonorGenerationPreviewResp;
import com.zhs.basket.service.IBasketHonorGenerationService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.vo.PlayerRankListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    @Autowired private BasketHonorCategoryMapper honorCategoryMapper;
    @Autowired private BasketPlayerRankAdminMapper playerRankAdminMapper;
    @Autowired private BasketSeasonMapper seasonMapper;
    @Autowired private BasketSeriesMapper seriesMapper;
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
            throw new ServiceException("没有可生成的球员荣誉，请先完成数据结算与球员榜");
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
            BasketHonorCategory category = honorCategoryMapper.selectByCode(item.getHonorCode());
            if (category == null) {
                continue;
            }
            BasketHonorRecord record = new BasketHonorRecord();
            record.setSeriesId(season.getSeriesId());
            record.setSeasonId(seasonId);
            record.setHonorCategoryId(category.getId());
            record.setTeamId(item.getTeamId());
            record.setPlayerId(item.getPlayerId());
            record.setHonorTitle(category.getHonorName());
            record.setHonorValue(item.getStatValue() == null ? null : item.getStatValue().toPlainString());
            record.setAwardDate(now);
            record.setSourceType(SOURCE_AUTO);
            record.setSourceId(batch.getId());
            record.setGenerationBatchId(batch.getId());
            record.setDisplayStatus(HonorDisplayStatusEnum.DRAFT.getCode());
            record.setDisplayOrder(0);
            record.setRemark("自动生成批次 " + batch.getBatchNo());
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
        BasketHonorCategory category = honorCategoryMapper.selectByCode(honorCode.getCode());
        if (category != null) {
            item.setHonorCategoryId(category.getId());
            int exists = honorRecordMapper.countActiveUnique(season.getId(), category.getId(), null, null, null);
            if (exists > 0) {
                item.setSkipReason("该赛季已存在同类型荣誉");
                item.setSelected(false);
                return item;
            }
        }
        PlayerRankListVo top = findTopPlayer(season.getId(), honorCode);
        if (top == null || top.getPlayerId() == null) {
            item.setSkipReason("球员榜暂无数据");
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
