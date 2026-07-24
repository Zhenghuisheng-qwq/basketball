package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketAnnouncement;
import com.zhs.basket.enums.AnnouncementCategoryEnum;
import com.zhs.basket.enums.AnnouncementStatusEnum;
import com.zhs.basket.mapper.BasketAnnouncementMapper;
import com.zhs.basket.req.AnnouncementAddReq;
import com.zhs.basket.req.AnnouncementEditReq;
import com.zhs.basket.req.AnnouncementQueryReq;
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

@Service
public class BasketAnnouncementServiceImpl implements IBasketAnnouncementService {

    private static final Integer NOT_DELETED = 0;
    private static final String DEFAULT_SCOPE = "PUBLIC";

    @Autowired private BasketAnnouncementMapper announcementMapper;
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
        if (announcementMapper.publishIfMatch(id, existing.getAnnouncementStatus()) != 1) {
            throw new ServiceException("发布失败");
        }
        publicCacheCleaner.clearAfterCommit();
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
}
