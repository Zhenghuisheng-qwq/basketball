package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPartner;
import com.zhs.basket.enums.PartnerStatusEnum;
import com.zhs.basket.mapper.BasketPartnerMapper;
import com.zhs.basket.mapper.BasketPartnerSupportRecordMapper;
import com.zhs.basket.req.PartnerAddReq;
import com.zhs.basket.req.PartnerEditReq;
import com.zhs.basket.req.PartnerQueryReq;
import com.zhs.basket.resp.PartnerListResp;
import com.zhs.basket.service.IBasketPartnerService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.basket.support.ExternalUrlValidator;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class BasketPartnerServiceImpl implements IBasketPartnerService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketPartnerMapper partnerMapper;
    @Autowired private BasketPartnerSupportRecordMapper supportRecordMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<PartnerListResp> selectList(PartnerQueryReq query) {
        List<PartnerListResp> list = partnerMapper.selectList(query == null ? new PartnerQueryReq() : query);
        list.forEach(this::fillStatusName);
        return list;
    }

    @Override
    public PartnerListResp selectById(Long id) {
        PartnerListResp detail = partnerMapper.selectDetailById(id);
        if (detail == null) {
            throw new ServiceException("合作伙伴不存在");
        }
        fillStatusName(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(PartnerAddReq req) {
        if (req == null || StringUtils.isEmpty(req.getPartnerName())) {
            throw new ServiceException("合作伙伴名称不能为空");
        }
        ExternalUrlValidator.validateHttpUrl(req.getWebsiteUrl(), "网站地址");
        if (partnerMapper.countByName(req.getPartnerName().trim(), null) > 0) {
            throw new ServiceException("合作伙伴名称已存在");
        }
        Date now = new Date();
        BasketPartner partner = new BasketPartner();
        partner.setPartnerCode(generatePartnerCode());
        partner.setPartnerName(req.getPartnerName().trim());
        partner.setLogoFileId(req.getLogoFileId());
        partner.setWebsiteUrl(req.getWebsiteUrl());
        partner.setPublicDescription(StringUtils.trim(req.getPublicDescription()));
        partner.setPartnerType(req.getPartnerType());
        partner.setCooperationStartDate(req.getCooperationStartDate());
        partner.setCooperationEndDate(req.getCooperationEndDate());
        Integer statusCode = req.getPartnerStatus() == null
                ? PartnerStatusEnum.PUBLISHED.getCode()
                : PartnerStatusEnum.fromCode(req.getPartnerStatus()).getCode();
        partner.setPartnerStatus(statusCode);
        partner.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
        partner.setRemark(req.getRemark());
        partner.setCreateBy(SecurityUtils.getUsername());
        partner.setUpdateBy(SecurityUtils.getUsername());
        partner.setCreateTime(now);
        partner.setUpdateTime(now);
        partner.setIsDelete(NOT_DELETED);
        partnerMapper.insert(partner);
        if (PartnerStatusEnum.PUBLISHED.getCode() == statusCode) {
            publicCacheCleaner.clearAfterCommit();
        }
        return partner.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, PartnerEditReq req) {
        BasketPartner existing = require(id);
        PartnerStatusEnum status = PartnerStatusEnum.fromCode(existing.getPartnerStatus());
        if (!status.canEdit()) {
            throw new ServiceException("当前状态不可编辑");
        }
        if (req.getPartnerName() != null) {
            String name = req.getPartnerName().trim();
            if (partnerMapper.countByName(name, id) > 0) {
                throw new ServiceException("合作伙伴名称已存在");
            }
            existing.setPartnerName(name);
        }
        if (req.getLogoFileId() != null) {
            existing.setLogoFileId(req.getLogoFileId());
        }
        if (req.getWebsiteUrl() != null) {
            ExternalUrlValidator.validateHttpUrl(req.getWebsiteUrl(), "网站地址");
            existing.setWebsiteUrl(req.getWebsiteUrl());
        }
        if (req.getPublicDescription() != null) {
            existing.setPublicDescription(StringUtils.trim(req.getPublicDescription()));
        }
        if (req.getPartnerType() != null) {
            existing.setPartnerType(req.getPartnerType());
        }
        if (req.getCooperationStartDate() != null) {
            existing.setCooperationStartDate(req.getCooperationStartDate());
        }
        if (req.getCooperationEndDate() != null) {
            existing.setCooperationEndDate(req.getCooperationEndDate());
        }
        if (req.getSortNo() != null) {
            existing.setSortNo(req.getSortNo());
        }
        if (req.getRemark() != null) {
            existing.setRemark(req.getRemark());
        }
        existing.setUpdateBy(SecurityUtils.getUsername());
        if (partnerMapper.updateById(existing) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketPartner existing = require(id);
        PartnerStatusEnum status = PartnerStatusEnum.fromCode(existing.getPartnerStatus());
        if (!status.canPublish()) {
            throw new ServiceException("当前状态不可发布");
        }
        if (partnerMapper.updateStatusIfMatch(id, existing.getPartnerStatus(),
                PartnerStatusEnum.PUBLISHED.getCode()) != 1) {
            throw new ServiceException("发布失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        BasketPartner existing = require(id);
        PartnerStatusEnum status = PartnerStatusEnum.fromCode(existing.getPartnerStatus());
        if (!status.canOffline()) {
            throw new ServiceException("当前状态不可下线");
        }
        if (partnerMapper.updateStatusIfMatch(id, existing.getPartnerStatus(),
                PartnerStatusEnum.OFFLINE.getCode()) != 1) {
            throw new ServiceException("下线失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        BasketPartner existing = require(id);
        PartnerStatusEnum status = PartnerStatusEnum.fromCode(existing.getPartnerStatus());
        if (status == PartnerStatusEnum.PUBLISHED) {
            throw new ServiceException("展示中的合作伙伴不可删除，请先改为「不展示」");
        }
        if (supportRecordMapper.countActiveByPartnerId(id) > 0) {
            throw new ServiceException("该合作伙伴已有赞助记录，不可删除，请改为「不展示」");
        }
        if (partnerMapper.softDelete(id, Arrays.asList(
                PartnerStatusEnum.DRAFT.getCode(), PartnerStatusEnum.OFFLINE.getCode())) != 1) {
            throw new ServiceException("删除失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long findOrCreatePublishedByName(String partnerName) {
        if (StringUtils.isEmpty(partnerName)) {
            throw new ServiceException("合作伙伴名称不能为空");
        }
        String name = partnerName.trim();
        BasketPartner existing = partnerMapper.selectByName(name);
        if (existing != null) {
            PartnerStatusEnum status = PartnerStatusEnum.fromCode(existing.getPartnerStatus());
            if (!status.isPublic()) {
                if (partnerMapper.updateStatusIfMatch(existing.getId(), existing.getPartnerStatus(),
                        PartnerStatusEnum.PUBLISHED.getCode()) != 1) {
                    throw new ServiceException("合作伙伴启用失败");
                }
                publicCacheCleaner.clearAfterCommit();
            }
            return existing.getId();
        }
        Date now = new Date();
        BasketPartner partner = new BasketPartner();
        partner.setPartnerCode(generatePartnerCode());
        partner.setPartnerName(name);
        partner.setPartnerStatus(PartnerStatusEnum.PUBLISHED.getCode());
        partner.setSortNo(0);
        partner.setCreateBy(SecurityUtils.getUsername());
        partner.setUpdateBy(SecurityUtils.getUsername());
        partner.setCreateTime(now);
        partner.setUpdateTime(now);
        partner.setIsDelete(NOT_DELETED);
        partnerMapper.insert(partner);
        publicCacheCleaner.clearAfterCommit();
        return partner.getId();
    }

    private BasketPartner require(Long id) {
        BasketPartner existing = partnerMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("合作伙伴不存在");
        }
        return existing;
    }

    private void fillStatusName(PartnerListResp resp) {
        if (resp == null || resp.getPartnerStatus() == null) {
            return;
        }
        try {
            resp.setPartnerStatusName(PartnerStatusEnum.fromCode(resp.getPartnerStatus()).getDescription());
        } catch (Exception ignored) {
            // ignore
        }
    }

    private String generatePartnerCode() {
        Integer maxSeq = partnerMapper.selectMaxPartnerCodeSequence();
        int next = maxSeq != null ? maxSeq + 1 : 1;
        return "CNPA-PT-" + String.format("%06d", next);
    }
}
