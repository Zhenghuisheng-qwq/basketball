package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPartnerDao;
import com.zhs.model.BasketPartner;
import com.zhs.response.publicapi.PublicPartnerResp;
import com.zhs.service.IBasketPublicPartnerService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketPublicPartnerServiceImpl implements IBasketPublicPartnerService {

    @Resource
    private BasketPartnerDao partnerDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public List<PublicPartnerResp> listPartners() {
        return publicSupport.cacheGetOrLoad("partner:list", () -> {
            List<BasketPartner> list = partnerDao.selectList(new LambdaQueryWrapper<BasketPartner>()
                    .eq(BasketPartner::getIsDelete, BasketPublicConstants.NOT_DELETED)
                    .eq(BasketPartner::getPartnerStatus, BasketPublicConstants.PUBLISHED)
                    .orderByAsc(BasketPartner::getSortNo)
                    .orderByDesc(BasketPartner::getId));
            if (CollectionUtils.isEmpty(list)) {
                return Collections.emptyList();
            }
            Set<Long> fileIds = list.stream()
                    .map(BasketPartner::getLogoFileId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
            Map<Long, String> urlMap = publicSupport.loadAccessUrlMap(fileIds);
            List<PublicPartnerResp> result = new ArrayList<>(list.size());
            for (BasketPartner item : list) {
                PublicPartnerResp vo = new PublicPartnerResp();
                vo.setPartnerId(item.getId());
                vo.setPartnerName(item.getPartnerName());
                vo.setLogoUrl(urlMap.get(item.getLogoFileId()));
                vo.setWebsiteUrl(item.getWebsiteUrl());
                vo.setPartnerType(item.getPartnerType());
                vo.setSortNo(item.getSortNo());
                result.add(vo);
            }
            return result;
        });
    }
}
