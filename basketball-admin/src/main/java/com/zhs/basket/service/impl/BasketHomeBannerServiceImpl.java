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

@Service
public class BasketHomeBannerServiceImpl implements IBasketHomeBannerService {

    private static final Integer NOT_DELETED = 0;
    private static final String DEFAULT_POSITION = "HOME_TOP";

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
        BannerTargetTypeEnum targetType = BannerTargetTypeEnum.fromCode(req.getTargetType());
        validateTarget(targetType, req.getTargetValue());

        Date now = new Date();
        BasketHomeBanner banner = new BasketHomeBanner();
        banner.setBannerName(req.getBannerName().trim());
        banner.setTitle(req.getTitle());
        banner.setSubtitle(req.getSubtitle());
        banner.setImageFileId(req.getImageFileId());
        banner.setPositionCode(StringUtils.isEmpty(req.getPositionCode()) ? DEFAULT_POSITION : req.getPositionCode());
        banner.setTargetType(targetType.getCode());
        banner.setTargetValue(req.getTargetValue());
        banner.setStartTime(req.getStartTime());
        banner.setEndTime(req.getEndTime());
        banner.setBannerStatus(BannerStatusEnum.DRAFT.getCode());
        banner.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
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
        BannerStatusEnum status = BannerStatusEnum.fromCode(existing.getBannerStatus());
        if (!status.canEdit()) {
            throw new ServiceException("当前状态不可编辑");
        }
        if (req.getBannerName() != null) {
            existing.setBannerName(req.getBannerName().trim());
        }
        if (req.getTitle() != null) {
            existing.setTitle(req.getTitle());
        }
        if (req.getSubtitle() != null) {
            existing.setSubtitle(req.getSubtitle());
        }
        if (req.getImageFileId() != null) {
            existing.setImageFileId(req.getImageFileId());
        }
        if (req.getPositionCode() != null) {
            existing.setPositionCode(req.getPositionCode());
        }
        BannerTargetTypeEnum targetType = BannerTargetTypeEnum.fromCode(
                req.getTargetType() != null ? req.getTargetType() : existing.getTargetType());
        String targetValue = req.getTargetValue() != null ? req.getTargetValue() : existing.getTargetValue();
        validateTarget(targetType, targetValue);
        existing.setTargetType(targetType.getCode());
        existing.setTargetValue(targetValue);
        if (req.getStartTime() != null) {
            existing.setStartTime(req.getStartTime());
        }
        if (req.getEndTime() != null) {
            existing.setEndTime(req.getEndTime());
        }
        if (req.getSortNo() != null) {
            existing.setSortNo(req.getSortNo());
        }
        if (homeBannerMapper.updateById(existing) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketHomeBanner existing = require(id);
        BannerStatusEnum status = BannerStatusEnum.fromCode(existing.getBannerStatus());
        if (!status.canPublish()) {
            throw new ServiceException("当前状态不可发布");
        }
        if (homeBannerMapper.updateStatusIfMatch(id, existing.getBannerStatus(),
                BannerStatusEnum.PUBLISHED.getCode()) != 1) {
            throw new ServiceException("发布失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        BasketHomeBanner existing = require(id);
        BannerStatusEnum status = BannerStatusEnum.fromCode(existing.getBannerStatus());
        if (!status.canOffline()) {
            throw new ServiceException("当前状态不可下线");
        }
        if (homeBannerMapper.updateStatusIfMatch(id, existing.getBannerStatus(),
                BannerStatusEnum.OFFLINE.getCode()) != 1) {
            throw new ServiceException("下线失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        BasketHomeBanner existing = require(id);
        BannerStatusEnum status = BannerStatusEnum.fromCode(existing.getBannerStatus());
        if (status == BannerStatusEnum.PUBLISHED) {
            throw new ServiceException("已发布横幅不可删除，请先下线");
        }
        if (homeBannerMapper.softDelete(id, Arrays.asList(
                BannerStatusEnum.DRAFT.getCode(), BannerStatusEnum.OFFLINE.getCode())) != 1) {
            throw new ServiceException("删除失败");
        }
        publicCacheCleaner.clearAfterCommit();
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
    }
}
