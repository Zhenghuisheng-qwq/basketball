package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.dao.BasketAnnouncementDao;
import com.zhs.dao.BasketAnnouncementRuleLinkDao;
import com.zhs.enums.PublicAnnouncementCategoryEnum;
import com.zhs.enums.PublicAnnouncementSortEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketAnnouncement;
import com.zhs.model.BasketAnnouncementRuleLink;
import com.zhs.response.publicapi.PublicAnnouncementAdjacentResp;
import com.zhs.response.publicapi.PublicAnnouncementCategoryResp;
import com.zhs.response.publicapi.PublicAnnouncementDetailResp;
import com.zhs.response.publicapi.PublicAnnouncementItemResp;
import com.zhs.service.IBasketPublicAnnouncementService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicAnnouncementServiceImpl implements IBasketPublicAnnouncementService {

    @Resource
    private BasketAnnouncementDao announcementDao;
    @Resource
    private BasketAnnouncementRuleLinkDao ruleLinkDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public List<PublicAnnouncementCategoryResp> listCategories() {
        return Arrays.stream(PublicAnnouncementCategoryEnum.values())
                .map(v -> new PublicAnnouncementCategoryResp(v.getCode(), v.getDescription()))
                .collect(Collectors.toList());
    }

    @Override
    public IPage<PublicAnnouncementItemResp> pageAnnouncements(Integer currentPage, Integer pageSize,
                                                               String categoryCode, String keyword,
                                                               String sortField, String sortOrder) {
        if (StringUtils.hasText(categoryCode) && PublicAnnouncementCategoryEnum.fromCode(categoryCode) == null) {
            throw new ServiceException("未知公告分类: " + categoryCode);
        }
        Page<BasketAnnouncement> page = publicSupport.newPage(currentPage, pageSize);
        LambdaQueryWrapper<BasketAnnouncement> wrapper = publishedWrapper()
                .select(BasketAnnouncement::getId, BasketAnnouncement::getAnnouncementNo,
                        BasketAnnouncement::getCategoryCode,                         BasketAnnouncement::getTitle, BasketAnnouncement::getTitleEn,
                        BasketAnnouncement::getSummary, BasketAnnouncement::getSummaryEn, BasketAnnouncement::getCoverFileId,
                        BasketAnnouncement::getAuthorName, BasketAnnouncement::getIsTop,
                        BasketAnnouncement::getIsImportant, BasketAnnouncement::getPublishTime,
                        BasketAnnouncement::getViewCount)
                .eq(StringUtils.hasText(categoryCode), BasketAnnouncement::getCategoryCode,
                        categoryCode == null ? null : categoryCode.trim().toUpperCase());
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BasketAnnouncement::getTitle, kw)
                    .or().like(BasketAnnouncement::getSummary, kw));
        }
        applySort(wrapper, sortField, sortOrder);
        IPage<BasketAnnouncement> entityPage = announcementDao.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(entityPage.getRecords())) {
            return publicSupport.emptyPage(currentPage, pageSize);
        }
        Page<PublicAnnouncementItemResp> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toItemList(entityPage.getRecords()));
        return result;
    }

    @Override
    public PublicAnnouncementDetailResp getDetail(Long announcementId) {
        BasketAnnouncement announcement = requirePublished(announcementId);
        announcementDao.update(null, new LambdaUpdateWrapper<BasketAnnouncement>()
                .eq(BasketAnnouncement::getId, announcementId)
                .setSql("view_count = ifnull(view_count, 0) + 1"));
        BasketAnnouncement refreshed = announcementDao.selectById(announcementId);
        if (refreshed != null) {
            announcement.setViewCount(refreshed.getViewCount());
        }
        PublicAnnouncementDetailResp detail = new PublicAnnouncementDetailResp();
        fillItem(detail, announcement, loadCoverMap(Collections.singletonList(announcement)));
        detail.setContentText(toPlainText(announcement.getContentText()));
        detail.setContentTextEn(announcement.getContentTextEn());
        return detail;
    }

    @Override
    public PublicAnnouncementAdjacentResp getPreviousNext(Long announcementId) {
        BasketAnnouncement current = requirePublished(announcementId);
        PublicAnnouncementAdjacentResp resp = new PublicAnnouncementAdjacentResp();
        BasketAnnouncement previous = announcementDao.selectOne(publishedWrapper()
                .select(BasketAnnouncement::getId, BasketAnnouncement::getAnnouncementNo,
                        BasketAnnouncement::getCategoryCode,                         BasketAnnouncement::getTitle, BasketAnnouncement::getTitleEn,
                        BasketAnnouncement::getSummary, BasketAnnouncement::getSummaryEn, BasketAnnouncement::getCoverFileId,
                        BasketAnnouncement::getAuthorName, BasketAnnouncement::getIsTop,
                        BasketAnnouncement::getIsImportant, BasketAnnouncement::getPublishTime,
                        BasketAnnouncement::getViewCount)
                .and(w -> w.lt(BasketAnnouncement::getPublishTime, current.getPublishTime())
                        .or(q -> q.eq(BasketAnnouncement::getPublishTime, current.getPublishTime())
                                .lt(BasketAnnouncement::getId, current.getId())))
                .orderByDesc(BasketAnnouncement::getPublishTime)
                .orderByDesc(BasketAnnouncement::getId)
                .last("limit 1"));
        BasketAnnouncement next = announcementDao.selectOne(publishedWrapper()
                .select(BasketAnnouncement::getId, BasketAnnouncement::getAnnouncementNo,
                        BasketAnnouncement::getCategoryCode,                         BasketAnnouncement::getTitle, BasketAnnouncement::getTitleEn,
                        BasketAnnouncement::getSummary, BasketAnnouncement::getSummaryEn, BasketAnnouncement::getCoverFileId,
                        BasketAnnouncement::getAuthorName, BasketAnnouncement::getIsTop,
                        BasketAnnouncement::getIsImportant, BasketAnnouncement::getPublishTime,
                        BasketAnnouncement::getViewCount)
                .and(w -> w.gt(BasketAnnouncement::getPublishTime, current.getPublishTime())
                        .or(q -> q.eq(BasketAnnouncement::getPublishTime, current.getPublishTime())
                                .gt(BasketAnnouncement::getId, current.getId())))
                .orderByAsc(BasketAnnouncement::getPublishTime)
                .orderByAsc(BasketAnnouncement::getId)
                .last("limit 1"));
        List<BasketAnnouncement> adjacent = new ArrayList<>(2);
        if (previous != null) {
            adjacent.add(previous);
        }
        if (next != null) {
            adjacent.add(next);
        }
        Map<Long, String> coverMap = loadCoverMap(adjacent);
        if (previous != null) {
            PublicAnnouncementItemResp item = new PublicAnnouncementItemResp();
            fillItem(item, previous, coverMap);
            resp.setPrevious(item);
        }
        if (next != null) {
            PublicAnnouncementItemResp item = new PublicAnnouncementItemResp();
            fillItem(item, next, coverMap);
            resp.setNext(item);
        }
        return resp;
    }

    @Override
    public List<PublicAnnouncementItemResp> listLatest(int limit) {
        int size = Math.clamp(limit, 1, BasketPublicConstants.MAX_SIZE);
        List<BasketAnnouncement> list = announcementDao.selectList(publishedWrapper()
                .select(BasketAnnouncement::getId, BasketAnnouncement::getAnnouncementNo,
                        BasketAnnouncement::getCategoryCode,                         BasketAnnouncement::getTitle, BasketAnnouncement::getTitleEn,
                        BasketAnnouncement::getSummary, BasketAnnouncement::getSummaryEn, BasketAnnouncement::getCoverFileId,
                        BasketAnnouncement::getAuthorName, BasketAnnouncement::getIsTop,
                        BasketAnnouncement::getIsImportant, BasketAnnouncement::getPublishTime,
                        BasketAnnouncement::getViewCount)
                .orderByDesc(BasketAnnouncement::getIsTop)
                .orderByDesc(BasketAnnouncement::getIsImportant)
                .orderByDesc(BasketAnnouncement::getPublishTime)
                .orderByDesc(BasketAnnouncement::getId)
                .last("limit " + size));
        return toItemList(list);
    }

    @Override
    public PublicAnnouncementDetailResp getCurrentRule(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季主键不能为空");
        }
        BasketAnnouncementRuleLink link = ruleLinkDao.selectOne(new LambdaQueryWrapper<BasketAnnouncementRuleLink>()
                .eq(BasketAnnouncementRuleLink::getSeasonId, seasonId)
                .eq(BasketAnnouncementRuleLink::getCurrentFlag, 1)
                .eq(BasketAnnouncementRuleLink::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .last("limit 1"));
        if (link == null || link.getNewAnnouncementId() == null) {
            throw new ServiceException("当前赛季暂无有效规则公告");
        }
        return getDetail(link.getNewAnnouncementId());
    }

    private LambdaQueryWrapper<BasketAnnouncement> publishedWrapper() {
        Date now = new Date();
        return new LambdaQueryWrapper<BasketAnnouncement>()
                .eq(BasketAnnouncement::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketAnnouncement::getAnnouncementStatus, BasketPublicConstants.PUBLISHED)
                .le(BasketAnnouncement::getPublishTime, now)
                .and(w -> w.isNull(BasketAnnouncement::getOfflineTime)
                        .or().gt(BasketAnnouncement::getOfflineTime, now));
    }

    private BasketAnnouncement requirePublished(Long announcementId) {
        if (announcementId == null) {
            throw new ServiceException("公告主键不能为空");
        }
        BasketAnnouncement announcement = announcementDao.selectById(announcementId);
        if (announcement == null
                || !Objects.equals(announcement.getIsDelete(), BasketPublicConstants.NOT_DELETED)
                || !Objects.equals(announcement.getAnnouncementStatus(), BasketPublicConstants.PUBLISHED)) {
            throw new ServiceException("公告不存在或未发布");
        }
        Date now = new Date();
        if (announcement.getPublishTime() != null && announcement.getPublishTime().after(now)) {
            throw new ServiceException("公告不存在或未发布");
        }
        if (announcement.getOfflineTime() != null && !announcement.getOfflineTime().after(now)) {
            throw new ServiceException("公告不存在或未发布");
        }
        return announcement;
    }

    private void applySort(LambdaQueryWrapper<BasketAnnouncement> wrapper, String sortField, String sortOrder) {
        PublicAnnouncementSortEnum sortEnum = PublicAnnouncementSortEnum.fromCode(sortField);
        boolean asc = publicSupport.isAsc(sortOrder);
        // 置顶/重要始终优先
        wrapper.orderByDesc(BasketAnnouncement::getIsTop)
                .orderByDesc(BasketAnnouncement::getIsImportant);
        if (sortEnum == PublicAnnouncementSortEnum.VIEW_COUNT) {
            if (asc) {
                wrapper.orderByAsc(BasketAnnouncement::getViewCount);
            } else {
                wrapper.orderByDesc(BasketAnnouncement::getViewCount);
            }
            wrapper.orderByDesc(BasketAnnouncement::getPublishTime).orderByDesc(BasketAnnouncement::getId);
            return;
        }
        if (asc) {
            wrapper.orderByAsc(BasketAnnouncement::getPublishTime);
        } else {
            wrapper.orderByDesc(BasketAnnouncement::getPublishTime);
        }
        wrapper.orderByDesc(BasketAnnouncement::getId);
    }

    private List<PublicAnnouncementItemResp> toItemList(List<BasketAnnouncement> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, String> coverMap = loadCoverMap(list);
        List<PublicAnnouncementItemResp> result = new ArrayList<>(list.size());
        for (BasketAnnouncement item : list) {
            PublicAnnouncementItemResp vo = new PublicAnnouncementItemResp();
            fillItem(vo, item, coverMap);
            result.add(vo);
        }
        return result;
    }

    private void fillItem(PublicAnnouncementItemResp vo, BasketAnnouncement item, Map<Long, String> coverMap) {
        vo.setAnnouncementId(item.getId());
        vo.setAnnouncementNo(item.getAnnouncementNo());
        vo.setCategoryCode(item.getCategoryCode());
        vo.setCategoryName(PublicAnnouncementCategoryEnum.resolveName(item.getCategoryCode()));
        vo.setTitle(item.getTitle());
        vo.setTitleEn(item.getTitleEn());
        vo.setSummary(item.getSummary());
        vo.setSummaryEn(item.getSummaryEn());
        vo.setCoverUrl(coverMap.get(item.getCoverFileId()));
        vo.setAuthorName(item.getAuthorName());
        vo.setIsTop(item.getIsTop());
        vo.setIsImportant(item.getIsImportant());
        vo.setPublishTime(item.getPublishTime());
        vo.setViewCount(item.getViewCount());
    }

    private Map<Long, String> loadCoverMap(List<BasketAnnouncement> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        Set<Long> fileIds = list.stream()
                .map(BasketAnnouncement::getCoverFileId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        return publicSupport.loadAccessUrlMap(fileIds);
    }

    private String toPlainText(Object content) {
        if (content == null) {
            return null;
        }
        if (content instanceof String) {
            return (String) content;
        }
        return String.valueOf(content);
    }
}
