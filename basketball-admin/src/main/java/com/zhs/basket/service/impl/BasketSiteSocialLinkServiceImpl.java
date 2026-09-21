package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketSiteSocialLink;
import com.zhs.basket.mapper.BasketSiteSocialLinkMapper;
import com.zhs.basket.req.SiteSocialLinkEditReq;
import com.zhs.basket.resp.SiteSocialLinkListResp;
import com.zhs.basket.service.IBasketSiteSocialLinkService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.support.ExternalUrlValidator;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BasketSiteSocialLinkServiceImpl implements IBasketSiteSocialLinkService {

    private static final Map<String, String> PLATFORM_NAMES = Map.of(
            "KOOK", "KOOK",
            "WECHAT", "微信",
            "BILIBILI", "B站",
            "DOUYIN", "抖音"
    );

    @Autowired
    private BasketSiteSocialLinkMapper socialLinkMapper;
    @Autowired
    private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<SiteSocialLinkListResp> selectList() {
        List<SiteSocialLinkListResp> list = socialLinkMapper.selectList();
        list.forEach(this::fillDisplay);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, SiteSocialLinkEditReq req) {
        if (req == null) {
            throw new ServiceException("数据不能为空");
        }
        BasketSiteSocialLink existing = socialLinkMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("社群配置不存在");
        }

        String title = requireText(req.getTitle(), "标题", 64);
        String subtitle = optionalText(req.getSubtitle(), "描述", 128);
        String buttonText = requireText(req.getButtonText(), "按钮文字", 32);
        String jumpUrl = normalizeJumpUrl(req.getJumpUrl());
        ExternalUrlValidator.validateHttpUrl(jumpUrl, "跳转链接", ExternalUrlValidator.DEFAULT_MAX_LENGTH);

        Integer sortOrder = req.getSortOrder();
        if (sortOrder == null) {
            throw new ServiceException("排序不能为空");
        }
        Integer status = req.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new ServiceException("状态仅允许启用或停用");
        }

        existing.setTitle(title);
        existing.setSubtitle(subtitle);
        existing.setButtonText(buttonText);
        existing.setJumpUrl(jumpUrl);
        existing.setSortOrder(sortOrder);
        existing.setStatus(status);
        if (socialLinkMapper.updateById(existing) != 1) {
            throw new ServiceException("保存失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    private void fillDisplay(SiteSocialLinkListResp resp) {
        if (resp == null) {
            return;
        }
        String code = resp.getPlatformCode() == null ? "" : resp.getPlatformCode().toUpperCase(Locale.ROOT);
        resp.setPlatformName(PLATFORM_NAMES.getOrDefault(code, code));
        if (resp.getStatus() != null) {
            resp.setStatusName(resp.getStatus() == 1 ? "启用" : "停用");
        }
    }

    private String requireText(String value, String fieldLabel, int maxLength) {
        if (StringUtils.isEmpty(value) || !StringUtils.hasText(value.trim())) {
            throw new ServiceException(fieldLabel + "不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ServiceException(fieldLabel + "长度不能超过" + maxLength);
        }
        return trimmed;
    }

    private String optionalText(String value, String fieldLabel, int maxLength) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ServiceException(fieldLabel + "长度不能超过" + maxLength);
        }
        return trimmed;
    }

    private String normalizeJumpUrl(String jumpUrl) {
        if (StringUtils.isEmpty(jumpUrl) || !StringUtils.hasText(jumpUrl.trim())) {
            return null;
        }
        return jumpUrl.trim();
    }
}
