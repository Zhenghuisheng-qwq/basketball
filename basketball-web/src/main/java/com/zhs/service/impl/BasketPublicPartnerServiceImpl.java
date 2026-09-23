package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPartnerDao;
import com.zhs.dao.BasketPartnerSupportRecordPublicDao;
import com.zhs.model.BasketPartner;
import com.zhs.response.publicapi.PublicPartnerPageResp;
import com.zhs.response.publicapi.PublicPartnerResp;
import com.zhs.response.publicapi.PublicPartnerSummaryResp;
import com.zhs.response.publicapi.PublicPartnerSupportRecordResp;
import com.zhs.response.publicapi.PublicPartnersResp;
import com.zhs.service.IBasketPublicPartnerService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import com.zhs.service.support.PartnerAmountFormatter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
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

    private static final int DEFAULT_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 50;

    @Resource
    private BasketPartnerDao partnerDao;
    @Resource
    private BasketPartnerSupportRecordPublicDao supportRecordPublicDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public List<PublicPartnerResp> listPartners() {
        return publicSupport.cacheGetOrLoad("partner:list", () -> {
            List<BasketPartner> list = partnerDao.selectList(new LambdaQueryWrapper<BasketPartner>()
                    .eq(BasketPartner::getIsDelete, BasketPublicConstants.NOT_DELETED)
                    .eq(BasketPartner::getPartnerStatus, BasketPublicConstants.PUBLISHED)
                    .orderByDesc(BasketPartner::getSortNo)
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

    @Override
    public PublicPartnersResp pagePartners(Integer currentPage, Integer pageSize) {
        int page = publicSupport.normalizePage(currentPage);
        int size = normalizePartnerPageSize(pageSize);
        String cacheKey = "partner:wall:records:" + page + ":" + size;
        return publicSupport.cacheGetOrLoad(cacheKey, () -> buildPartnersResp(page, size));
    }

    private PublicPartnersResp buildPartnersResp(int page, int size) {
        PublicPartnersResp resp = new PublicPartnersResp();

        PublicPartnerSummaryResp summary = new PublicPartnerSummaryResp();
        Long count = supportRecordPublicDao.countPublicRecords();
        BigDecimal totalCash = supportRecordPublicDao.sumPublicCashAmount();
        summary.setSupportRecordCount(count == null ? 0L : count);
        summary.setTotalCashAmount(PartnerAmountFormatter.normalizeAmount(totalCash));
        resp.setSummary(summary);

        PublicPartnerPageResp pageResp = new PublicPartnerPageResp();
        Long rowCount = supportRecordPublicDao.countPublicSupportRows();
        long total = rowCount == null ? 0L : rowCount;
        pageResp.setTotal(total);
        if (total <= 0) {
            pageResp.setRows(Collections.emptyList());
            resp.setPage(pageResp);
            return resp;
        }
        long offset = (long) (page - 1) * size;
        List<PublicPartnerSupportRecordResp> rows = supportRecordPublicDao.selectPublicPage(offset, size);
        if (rows == null) {
            rows = Collections.emptyList();
        }
        rows.forEach(PartnerAmountFormatter::fillDisplay);
        pageResp.setRows(rows);
        resp.setPage(pageResp);
        return resp;
    }

    private int normalizePartnerPageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
