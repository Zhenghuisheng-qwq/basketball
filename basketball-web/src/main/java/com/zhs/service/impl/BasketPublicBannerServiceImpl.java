package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketHomeBannerDao;
import com.zhs.model.BasketHomeBanner;
import com.zhs.response.publicapi.PublicHomeBannerResp;
import com.zhs.service.IBasketPublicBannerService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BasketPublicBannerServiceImpl implements IBasketPublicBannerService {

    @Resource
    private BasketHomeBannerDao homeBannerDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public List<PublicHomeBannerResp> listBanners(String positionCode) {
        String positionKey = StringUtils.hasText(positionCode) ? positionCode.trim() : "ALL";
        return publicSupport.cacheGetOrLoad("banner:list:" + positionKey, () -> loadBanners(positionCode));
    }

    private List<PublicHomeBannerResp> loadBanners(String positionCode) {
        Date now = new Date();
        LambdaQueryWrapper<BasketHomeBanner> wrapper = new LambdaQueryWrapper<BasketHomeBanner>()
                .eq(BasketHomeBanner::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketHomeBanner::getBannerStatus, BasketPublicConstants.PUBLISHED)
                .and(w -> w.isNull(BasketHomeBanner::getStartTime).or().le(BasketHomeBanner::getStartTime, now))
                .and(w -> w.isNull(BasketHomeBanner::getEndTime).or().gt(BasketHomeBanner::getEndTime, now))
                .eq(StringUtils.hasText(positionCode), BasketHomeBanner::getPositionCode,
                        StringUtils.hasText(positionCode) ? positionCode.trim() : null)
                .orderByAsc(BasketHomeBanner::getSortNo)
                .orderByDesc(BasketHomeBanner::getId);
        List<BasketHomeBanner> list = homeBannerDao.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        Set<Long> fileIds = new HashSet<>();
        for (BasketHomeBanner item : list) {
            if (item.getImageFileId() != null) {
                fileIds.add(item.getImageFileId());
            }
            if (item.getMobileImageFileId() != null) {
                fileIds.add(item.getMobileImageFileId());
            }
        }
        Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(fileIds);
        List<PublicHomeBannerResp> result = new ArrayList<>(list.size());
        for (BasketHomeBanner item : list) {
            PublicHomeBannerResp vo = new PublicHomeBannerResp();
            vo.setBannerId(item.getId());
            vo.setTitle(item.getTitle());
            vo.setTitleEn(item.getTitleEn());
            vo.setSubtitle(item.getSubtitle());
            vo.setSubtitleEn(item.getSubtitleEn());
            vo.setImageUrl(urlMap.get(item.getImageFileId()));
            vo.setMobileImageUrl(urlMap.get(item.getMobileImageFileId()));
            vo.setAltText(item.getAltText());
            vo.setPositionCode(item.getPositionCode());
            vo.setTargetType(item.getTargetType());
            vo.setTargetValue(item.getTargetValue());
            vo.setSortNo(item.getSortNo());
            result.add(vo);
        }
        return result;
    }
}
