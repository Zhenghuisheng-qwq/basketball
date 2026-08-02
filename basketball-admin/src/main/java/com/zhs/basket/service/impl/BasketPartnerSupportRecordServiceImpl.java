package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPartner;
import com.zhs.basket.domain.BasketPartnerSupportRecord;
import com.zhs.basket.enums.PartnerStatusEnum;
import com.zhs.basket.enums.PartnerSupportTypeEnum;
import com.zhs.basket.mapper.BasketPartnerMapper;
import com.zhs.basket.mapper.BasketPartnerSupportRecordMapper;
import com.zhs.basket.req.PartnerSupportAddReq;
import com.zhs.basket.req.PartnerSupportEditReq;
import com.zhs.basket.req.PartnerSupportQueryReq;
import com.zhs.basket.resp.PartnerSupportListResp;
import com.zhs.basket.service.IBasketPartnerService;
import com.zhs.basket.service.IBasketPartnerSupportRecordService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketPartnerSupportRecordServiceImpl implements IBasketPartnerSupportRecordService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer PUBLISHED = 1;
    private static final Integer DISABLED = 0;

    @Autowired
    private BasketPartnerSupportRecordMapper supportRecordMapper;
    @Autowired
    private BasketPartnerMapper partnerMapper;
    @Autowired
    private IBasketPartnerService partnerService;
    @Autowired
    private BasketPublicCacheCleaner publicCacheCleaner;

    @Override
    public List<PartnerSupportListResp> selectList(PartnerSupportQueryReq query) {
        List<PartnerSupportListResp> list = supportRecordMapper.selectList(query == null ? new PartnerSupportQueryReq() : query);
        list.forEach(this::fillDisplayFields);
        return list;
    }

    @Override
    public PartnerSupportListResp selectById(Long id) {
        PartnerSupportListResp detail = supportRecordMapper.selectDetailById(id);
        if (detail == null) {
            throw new ServiceException("支持记录不存在");
        }
        fillDisplayFields(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(PartnerSupportAddReq req) {
        Long partnerId = resolvePartnerId(req.getPartnerId(), req.getPartnerName());
        String supportType = StringUtils.isEmpty(req.getSupportType())
                ? PartnerSupportTypeEnum.CASH.getCode()
                : req.getSupportType();
        validateReq(partnerId, supportType, req.getSupportAmount(), req.getSupportDate());
        Date now = new Date();
        BasketPartnerSupportRecord record = new BasketPartnerSupportRecord();
        record.setPartnerId(partnerId);
        record.setSupportType(PartnerSupportTypeEnum.fromCode(supportType).getCode());
        record.setSupportAmount(req.getSupportAmount());
        record.setSupportDate(req.getSupportDate());
        record.setPublicDescription(StringUtils.trim(req.getPublicDescription()));
        record.setPublishStatus(req.getPublishStatus() == null ? PUBLISHED : req.getPublishStatus());
        record.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
        record.setCreateBy(SecurityUtils.getUsername());
        record.setUpdateBy(SecurityUtils.getUsername());
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        if (supportRecordMapper.insert(record) != 1) {
            throw new ServiceException("新增支持记录失败");
        }
        publicCacheCleaner.clearAfterCommit();
        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(Long id, PartnerSupportEditReq req) {
        BasketPartnerSupportRecord existing = require(id);
        if (req.getPartnerId() != null) {
            requirePartner(req.getPartnerId());
            existing.setPartnerId(req.getPartnerId());
        } else if (StringUtils.isNotEmpty(req.getPartnerName())) {
            existing.setPartnerId(resolvePartnerId(null, req.getPartnerName()));
        }
        if (req.getSupportType() != null) {
            PartnerSupportTypeEnum type = PartnerSupportTypeEnum.fromCode(req.getSupportType());
            PartnerSupportTypeEnum.validateAmount(type.getCode(), req.getSupportAmount() != null ? req.getSupportAmount() : existing.getSupportAmount());
            existing.setSupportType(type.getCode());
        }
        if (req.getSupportAmount() != null || (req.getSupportType() != null && PartnerSupportTypeEnum.CASH.getCode().equals(existing.getSupportType()))) {
            PartnerSupportTypeEnum.validateAmount(existing.getSupportType(), req.getSupportAmount());
            existing.setSupportAmount(req.getSupportAmount());
        }
        if (req.getSupportDate() != null) {
            existing.setSupportDate(req.getSupportDate());
        }
        if (req.getPublicDescription() != null) {
            existing.setPublicDescription(StringUtils.trim(req.getPublicDescription()));
        }
        if (req.getPublishStatus() != null) {
            existing.setPublishStatus(req.getPublishStatus());
        }
        if (req.getSortOrder() != null) {
            existing.setSortOrder(req.getSortOrder());
        }
        existing.setUpdateBy(SecurityUtils.getUsername());
        if (supportRecordMapper.updateById(existing) != 1) {
            throw new ServiceException("更新支持记录失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        BasketPartnerSupportRecord existing = require(id);
        requirePublishedPartner(existing.getPartnerId());
        if (supportRecordMapper.updatePublishStatus(id, existing.getPublishStatus(), PUBLISHED) != 1) {
            throw new ServiceException("启用失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        BasketPartnerSupportRecord existing = require(id);
        if (supportRecordMapper.updatePublishStatus(id, existing.getPublishStatus(), DISABLED) != 1) {
            throw new ServiceException("停用失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        require(id);
        if (supportRecordMapper.softDelete(id) != 1) {
            throw new ServiceException("删除失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    private void validateReq(Long partnerId, String supportType, java.math.BigDecimal supportAmount, Date supportDate) {
        requirePartner(partnerId);
        if (supportDate == null) {
            throw new ServiceException("赞助日期不能为空");
        }
        PartnerSupportTypeEnum type = PartnerSupportTypeEnum.fromCode(supportType);
        PartnerSupportTypeEnum.validateAmount(type.getCode(), supportAmount);
    }

    private Long resolvePartnerId(Long partnerId, String partnerName) {
        if (partnerId != null) {
            requirePartner(partnerId);
            return partnerId;
        }
        return partnerService.findOrCreatePublishedByName(partnerName);
    }

    private BasketPartner requirePartner(Long partnerId) {
        if (partnerId == null) {
            throw new ServiceException("合作伙伴不能为空");
        }
        BasketPartner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new ServiceException("合作伙伴不存在");
        }
        return partner;
    }

    private void requirePublishedPartner(Long partnerId) {
        BasketPartner partner = requirePartner(partnerId);
        if (!PartnerStatusEnum.fromCode(partner.getPartnerStatus()).isPublic()) {
            throw new ServiceException("合作伙伴未启用，无法公开支持记录");
        }
    }

    private BasketPartnerSupportRecord require(Long id) {
        BasketPartnerSupportRecord existing = supportRecordMapper.selectById(id);
        if (existing == null) {
            throw new ServiceException("支持记录不存在");
        }
        return existing;
    }

    private void fillDisplayFields(PartnerSupportListResp resp) {
        if (resp == null) {
            return;
        }
        try {
            resp.setSupportTypeName(PartnerSupportTypeEnum.fromCode(resp.getSupportType()).getDescription());
        } catch (Exception ignored) {
            resp.setSupportTypeName(resp.getSupportType());
        }
        if (resp.getPublishStatus() != null) {
            resp.setPublishStatusName(resp.getPublishStatus() == 1 ? "已公开" : "已停用");
        }
    }
}
