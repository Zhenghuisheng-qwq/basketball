package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketHomeBanner;
import com.zhs.basket.enums.BannerStatusEnum;
import com.zhs.basket.enums.BannerTargetTypeEnum;
import com.zhs.basket.mapper.BasketHomeBannerMapper;
import com.zhs.basket.req.HomeBannerAddReq;
import com.zhs.basket.req.HomeBannerEditReq;
import com.zhs.basket.req.HomeBannerQueryReq;
import com.zhs.basket.resp.HomeBannerListResp;
import com.zhs.basket.service.IBasketHomeBannerService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.support.ExternalUrlValidator;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketHomeBannerServiceImpl implements IBasketHomeBannerService {

    private static final Integer NOT_DELETED = 0;
    private static final String DEFAULT_POSITION = "HOME_TOP";
    private static final String GROUP_PUBLISH_MESSAGE = "首页轮播已改为整组发布，请使用「发布首页轮播」";

    @Autowired private BasketHomeBannerMapper homeBannerMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<HomeBannerListResp> selectList(HomeBannerQueryReq query) {
        List<HomeBannerListResp> list = homeBannerMapper.selectList(query == null ? new HomeBannerQueryReq() : query);
        list.forEach(this::fillStatusName);
        return list;
    }

    @Override
    public HomeBannerListResp selectById(Long id) {
        HomeBannerListResp detail = homeBannerMapper.selectDetailById(id);
        if (detail == null) {
            throw new ServiceException("横幅不存在");
        }
        fillStatusName(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(HomeBannerAddReq req) {
        if (req == null || StringUtils.isEmpty(req.getBannerName())) {
            throw new ServiceException("横幅名称不能为空");
        }
        if (req.getImageFileId() == null) {
            throw new ServiceException("横幅图片不能为空");
        }
        String position = normalizePosition(req.getPositionCode());
        BannerTargetTypeEnum targetType = BannerTargetTypeEnum.fromCode(req.getTargetType());
        validateTarget(targetType, req.getTargetValue());
        int enabled = enabledFlag(req.getDraftEnabled());

        Date now = new Date();
        BasketHomeBanner banner = new BasketHomeBanner();
        banner.setBannerName(req.getBannerName().trim());
        banner.setTitle(req.getTitle());
        banner.setTitleEn(blankToNull(req.getTitleEn()));
        banner.setSubtitle(req.getSubtitle());
        banner.setSubtitleEn(blankToNull(req.getSubtitleEn()));
        banner.setImageFileId(req.getImageFileId());
        banner.setMobileImageFileId(req.getMobileImageFileId());
        banner.setAltText(normalizeAltText(req.getAltText()));
        banner.setPositionCode(position);
        banner.setTargetType(targetType.getCode());
        banner.setTargetValue(req.getTargetValue());
        banner.setStartTime(req.getStartTime());
        banner.setEndTime(req.getEndTime());
        banner.setBannerStatus(BannerStatusEnum.DRAFT.getCode());
        banner.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
        banner.setHasDraft(1);
        banner.setDraftEnabled(enabled);
        banner.setDraftRemove(0);
        copyLiveToDraft(banner);
        banner.setCreateTime(now);
        banner.setUpdateTime(now);
        banner.setIsDelete(NOT_DELETED);
        homeBannerMapper.insert(banner);
        return banner.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, HomeBannerEditReq req) {
        BasketHomeBanner existing = require(id);
        if (req == null) {
            throw new ServiceException("横幅内容不能为空");
        }
        BannerStatusEnum status = BannerStatusEnum.fromCode(existing.getBannerStatus());
        if (status == BannerStatusEnum.PUBLISHED) {
            savePublishedDraft(existing, req);
            return;
        }
        saveUnpublished(existing, req);
    }

    @Override
    public void publish(Long id) {
        throw new ServiceException(GROUP_PUBLISH_MESSAGE);
    }

    @Override
    public void offline(Long id) {
        throw new ServiceException(GROUP_PUBLISH_MESSAGE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        BasketHomeBanner existing = require(id);
        BannerStatusEnum status = BannerStatusEnum.fromCode(existing.getBannerStatus());
        if (status == BannerStatusEnum.PUBLISHED) {
            if (!Objects.equals(existing.getHasDraft(), 1)) {
                copyLiveToDraft(existing);
            }
            existing.setHasDraft(1);
            existing.setDraftEnabled(0);
            existing.setDraftRemove(1);
            if (StringUtils.isEmpty(existing.getBannerName())) {
                existing.setBannerName("首页横幅");
            }
            if (homeBannerMapper.updateDraft(existing) != 1) {
                throw new ServiceException("删除失败");
            }
            return;
        }
        if (homeBannerMapper.softDelete(id, Arrays.asList(
                BannerStatusEnum.DRAFT.getCode(), BannerStatusEnum.OFFLINE.getCode())) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishHomeTop() {
        List<BasketHomeBanner> rows = homeBannerMapper.selectHomeTopForUpdate();
        List<BasketHomeBanner> pending = rows.stream().filter(this::isPending).toList();
        if (pending.isEmpty()) {
            return -1;
        }
        for (BasketHomeBanner row : pending) {
            if (isRemove(row) || !willEnable(row)) {
                continue;
            }
            if (effectiveImageId(row) == null) {
                throw new ServiceException("「" + displayName(row) + "」缺少电脑端图片，首页轮播未发布");
            }
        }
        for (BasketHomeBanner row : pending) {
            if (isRemove(row)) {
                if (homeBannerMapper.markDeleted(row.getId()) != 1) {
                    throw new ServiceException("发布失败");
                }
                continue;
            }
            BasketHomeBanner next = buildPublishedRow(row);
            if (homeBannerMapper.applyPublish(next) != 1) {
                throw new ServiceException("发布失败");
            }
        }
        publicCacheCleaner.clearAfterCommit();
        return countLive(rows, pending);
    }

    private void savePublishedDraft(BasketHomeBanner existing, HomeBannerEditReq req) {
        if (!Objects.equals(existing.getHasDraft(), 1) || Objects.equals(existing.getDraftRemove(), 1)) {
            copyLiveToDraft(existing);
        }
        overlayDraft(existing, req);
        existing.setHasDraft(1);
        existing.setDraftRemove(0);
        if (req.getBannerName() != null && !req.getBannerName().isBlank()) {
            existing.setBannerName(req.getBannerName().trim());
        }
        validateDraft(existing);
        if (homeBannerMapper.updateDraft(existing) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    private void saveUnpublished(BasketHomeBanner existing, HomeBannerEditReq req) {
        if (req.getBannerName() != null && !req.getBannerName().isBlank()) {
            existing.setBannerName(req.getBannerName().trim());
        }
        existing.setTitle(req.getTitle());
        existing.setTitleEn(blankToNull(req.getTitleEn()));
        existing.setSubtitle(req.getSubtitle());
        existing.setSubtitleEn(blankToNull(req.getSubtitleEn()));
        if (req.getImageFileId() != null) {
            existing.setImageFileId(req.getImageFileId());
        }
        existing.setMobileImageFileId(req.getMobileImageFileId());
        existing.setAltText(normalizeAltText(req.getAltText()));
        existing.setPositionCode(normalizePosition(
                req.getPositionCode() != null ? req.getPositionCode() : existing.getPositionCode()));
        BannerTargetTypeEnum targetType = BannerTargetTypeEnum.fromCode(
                req.getTargetType() != null ? req.getTargetType() : existing.getTargetType());
        String targetValue = req.getTargetValue() != null ? req.getTargetValue() : existing.getTargetValue();
        validateTarget(targetType, targetValue);
        existing.setTargetType(targetType.getCode());
        existing.setTargetValue(targetValue);
        existing.setStartTime(req.getStartTime());
        existing.setEndTime(req.getEndTime());
        if (req.getSortNo() != null) {
            existing.setSortNo(req.getSortNo());
        }
        existing.setHasDraft(1);
        existing.setDraftEnabled(enabledFlag(req.getDraftEnabled()));
        existing.setDraftRemove(0);
        copyLiveToDraft(existing);
        if (Objects.equals(existing.getDraftEnabled(), 1) && existing.getImageFileId() == null) {
            throw new ServiceException("横幅图片不能为空");
        }
        if (homeBannerMapper.updateById(existing) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    private void overlayDraft(BasketHomeBanner existing, HomeBannerEditReq req) {
        BannerTargetTypeEnum targetType = BannerTargetTypeEnum.fromCode(
                req.getTargetType() != null ? req.getTargetType() : existing.getDraftTargetType());
        String targetValue = req.getTargetValue() != null ? req.getTargetValue() : existing.getDraftTargetValue();
        validateTarget(targetType, targetValue);
        existing.setDraftTitle(req.getTitle());
        existing.setDraftTitleEn(blankToNull(req.getTitleEn()));
        existing.setDraftSubtitle(req.getSubtitle());
        existing.setDraftSubtitleEn(blankToNull(req.getSubtitleEn()));
        if (req.getImageFileId() != null) {
            existing.setDraftImageFileId(req.getImageFileId());
        }
        existing.setDraftMobileImageFileId(req.getMobileImageFileId());
        existing.setDraftAltText(normalizeAltText(req.getAltText()));
        existing.setDraftTargetType(targetType.getCode());
        existing.setDraftTargetValue(targetValue);
        existing.setDraftSortNo(req.getSortNo() == null ? existing.getDraftSortNo() : req.getSortNo());
        existing.setDraftStartTime(req.getStartTime());
        existing.setDraftEndTime(req.getEndTime());
        existing.setDraftEnabled(enabledFlag(req.getDraftEnabled()));
    }

    private void validateDraft(BasketHomeBanner existing) {
        if (Objects.equals(existing.getDraftEnabled(), 1) && existing.getDraftImageFileId() == null) {
            throw new ServiceException("横幅图片不能为空");
        }
        BannerTargetTypeEnum targetType = BannerTargetTypeEnum.fromCode(existing.getDraftTargetType());
        validateTarget(targetType, existing.getDraftTargetValue());
    }

    private void copyLiveToDraft(BasketHomeBanner banner) {
        banner.setDraftImageFileId(banner.getImageFileId());
        banner.setDraftMobileImageFileId(banner.getMobileImageFileId());
        banner.setDraftAltText(banner.getAltText());
        banner.setDraftTitle(banner.getTitle());
        banner.setDraftTitleEn(banner.getTitleEn());
        banner.setDraftSubtitle(banner.getSubtitle());
        banner.setDraftSubtitleEn(banner.getSubtitleEn());
        banner.setDraftTargetType(banner.getTargetType());
        banner.setDraftTargetValue(banner.getTargetValue());
        banner.setDraftSortNo(banner.getSortNo());
        banner.setDraftStartTime(banner.getStartTime());
        banner.setDraftEndTime(banner.getEndTime());
    }

    private BasketHomeBanner buildPublishedRow(BasketHomeBanner row) {
        BasketHomeBanner next = new BasketHomeBanner();
        next.setId(row.getId());
        boolean fromDraft = Objects.equals(row.getHasDraft(), 1);
        next.setTitle(fromDraft ? row.getDraftTitle() : row.getTitle());
        next.setTitleEn(fromDraft ? row.getDraftTitleEn() : row.getTitleEn());
        next.setSubtitle(fromDraft ? row.getDraftSubtitle() : row.getSubtitle());
        next.setSubtitleEn(fromDraft ? row.getDraftSubtitleEn() : row.getSubtitleEn());
        next.setImageFileId(effectiveImageId(row));
        next.setMobileImageFileId(fromDraft ? row.getDraftMobileImageFileId() : row.getMobileImageFileId());
        next.setAltText(fromDraft ? row.getDraftAltText() : row.getAltText());
        next.setTargetType(fromDraft ? row.getDraftTargetType() : row.getTargetType());
        next.setTargetValue(fromDraft ? row.getDraftTargetValue() : row.getTargetValue());
        next.setStartTime(fromDraft ? row.getDraftStartTime() : row.getStartTime());
        next.setEndTime(fromDraft ? row.getDraftEndTime() : row.getEndTime());
        Integer sortNo = fromDraft ? row.getDraftSortNo() : row.getSortNo();
        next.setSortNo(sortNo == null ? 0 : sortNo);
        next.setBannerStatus(willEnable(row)
                ? BannerStatusEnum.PUBLISHED.getCode()
                : BannerStatusEnum.OFFLINE.getCode());
        return next;
    }

    private boolean isPending(BasketHomeBanner row) {
        return Objects.equals(row.getBannerStatus(), BannerStatusEnum.DRAFT.getCode())
                || Objects.equals(row.getHasDraft(), 1);
    }

    private boolean isRemove(BasketHomeBanner row) {
        return Objects.equals(row.getHasDraft(), 1) && Objects.equals(row.getDraftRemove(), 1);
    }

    private boolean willEnable(BasketHomeBanner row) {
        if (isRemove(row)) {
            return false;
        }
        if (row.getDraftEnabled() != null) {
            return Objects.equals(row.getDraftEnabled(), 1);
        }
        return !Objects.equals(row.getBannerStatus(), BannerStatusEnum.OFFLINE.getCode());
    }

    private Long effectiveImageId(BasketHomeBanner row) {
        if (Objects.equals(row.getHasDraft(), 1) && row.getDraftImageFileId() != null) {
            return row.getDraftImageFileId();
        }
        return row.getImageFileId();
    }

    private int countLive(List<BasketHomeBanner> rows, List<BasketHomeBanner> pending) {
        int live = 0;
        for (BasketHomeBanner row : rows) {
            if (pending.contains(row)) {
                if (!isRemove(row) && willEnable(row)) {
                    live++;
                }
                continue;
            }
            if (Objects.equals(row.getBannerStatus(), BannerStatusEnum.PUBLISHED.getCode())) {
                live++;
            }
        }
        return live;
    }

    private String displayName(BasketHomeBanner row) {
        return StringUtils.isEmpty(row.getBannerName()) ? String.valueOf(row.getId()) : row.getBannerName();
    }

    private String normalizePosition(String positionCode) {
        String position = StringUtils.isEmpty(positionCode) ? DEFAULT_POSITION : positionCode.trim();
        if (!DEFAULT_POSITION.equals(position)) {
            throw new ServiceException("首页轮播只维护 HOME_TOP");
        }
        return position;
    }

    private int enabledFlag(Integer draftEnabled) {
        return Objects.equals(draftEnabled, 0) ? 0 : 1;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAltText(String altText) {
        if (altText == null) {
            return null;
        }
        String trimmed = altText.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 256) {
            throw new ServiceException("替代文字不能超过256字");
        }
        return trimmed;
    }

    private void validateTarget(BannerTargetTypeEnum targetType, String targetValue) {
        if (targetType == BannerTargetTypeEnum.NONE) {
            return;
        }
        if (targetType == BannerTargetTypeEnum.EXTERNAL) {
            if (StringUtils.isEmpty(targetValue)) {
                throw new ServiceException("外链跳转值不能为空");
            }
            ExternalUrlValidator.validateHttpUrl(targetValue, "跳转链接");
            return;
        }
        if (StringUtils.isEmpty(targetValue)) {
            throw new ServiceException("跳转值不能为空");
        }
        String lower = targetValue.trim().toLowerCase();
        if (lower.startsWith("javascript:")) {
            throw new ServiceException("跳转值禁止 javascript: 协议");
        }
    }

    private BasketHomeBanner require(Long id) {
        BasketHomeBanner existing = homeBannerMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("横幅不存在");
        }
        return existing;
    }

    private void fillStatusName(HomeBannerListResp resp) {
        if (resp == null || resp.getBannerStatus() == null) {
            return;
        }
        try {
            resp.setBannerStatusName(BannerStatusEnum.fromCode(resp.getBannerStatus()).getDescription());
        } catch (Exception ignored) {
            // ignore
        }
        resp.setPendingChangeLabel(pendingLabel(resp));
    }

    private String pendingLabel(HomeBannerListResp resp) {
        boolean pending = Objects.equals(resp.getBannerStatus(), BannerStatusEnum.DRAFT.getCode())
                || Objects.equals(resp.getHasDraft(), 1);
        if (!pending) {
            return null;
        }
        if (Objects.equals(resp.getDraftRemove(), 1)) {
            return "发布后删除";
        }
        if (Objects.equals(resp.getDraftEnabled(), 0)) {
            return "发布后停用";
        }
        return "有未发布修改";
    }
}
