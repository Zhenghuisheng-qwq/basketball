package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketAnnouncement;
import com.zhs.basket.domain.BasketAnnouncementRuleLink;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.AnnouncementCategoryEnum;
import com.zhs.basket.enums.AnnouncementStatusEnum;
import com.zhs.basket.mapper.BasketAnnouncementMapper;
import com.zhs.basket.mapper.BasketAnnouncementRuleLinkMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.req.AnnouncementAddReq;
import com.zhs.basket.req.AnnouncementEditReq;
import com.zhs.basket.req.AnnouncementQueryReq;
import com.zhs.basket.req.AnnouncementRuleSupersedeReq;
import com.zhs.basket.req.AnnouncementScheduleReq;
import com.zhs.basket.resp.AnnouncementDetailResp;
import com.zhs.basket.resp.AnnouncementListResp;
import com.zhs.basket.service.IBasketAnnouncementService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.support.ContentTextSanitizer;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketAnnouncementServiceImpl implements IBasketAnnouncementService {

    private static final Integer NOT_DELETED = 0;
    private static final String DEFAULT_SCOPE = "PUBLIC";

    @Autowired private BasketAnnouncementMapper announcementMapper;
    @Autowired private BasketAnnouncementRuleLinkMapper ruleLinkMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<AnnouncementListResp> selectList(AnnouncementQueryReq query) {
        List<AnnouncementListResp> list = announcementMapper.selectList(query == null ? new AnnouncementQueryReq() : query);
        list.forEach(this::fillNames);
        return list;
    }

    @Override
    public AnnouncementDetailResp selectById(Long id) {
        AnnouncementDetailResp detail = announcementMapper.selectDetailById(id);
        if (detail == null) {
            throw new ServiceException("公告不存在");
        }
        fillNames(detail);
        return detail;
    }

    @Override
    public AnnouncementDetailResp getCurrentRule(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季不能为空");
        }
        AnnouncementDetailResp detail = announcementMapper.selectCurrentRuleBySeason(seasonId);
        if (detail == null) {
            throw new ServiceException("当前赛季暂无有效规则公告");
        }
        fillNames(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(AnnouncementAddReq req) {
        if (req == null || StringUtils.isEmpty(req.getTitle())) {
            throw new ServiceException("公告标题不能为空");
        }
        AnnouncementCategoryEnum category = AnnouncementCategoryEnum.fromCode(req.getCategoryCode());
        String content = ContentTextSanitizer.requirePlainText(req.getContentText());

        Date now = new Date();
        BasketAnnouncement announcement = new BasketAnnouncement();
        announcement.setAnnouncementNo(generateAnnouncementNo());
        announcement.setCategoryCode(category.getCode());
        applyRuleMetadata(announcement, req.getTournamentId(), req.getSeasonId(), req.getRuleVersion());
        announcement.setTitle(req.getTitle().trim());
        announcement.setSummary(req.getSummary());
        announcement.setCoverFileId(req.getCoverFileId());
        announcement.setContentText(content);
        try {
            announcement.setAuthorId(SecurityUtils.getUserId());
            announcement.setAuthorName(SecurityUtils.getUsername());
        } catch (Exception ex) {
            announcement.setAuthorId(0L);
            announcement.setAuthorName("system");
        }
        announcement.setAnnouncementStatus(AnnouncementStatusEnum.DRAFT.getCode());
        announcement.setPublicScope(StringUtils.isEmpty(req.getPublicScope()) ? DEFAULT_SCOPE : req.getPublicScope());
        announcement.setIsTop(0);
        announcement.setIsImportant(req.getIsImportant() == null ? 0 : req.getIsImportant());
        announcement.setScheduledPublishTime(req.getScheduledPublishTime());
        announcement.setViewCount(0L);
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        announcement.setIsDelete(NOT_DELETED);
        announcementMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, AnnouncementEditReq req) {
        BasketAnnouncement existing = require(id);
        AnnouncementStatusEnum status = AnnouncementStatusEnum.fromCode(existing.getAnnouncementStatus());
        if (!status.canEdit()) {
            throw new ServiceException("当前状态不可编辑");
        }
        if (req.getCategoryCode() != null) {
            existing.setCategoryCode(AnnouncementCategoryEnum.fromCode(req.getCategoryCode()).getCode());
        }
        if (req.getTournamentId() != null) {
            existing.setTournamentId(req.getTournamentId());
        }
        if (req.getSeasonId() != null) {
            existing.setSeasonId(req.getSeasonId());
        }
        if (req.getRuleVersion() != null) {
            existing.setRuleVersion(req.getRuleVersion());
        }
        if (req.getScheduledPublishTime() != null) {
            existing.setScheduledPublishTime(req.getScheduledPublishTime());
        }
        if (req.getTitle() != null) {
            existing.setTitle(req.getTitle().trim());
        }
        if (req.getSummary() != null) {
            existing.setSummary(req.getSummary());
        }
        if (req.getCoverFileId() != null) {
            existing.setCoverFileId(req.getCoverFileId());
        }
        if (req.getContentText() != null) {
            existing.setContentText(ContentTextSanitizer.requirePlainText(req.getContentText()));
        }
        if (req.getPublicScope() != null) {
            existing.setPublicScope(req.getPublicScope());
        }
        if (req.getIsImportant() != null) {
            existing.setIsImportant(req.getIsImportant());
        }
        if (announcementMapper.updateById(existing) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketAnnouncement existing = require(id);
        AnnouncementStatusEnum status = AnnouncementStatusEnum.fromCode(existing.getAnnouncementStatus());
        if (!status.canPublish()) {
            throw new ServiceException("当前状态不可发布");
        }
        if (existing.getScheduledPublishTime() != null && existing.getScheduledPublishTime().after(new Date())) {
            throw new ServiceException("尚未到达定时发布时间，请稍后再发布或取消定时");
        }
        if (announcementMapper.publishIfMatch(id, existing.getAnnouncementStatus()) != 1) {
            throw new ServiceException("发布失败");
        }
        activateRuleSupersedeIfNeeded(id);
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void supersedeRule(Long newId, AnnouncementRuleSupersedeReq req) {
        if (req == null || req.getOldAnnouncementId() == null) {
            throw new ServiceException("被替代的旧公告不能为空");
        }
        if (Objects.equals(newId, req.getOldAnnouncementId())) {
            throw new ServiceException("新旧公告不能相同");
        }
        BasketAnnouncement newAnnouncement = require(newId);
        BasketAnnouncement oldAnnouncement = require(req.getOldAnnouncementId());
        AnnouncementCategoryEnum newCategory = AnnouncementCategoryEnum.fromCode(newAnnouncement.getCategoryCode());
        AnnouncementCategoryEnum oldCategory = AnnouncementCategoryEnum.fromCode(oldAnnouncement.getCategoryCode());
        if (newCategory != AnnouncementCategoryEnum.RULE || oldCategory != AnnouncementCategoryEnum.RULE) {
            throw new ServiceException("仅规则类公告可执行替代");
        }
        AnnouncementStatusEnum newStatus = AnnouncementStatusEnum.fromCode(newAnnouncement.getAnnouncementStatus());
        if (!newStatus.canEdit()) {
            throw new ServiceException("新公告当前状态不可绑定替代关系");
        }
        Long seasonId = req.getSeasonId() != null ? req.getSeasonId() : newAnnouncement.getSeasonId();
        if (seasonId == null) {
            seasonId = oldAnnouncement.getSeasonId();
        }
        if (seasonId == null) {
            throw new ServiceException("请指定关联赛季");
        }
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        Long tournamentId = req.getTournamentId() != null ? req.getTournamentId() : season.getTournamentId();
        String ruleVersion = StringUtils.isNotEmpty(req.getRuleVersion())
                ? req.getRuleVersion().trim()
                : newAnnouncement.getRuleVersion();

        newAnnouncement.setSeasonId(seasonId);
        newAnnouncement.setTournamentId(tournamentId);
        newAnnouncement.setRuleVersion(ruleVersion);
        if (announcementMapper.updateById(newAnnouncement) != 1) {
            throw new ServiceException("更新新公告失败");
        }
        if (announcementMapper.updateSupersededBy(oldAnnouncement.getId(), newId) != 1) {
            throw new ServiceException("标记旧公告失败");
        }
        ruleLinkMapper.softDeleteByNewAnnouncement(newId);
        Date now = new Date();
        BasketAnnouncementRuleLink link = new BasketAnnouncementRuleLink();
        link.setTournamentId(tournamentId);
        link.setSeasonId(seasonId);
        link.setOldAnnouncementId(oldAnnouncement.getId());
        link.setNewAnnouncementId(newId);
        link.setRuleVersion(ruleVersion);
        link.setEffectiveTime(now);
        link.setCurrentFlag(0);
        link.setCreateTime(now);
        link.setUpdateTime(now);
        link.setIsDelete(NOT_DELETED);
        ruleLinkMapper.insert(link);
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedulePublish(Long id, AnnouncementScheduleReq req) {
        BasketAnnouncement existing = require(id);
        AnnouncementStatusEnum status = AnnouncementStatusEnum.fromCode(existing.getAnnouncementStatus());
        if (!status.canEdit()) {
            throw new ServiceException("当前状态不可设置定时发布");
        }
        if (req == null || req.getScheduledPublishTime() == null) {
            throw new ServiceException("定时发布时间不能为空");
        }
        if (req.getScheduledPublishTime().before(new Date())) {
            throw new ServiceException("定时发布时间必须晚于当前时间");
        }
        existing.setScheduledPublishTime(req.getScheduledPublishTime());
        if (announcementMapper.updateById(existing) != 1) {
            throw new ServiceException("设置定时发布失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        BasketAnnouncement existing = require(id);
        AnnouncementStatusEnum status = AnnouncementStatusEnum.fromCode(existing.getAnnouncementStatus());
        if (!status.canOffline()) {
            throw new ServiceException("当前状态不可下线");
        }
        if (announcementMapper.offlineIfMatch(id, existing.getAnnouncementStatus()) != 1) {
            throw new ServiceException("下线失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void top(Long id) {
        BasketAnnouncement existing = require(id);
        AnnouncementStatusEnum status = AnnouncementStatusEnum.fromCode(existing.getAnnouncementStatus());
        if (!status.canTop()) {
            throw new ServiceException("仅已发布公告可置顶");
        }
        if (announcementMapper.updateTop(id, 1) != 1) {
            throw new ServiceException("置顶失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void untop(Long id) {
        require(id);
        if (announcementMapper.updateTop(id, 0) != 1) {
            throw new ServiceException("取消置顶失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        BasketAnnouncement existing = require(id);
        AnnouncementStatusEnum status = AnnouncementStatusEnum.fromCode(existing.getAnnouncementStatus());
        if (!status.canDelete()) {
            throw new ServiceException("已发布公告不可删除，请先下线");
        }
        if (announcementMapper.softDelete(id, Arrays.asList(
                AnnouncementStatusEnum.DRAFT.getCode(), AnnouncementStatusEnum.OFFLINE.getCode())) != 1) {
            throw new ServiceException("删除失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    private String generateAnnouncementNo() {
        String prefix = "ANN" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int seq = announcementMapper.countByNoPrefix(prefix) + 1;
        return prefix + String.format("%04d", seq);
    }

    private BasketAnnouncement require(Long id) {
        BasketAnnouncement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("公告不存在");
        }
        return existing;
    }

    private void fillNames(AnnouncementListResp resp) {
        if (resp == null) {
            return;
        }
        try {
            resp.setCategoryName(AnnouncementCategoryEnum.fromCode(resp.getCategoryCode()).getDescription());
        } catch (Exception ignored) {
            // ignore
        }
        try {
            resp.setAnnouncementStatusName(
                    AnnouncementStatusEnum.fromCode(resp.getAnnouncementStatus()).getDescription());
        } catch (Exception ignored) {
            // ignore
        }
    }

    private void applyRuleMetadata(BasketAnnouncement announcement, Long tournamentId, Long seasonId, String ruleVersion) {
        if (!AnnouncementCategoryEnum.RULE.getCode().equalsIgnoreCase(announcement.getCategoryCode())) {
            return;
        }
        if (seasonId != null) {
            BasketTournamentSeason season = seasonMapper.selectById(seasonId);
            if (season == null) {
                throw new ServiceException("赛季不存在");
            }
            announcement.setSeasonId(seasonId);
            announcement.setTournamentId(tournamentId != null ? tournamentId : season.getTournamentId());
        } else if (tournamentId != null) {
            announcement.setTournamentId(tournamentId);
        }
        if (StringUtils.isNotEmpty(ruleVersion)) {
            announcement.setRuleVersion(ruleVersion.trim());
        }
    }

    private void activateRuleSupersedeIfNeeded(Long newAnnouncementId) {
        BasketAnnouncementRuleLink pending = ruleLinkMapper.selectPendingByNewAnnouncement(newAnnouncementId);
        if (pending == null) {
            return;
        }
        Date now = new Date();
        ruleLinkMapper.clearCurrentBySeason(pending.getSeasonId(), now);
        ruleLinkMapper.activateLink(pending.getId(), now);
        BasketAnnouncement oldAnnouncement = announcementMapper.selectById(pending.getOldAnnouncementId());
        if (oldAnnouncement != null
                && AnnouncementStatusEnum.PUBLISHED.getCode() == oldAnnouncement.getAnnouncementStatus()) {
            announcementMapper.offlineIfMatch(oldAnnouncement.getId(), oldAnnouncement.getAnnouncementStatus());
        }
    }
}
