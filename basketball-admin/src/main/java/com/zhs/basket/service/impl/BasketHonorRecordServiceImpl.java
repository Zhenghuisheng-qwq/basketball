package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketHonorCategory;
import com.zhs.basket.domain.BasketHonorLineup;
import com.zhs.basket.domain.BasketHonorRecord;
import com.zhs.basket.domain.BasketStatChangeRecord;
import com.zhs.basket.enums.HonorCodeEnum;
import com.zhs.basket.enums.HonorDisplayStatusEnum;
import com.zhs.basket.enums.PlayerRankTypeEnum;
import com.zhs.basket.mapper.BasketHonorCategoryMapper;
import com.zhs.basket.mapper.BasketHonorLineupMapper;
import com.zhs.basket.mapper.BasketHonorRecordMapper;
import com.zhs.basket.mapper.BasketPlayerRankAdminMapper;
import com.zhs.basket.mapper.BasketStatChangeRecordMapper;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.req.HonorRecordAddReq;
import com.zhs.basket.req.HonorRecordEditReq;
import com.zhs.basket.req.HonorRecordQueryReq;
import com.zhs.basket.req.HonorRecordRevokeReq;
import com.zhs.basket.req.PlayerHonorCandidateQueryReq;
import com.zhs.basket.resp.HonorLineupResp;
import com.zhs.basket.resp.HonorRecordDetailResp;
import com.zhs.basket.resp.HonorRecordListResp;
import com.zhs.basket.resp.PlayerHonorCandidateResp;
import com.zhs.basket.service.IBasketHonorRecordService;
import com.zhs.basket.support.BasketHonorCategoryCatalog;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.vo.PlayerRankListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BasketHonorRecordServiceImpl implements IBasketHonorRecordService {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String BIZ_HONOR = "HONOR";
    private static final Integer NOT_DELETED = 0;
    private static final String RECIPIENT_TEAM = "TEAM";
    private static final String RECIPIENT_PLAYER = "PLAYER";

    @Autowired private BasketHonorRecordMapper honorRecordMapper;
    @Autowired private BasketHonorCategoryMapper honorCategoryMapper;
    @Autowired private BasketHonorCategoryCatalog honorCategoryCatalog;
    @Autowired private BasketHonorLineupMapper honorLineupMapper;
    @Autowired private BasketSeasonMapper seasonMapper;
    @Autowired private BasketPlayerRankAdminMapper playerRankAdminMapper;
    @Autowired private BasketStatChangeRecordMapper statChangeRecordMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<HonorRecordListResp> selectList(HonorRecordQueryReq query, String recipientType) {
        HonorRecordQueryReq q = query == null ? new HonorRecordQueryReq() : query;
        if (StringUtils.isNotEmpty(recipientType)) {
            q.setRecipientType(recipientType);
        }
        List<HonorRecordListResp> list = honorRecordMapper.selectList(q);
        list.forEach(this::fillStatusName);
        return list;
    }

    @Override
    public HonorRecordDetailResp selectById(Long id, String recipientType) {
        HonorRecordListResp base = honorRecordMapper.selectDetailById(id);
        if (base == null) {
            throw new ServiceException("荣誉记录不存在");
        }
        assertRecipient(base.getRecipientType(), recipientType);
        HonorRecordDetailResp detail = new HonorRecordDetailResp();
        BeanUtils.copyBeanProp(detail, base);
        fillStatusName(detail);
        detail.setLineup(honorLineupMapper.selectByHonorRecordId(id));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(HonorRecordAddReq req, String recipientType, String sourceType) {
        if (req == null) {
            throw new ServiceException("请求不能为空");
        }
        BasketHonorCategory category = resolveCategory(req.getHonorCategoryId(), req.getHonorCode());
        assertRecipient(category.getRecipientType(), recipientType);
        validateRecipientFields(category, req.getTeamId(), req.getPlayerId());
        validateManualRemark(category, req.getRemark());
        if (req.getSeasonId() != null) {
            BasketSeason season = seasonMapper.selectById(req.getSeasonId());
            if (season == null) {
                throw new ServiceException("赛季不存在");
            }
            if (req.getTournamentId() == null) {
                req.setTournamentId(season.getSeriesId());
            }
        }
        ensureUnique(req.getSeasonId(), category.getId(), req.getTeamId(), req.getPlayerId(), null);

        Date now = new Date();
        BasketHonorRecord record = new BasketHonorRecord();
        record.setSeriesId(req.getTournamentId());
        record.setSeasonId(req.getSeasonId());
        record.setHonorCategoryId(category.getId());
        record.setTeamId(req.getTeamId());
        record.setPlayerId(req.getPlayerId());
        record.setMatchId(req.getMatchId());
        record.setHonorTitle(StringUtils.isEmpty(req.getHonorTitle()) ? category.getHonorName() : req.getHonorTitle());
        record.setHonorTitleEn(req.getHonorTitleEn());
        record.setHonorSubtitle(req.getHonorSubtitle());
        record.setHonorSubtitleEn(req.getHonorSubtitleEn());
        record.setHonorValue(req.getHonorValue());
        record.setAwardDate(req.getAwardDate() == null ? now : req.getAwardDate());
        record.setCoverFileId(req.getCoverFileId());
        record.setSourceType(StringUtils.isEmpty(sourceType) ? SOURCE_MANUAL : sourceType);
        record.setSourceId(null);
        record.setDisplayStatus(HonorDisplayStatusEnum.DRAFT.getCode());
        record.setDisplayOrder(req.getDisplayOrder() == null ? 0 : req.getDisplayOrder());
        record.setRemark(req.getRemark());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        honorRecordMapper.insert(record);
        writeAudit(record.getId(), "display_status", null, String.valueOf(record.getDisplayStatus()), "新增荣誉");
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, HonorRecordEditReq req, String recipientType) {
        BasketHonorRecord existing = requireRecord(id, recipientType);
        HonorDisplayStatusEnum status = HonorDisplayStatusEnum.fromCode(existing.getDisplayStatus());
        if (!status.canEdit()) {
            throw new ServiceException("已发布荣誉不可普通编辑，请先撤销或下线");
        }
        BasketHonorCategory category = resolveCategory(
                req.getHonorCategoryId() != null ? req.getHonorCategoryId() : existing.getHonorCategoryId(),
                req.getHonorCode());
        assertRecipient(category.getRecipientType(), recipientType);
        Long teamId = req.getTeamId() != null ? req.getTeamId() : existing.getTeamId();
        Long playerId = req.getPlayerId() != null ? req.getPlayerId() : existing.getPlayerId();
        validateRecipientFields(category, teamId, playerId);
        String remark = req.getRemark() != null ? req.getRemark() : existing.getRemark();
        validateManualRemark(category, remark);
        ensureUnique(existing.getSeasonId(), category.getId(), teamId, playerId, id);

        existing.setHonorCategoryId(category.getId());
        existing.setTeamId(teamId);
        existing.setPlayerId(playerId);
        if (req.getMatchId() != null) {
            existing.setMatchId(req.getMatchId());
        }
        if (req.getHonorTitle() != null) {
            existing.setHonorTitle(req.getHonorTitle());
        }
        if (req.getHonorTitleEn() != null) {
            existing.setHonorTitleEn(req.getHonorTitleEn());
        }
        if (req.getHonorSubtitle() != null) {
            existing.setHonorSubtitle(req.getHonorSubtitle());
        }
        if (req.getHonorSubtitleEn() != null) {
            existing.setHonorSubtitleEn(req.getHonorSubtitleEn());
        }
        if (req.getHonorValue() != null) {
            existing.setHonorValue(req.getHonorValue());
        }
        if (req.getAwardDate() != null) {
            existing.setAwardDate(req.getAwardDate());
        }
        if (req.getCoverFileId() != null) {
            existing.setCoverFileId(req.getCoverFileId());
        }
        if (req.getDisplayOrder() != null) {
            existing.setDisplayOrder(req.getDisplayOrder());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        if (honorRecordMapper.updateById(existing) != 1) {
            throw new ServiceException("更新失败");
        }
        writeAudit(id, "honor_record", null, "EDIT", "编辑荣誉");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id, String recipientType) {
        BasketHonorRecord existing = requireRecord(id, recipientType);
        HonorDisplayStatusEnum status = HonorDisplayStatusEnum.fromCode(existing.getDisplayStatus());
        if (!status.canPublish()) {
            throw new ServiceException("当前状态不可发布");
        }
        ensureUnique(existing.getSeasonId(), existing.getHonorCategoryId(), existing.getTeamId(),
                existing.getPlayerId(), id);
        if (honorRecordMapper.updateDisplayStatusIfMatch(id, existing.getDisplayStatus(),
                HonorDisplayStatusEnum.PUBLISHED.getCode()) != 1) {
            throw new ServiceException("发布失败");
        }
        writeAudit(id, "display_status", String.valueOf(existing.getDisplayStatus()),
                String.valueOf(HonorDisplayStatusEnum.PUBLISHED.getCode()), "发布荣誉");
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        BasketHonorRecord existing = honorRecordMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("荣誉记录不存在");
        }
        HonorDisplayStatusEnum status = HonorDisplayStatusEnum.fromCode(existing.getDisplayStatus());
        if (!status.canOffline()) {
            throw new ServiceException("当前状态不可下线");
        }
        if (honorRecordMapper.updateDisplayStatusIfMatch(id, existing.getDisplayStatus(),
                HonorDisplayStatusEnum.OFFLINE.getCode()) != 1) {
            throw new ServiceException("下线失败");
        }
        writeAudit(id, "display_status", String.valueOf(existing.getDisplayStatus()),
                String.valueOf(HonorDisplayStatusEnum.OFFLINE.getCode()), "下线荣誉/名人堂");
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id, HonorRecordRevokeReq req, String recipientType) {
        String reason = resolveRevokeReason(req);
        if (StringUtils.isEmpty(reason)) {
            throw new ServiceException("撤销原因不能为空");
        }
        BasketHonorRecord existing = requireRecord(id, recipientType);
        HonorDisplayStatusEnum status = HonorDisplayStatusEnum.fromCode(existing.getDisplayStatus());
        if (!status.canRevoke()) {
            throw new ServiceException("当前状态不可撤销");
        }
        if (honorRecordMapper.updateDisplayStatusIfMatch(id, existing.getDisplayStatus(),
                HonorDisplayStatusEnum.REVOKED.getCode()) != 1) {
            throw new ServiceException("撤销失败");
        }
        writeAudit(id, "display_status", String.valueOf(existing.getDisplayStatus()),
                String.valueOf(HonorDisplayStatusEnum.REVOKED.getCode()), reason);
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id, String recipientType) {
        BasketHonorRecord existing = requireRecord(id, recipientType);
        HonorDisplayStatusEnum status = HonorDisplayStatusEnum.fromCode(existing.getDisplayStatus());
        if (status == HonorDisplayStatusEnum.PUBLISHED) {
            throw new ServiceException("已发布荣誉不可直接删除，请先撤销或下线");
        }
        List<Integer> from = Arrays.asList(
                HonorDisplayStatusEnum.DRAFT.getCode(),
                HonorDisplayStatusEnum.OFFLINE.getCode(),
                HonorDisplayStatusEnum.REVOKED.getCode());
        if (honorRecordMapper.softDelete(id, from) != 1) {
            throw new ServiceException("删除失败");
        }
        honorLineupMapper.softDeleteByHonorRecordId(id);
        writeAudit(id, "is_delete", "0", "1", "删除荣誉");
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    public List<PlayerHonorCandidateResp> listPlayerCandidates(PlayerHonorCandidateQueryReq query) {
        if (query == null || query.getSeasonId() == null || StringUtils.isEmpty(query.getAwardType())) {
            throw new ServiceException("seasonId 与 awardType 不能为空");
        }
        String rankType = mapAwardTypeToRankType(query.getAwardType());
        RankingQuery rankingQuery = new RankingQuery();
        rankingQuery.setSeasonId(query.getSeasonId());
        rankingQuery.setStageId(query.getStageId());
        rankingQuery.setRankType(rankType);
        List<PlayerRankListVo> ranks = playerRankAdminMapper.selectLatestList(rankingQuery);
        String awardType = query.getAwardType().trim().toUpperCase(Locale.ROOT);
        return ranks.stream().map(r -> {
            PlayerHonorCandidateResp resp = new PlayerHonorCandidateResp();
            resp.setPlayerId(r.getPlayerId());
            resp.setPlayerNo(r.getPlayerNo());
            resp.setDisplayName(r.getDisplayName());
            resp.setAvatarUrl(r.getAvatarUrl());
            resp.setTeamId(r.getTeamId());
            resp.setTeamName(r.getTeamName());
            resp.setRankNo(r.getRankNo());
            resp.setStatValue(r.getStatValue());
            resp.setGames(r.getGames());
            resp.setRankType(r.getRankType());
            resp.setAwardType(awardType);
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HonorRecordListResp> listImportCandidates(Long tournamentId, Long seasonId) {
        List<HonorRecordListResp> list = honorRecordMapper.selectImportCandidates(tournamentId, seasonId);
        list.forEach(this::fillStatusName);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importFromHonor(Long honorId, String hallSourceType) {
        if (honorId == null) {
            throw new ServiceException("来源荣誉不能为空");
        }
        String sourceType = StringUtils.isEmpty(hallSourceType) ? "HALL_OF_FAME" : hallSourceType;
        HonorRecordListResp source = honorRecordMapper.selectDetailById(honorId);
        if (source == null) {
            throw new ServiceException("来源荣誉不存在");
        }
        if (!Objects.equals(HonorDisplayStatusEnum.PUBLISHED.getCode(), source.getDisplayStatus())) {
            throw new ServiceException("仅已发布荣誉可入选名人堂");
        }
        if ("HALL_OF_FAME".equalsIgnoreCase(source.getSourceType())) {
            throw new ServiceException("名人堂记录不能再次导入");
        }
        if (honorRecordMapper.countBySourceHonorId(sourceType, honorId) > 0) {
            throw new ServiceException("该荣誉已入选名人堂");
        }
        Date now = new Date();
        BasketHonorRecord record = new BasketHonorRecord();
        record.setSeriesId(source.getTournamentId());
        record.setSeasonId(source.getSeasonId());
        record.setHonorCategoryId(source.getHonorCategoryId());
        record.setTeamId(source.getTeamId());
        record.setPlayerId(source.getPlayerId());
        record.setMatchId(source.getMatchId());
        record.setHonorTitle(source.getHonorTitle());
        record.setHonorSubtitle(source.getHonorSubtitle());
        record.setHonorValue(source.getHonorValue());
        record.setAwardDate(source.getAwardDate() == null ? now : source.getAwardDate());
        record.setCoverFileId(source.getCoverFileId());
        record.setSourceType(sourceType);
        record.setSourceId(honorId);
        record.setDisplayStatus(HonorDisplayStatusEnum.DRAFT.getCode());
        record.setDisplayOrder(source.getDisplayOrder() == null ? 0 : source.getDisplayOrder());
        record.setRemark("来源荣誉ID " + honorId);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        honorRecordMapper.insert(record);

        List<HonorLineupResp> lineup = honorLineupMapper.selectByHonorRecordId(honorId);
        if (lineup != null && !lineup.isEmpty()) {
            List<BasketHonorLineup> copies = lineup.stream().map(item -> {
                BasketHonorLineup row = new BasketHonorLineup();
                row.setHonorRecordId(record.getId());
                row.setTeamId(item.getTeamId());
                row.setPlayerId(item.getPlayerId());
                row.setMemberId(item.getMemberId());
                row.setRoleType(item.getRoleType());
                row.setJerseyNo(item.getJerseyNo());
                row.setPositionCode(item.getPositionCode());
                row.setSortNo(item.getSortNo());
                row.setCreateTime(now);
                row.setUpdateTime(now);
                row.setIsDelete(NOT_DELETED);
                return row;
            }).collect(Collectors.toList());
            honorLineupMapper.batchInsert(copies);
        }
        writeAudit(record.getId(), "source_id", null, String.valueOf(honorId), "从荣誉导入名人堂");
        publicCacheCleaner.clearAfterCommit();
        return record.getId();
    }

    private String mapAwardTypeToRankType(String awardType) {
        String key = awardType.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "SCORING", "SCORING_CHAMPION" -> PlayerRankTypeEnum.AVG_POINTS.getCode();
            case "REBOUND", "REBOUND_CHAMPION" -> PlayerRankTypeEnum.AVG_REBOUNDS.getCode();
            case "ASSIST", "ASSIST_CHAMPION" -> PlayerRankTypeEnum.AVG_ASSISTS.getCode();
            case "STEAL", "STEAL_CHAMPION" -> PlayerRankTypeEnum.AVG_STEALS.getCode();
            case "BLOCK", "BLOCK_CHAMPION" -> PlayerRankTypeEnum.AVG_BLOCKS.getCode();
            default -> throw new ServiceException("不支持的奖项类型: " + awardType);
        };
    }

    private BasketHonorRecord requireRecord(Long id, String recipientType) {
        BasketHonorRecord existing = honorRecordMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("荣誉记录不存在");
        }
        if (StringUtils.isNotEmpty(recipientType)) {
            BasketHonorCategory category = honorCategoryMapper.selectById(existing.getHonorCategoryId());
            if (category == null) {
                throw new ServiceException("荣誉分类不存在");
            }
            assertRecipient(category.getRecipientType(), recipientType);
        }
        return existing;
    }

    private BasketHonorCategory resolveCategory(Long categoryId, String honorCode) {
        BasketHonorCategory category = null;
        if (categoryId != null) {
            category = honorCategoryMapper.selectById(categoryId);
        } else if (StringUtils.isNotEmpty(honorCode)) {
            return honorCategoryCatalog.requireByCode(honorCode);
        }
        if (category == null) {
            throw new ServiceException("荣誉分类不存在");
        }
        return category;
    }

    private void validateRecipientFields(BasketHonorCategory category, Long teamId, Long playerId) {
        if (RECIPIENT_TEAM.equals(category.getRecipientType()) && teamId == null) {
            throw new ServiceException("球队荣誉必须指定球队");
        }
        if (RECIPIENT_PLAYER.equals(category.getRecipientType()) && playerId == null) {
            throw new ServiceException("球员荣誉必须指定球员");
        }
    }

    private void validateManualRemark(BasketHonorCategory category, String remark) {
        if (HonorCodeEnum.PLAYER_HONOR.getCode().equalsIgnoreCase(category.getHonorCode())
                && StringUtils.isEmpty(remark)) {
            throw new ServiceException("人工球员荣誉必须填写说明");
        }
    }

    private void ensureUnique(Long seasonId, Long categoryId, Long teamId, Long playerId, Long excludeId) {
        if (seasonId == null || categoryId == null) {
            return;
        }
        int count = honorRecordMapper.countActiveUnique(seasonId, categoryId, teamId, playerId, excludeId);
        if (count > 0) {
            throw new ServiceException("同赛季同荣誉分类下已存在未撤销记录，不能重复授予");
        }
    }

    private void assertRecipient(String actual, String expected) {
        if (StringUtils.isEmpty(expected)) {
            return;
        }
        if (!Objects.equals(actual, expected)) {
            throw new ServiceException("荣誉对象类型不匹配");
        }
    }

    private void fillStatusName(HonorRecordListResp resp) {
        if (resp == null || resp.getDisplayStatus() == null) {
            return;
        }
        try {
            resp.setDisplayStatusName(HonorDisplayStatusEnum.fromCode(resp.getDisplayStatus()).getDescription());
        } catch (Exception ignored) {
            // ignore
        }
    }

    private String resolveRevokeReason(HonorRecordRevokeReq req) {
        if (req == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(req.getRevokeReason())) {
            return req.getRevokeReason();
        }
        return req.getReason();
    }

    private void writeAudit(Long bizId, String fieldName, String before, String after, String reason) {
        BasketStatChangeRecord record = new BasketStatChangeRecord();
        record.setBizType(BIZ_HONOR);
        record.setBizId(bizId);
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
