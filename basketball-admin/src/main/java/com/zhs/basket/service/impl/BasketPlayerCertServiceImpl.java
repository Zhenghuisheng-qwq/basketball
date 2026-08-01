package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.req.BatchAuditOperationReq;
import com.zhs.basket.req.PlayerCertApproveReq;
import com.zhs.basket.req.PlayerCertRejectReq;
import com.zhs.basket.support.BatchAuditSupport;
import com.zhs.basket.vo.BatchAuditResultVo;
import com.zhs.basket.service.IBasketPlayerCertService;
import com.zhs.basket.support.*;
import com.zhs.basket.vo.*;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketPlayerCertServiceImpl implements IBasketPlayerCertService {

    private static final Integer NOT_DELETED = 0;
    private static final int CERT_APPROVED = PlayerCertStatusEnum.APPROVED.getCode();
    private static final int CERT_REJECTED = PlayerCertStatusEnum.REJECTED.getCode();
    private static final int PRIMARY = 1;
    private static final String DEFAULT_REJECT_REASON = "认证信息未通过审核";

    @Autowired private BasketPlayerCertApplicationMapper applicationMapper;
    @Autowired private BasketPlayerCertAuditMapper auditMapper;
    @Autowired private BasketPlayerCertMaterialMapper materialMapper;
    @Autowired private BasketFileAssetMapper fileAssetMapper;
    @Autowired private BasketPlayerProfileMapper profileMapper;
    @Autowired private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired private PlatformIdentityHistoryWriter platformIdentityHistoryWriter;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private BasketAdminMessageService messageService;
    @Autowired private BasketAccountSecurityLogWriter securityLogWriter;

    @Override
    public List<PlayerCertListVo> selectList(PlayerCertQuery query) {
        List<PlayerCertListVo> list = applicationMapper.selectList(query);
        list.forEach(vo ->
                vo.setApplicationStatusName(PlayerCertStatusEnum.fromCode(vo.getApplicationStatus()).getAuditDisplayDescription())
        );
        return list;
    }

    @Override
    public PlayerCertDetailVo selectDetail(Long applicationId) {
        PlayerCertDetailVo vo = applicationMapper.selectDetailVo(applicationId);
        if (vo == null) {
            throw new ServiceException("认证申请不存在");
        }
        vo.setApplicationStatusName(PlayerCertStatusEnum.fromCode(vo.getApplicationStatus()).getAuditDisplayDescription());
        vo.setMaterials(loadMaterials(applicationId));
        vo.setAuditRecords(selectAuditRecords(applicationId));
        return vo;
    }

    private List<PlayerCertMaterialVo> loadMaterials(Long applicationId) {
        List<BasketPlayerCertMaterial> materials = materialMapper.selectByApplicationId(applicationId);
        if (materials == null || materials.isEmpty()) {
            return Collections.emptyList();
        }
        List<PlayerCertMaterialVo> result = new ArrayList<>();
        for (BasketPlayerCertMaterial material : materials) {
            PlayerCertMaterialVo item = new PlayerCertMaterialVo();
            item.setMaterialId(material.getId());
            item.setMaterialType(material.getMaterialType());
            item.setFileId(material.getFileId());
            item.setMaterialName(material.getMaterialName());
            item.setSortNo(material.getSortNo());
            item.setUploadTime(material.getCreateTime());
            if (material.getFileId() != null) {
                BasketFileAsset file = fileAssetMapper.selectById(material.getFileId());
                if (file != null && NOT_DELETED.equals(file.getIsDelete())) {
                    item.setFileName(file.getOriginalName() != null ? file.getOriginalName() : file.getFileName());
                    item.setMimeType(file.getMimeType());
                    item.setFileSize(file.getFileSize());
                    item.setAccessUrl(file.getAccessUrl());
                }
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public List<PlayerCertAuditRecordVo> selectAuditRecords(Long applicationId) {
        return auditMapper.selectByApplicationId(applicationId).stream().map(r -> {
            PlayerCertAuditRecordVo vo = new PlayerCertAuditRecordVo();
            BeanUtils.copyBeanProp(vo, r);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startReview(Long applicationId) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        Long auditorId = SecurityUtils.getUserId();
        int rows = applicationMapper.updateStatusIfMatch(applicationId,
                Collections.singletonList(PlayerCertStatusEnum.SUBMITTED.getCode()),
                PlayerCertStatusEnum.REVIEWING.getCode(), auditorId, null, null, null);
        assertSingleRow(rows, "开始审核失败，状态已变更");
        writeAudit(app, "START_REVIEW", app.getApplicationStatus(), PlayerCertStatusEnum.REVIEWING.getCode(), "开始审核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long applicationId, PlayerCertApproveReq req) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        if (req == null) {
            req = new PlayerCertApproveReq();
        }
        String normalizedGameAccount = GameAccountNormalizer.normalize(app.getGameAccount());
        if (!StringUtils.hasText(normalizedGameAccount)) {
            throw new ServiceException("游戏账号不能为空");
        }
        if (platformAccountMapper.countActiveByPlatformAndNormalized(app.getGameTerminal(), normalizedGameAccount) > 0) {
            throw new ServiceException("该平台游戏ID已被使用");
        }
        Long auditorId = SecurityUtils.getUserId();
        Date now = new Date();
        int rows = applicationMapper.updateStatusIfMatch(applicationId,
                Arrays.asList(PlayerCertStatusEnum.SUBMITTED.getCode(), PlayerCertStatusEnum.REVIEWING.getCode()),
                PlayerCertStatusEnum.APPROVED.getCode(), auditorId, req.getAuditRemark(), now, auditorId);
        assertSingleRow(rows, "审核通过失败，状态已变更");
        BasketPlayerProfile profile = upsertProfile(app, req.getPositionCode(), now);
        BasketPlayerPlatformAccount primaryAccount = insertPrimaryPlatformAccountIfAbsent(
                app, profile, normalizedGameAccount, auditorId, now);
        if (primaryAccount != null) {
            platformIdentityHistoryWriter.writeInitialBind(app.getMemberId(), profile.getId(), primaryAccount,
                    now, app.getId(), auditorId);
        }
        applicationMapper.updatePlayerIdAndNormalized(applicationId, profile.getId(), normalizedGameAccount);
        writeAudit(app, "APPROVE", app.getApplicationStatus(), PlayerCertStatusEnum.APPROVED.getCode(), req.getAuditRemark());
        messageService.sendSiteMessage(app.getMemberId(), auditorId, "PLAYER_CERT", applicationId,
                "球员认证已通过", "您的球员认证申请已通过审核。");
        securityLogWriter.write(app.getMemberId(), "PLAYER_CERT_APPROVED", "球员认证通过", null, "已通过", auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long applicationId, PlayerCertRejectReq req) {
        BasketPlayerCertApplication app = requireApplication(applicationId);
        Long auditorId = SecurityUtils.getUserId();
        Date now = new Date();
        String remark = resolveRejectRemark(req);
        int rows = applicationMapper.updateStatusIfMatch(applicationId,
                Arrays.asList(PlayerCertStatusEnum.SUBMITTED.getCode(), PlayerCertStatusEnum.REVIEWING.getCode()),
                PlayerCertStatusEnum.REJECTED.getCode(), auditorId, remark, now, auditorId);
        assertSingleRow(rows, "驳回失败，状态已变更");
        updateProfileRejected(app, now);
        writeAudit(app, "REJECT", app.getApplicationStatus(), PlayerCertStatusEnum.REJECTED.getCode(), remark);
        messageService.sendSiteMessage(app.getMemberId(), auditorId, "PLAYER_CERT", applicationId,
                "球员认证已驳回", "您的球员认证申请已被驳回：" + remark);
        securityLogWriter.write(app.getMemberId(), "PLAYER_CERT_REJECTED", "球员认证驳回", null, remark, auditorId);
    }

    @Override
    public BatchAuditResultVo batchApprove(BatchAuditOperationReq req) {
        PlayerCertApproveReq approveReq = new PlayerCertApproveReq();
        approveReq.setAuditRemark(req.getAuditRemark());
        approveReq.setPositionCode(req.getPositionCode());
        return BatchAuditSupport.run(req.getIds(), id -> approve(id, approveReq));
    }

    @Override
    public BatchAuditResultVo batchReject(BatchAuditOperationReq req) {
        PlayerCertRejectReq rejectReq = new PlayerCertRejectReq();
        rejectReq.setRejectReason(req.getRejectReason());
        rejectReq.setAuditRemark(req.getAuditRemark());
        return BatchAuditSupport.run(req.getIds(), id -> reject(id, rejectReq));
    }

    private String resolveRejectRemark(PlayerCertRejectReq req) {
        if (req != null && StringUtils.hasText(req.getRejectReason())) {
            return req.getRejectReason();
        }
        if (req != null && StringUtils.hasText(req.getAuditRemark())) {
            return req.getAuditRemark();
        }
        return DEFAULT_REJECT_REASON;
    }

    private BasketPlayerProfile upsertProfile(BasketPlayerCertApplication app, String positionCode, Date certifiedTime) {
        BasketPlayerProfile profile = profileMapper.selectByMemberId(app.getMemberId());
        if (profile == null) {
            profile = new BasketPlayerProfile();
            profile.setMemberId(app.getMemberId());
            profile.setPlayerNo(playerNoGenerator.generatePlayerNo());
            profile.setIsDelete(NOT_DELETED);
        } else if (!StringUtils.hasText(profile.getPlayerNo())) {
            profile.setPlayerNo(playerNoGenerator.generatePlayerNo());
        }
        profile.setGameTerminal(app.getGameTerminal());
        profile.setGameAccount(app.getGameAccount());
        profile.setGameNickname(app.getGameNickname());
        if (StringUtils.hasText(positionCode)) {
            profile.setPositionCode(positionCode);
        } else if (StringUtils.hasText(app.getPositionCode())) {
            profile.setPositionCode(app.getPositionCode());
        }
        profile.setCertificationStatus(CERT_APPROVED);
        profile.setCertifiedTime(certifiedTime);
        if (profile.getPlayerStatus() == null) {
            profile.setPlayerStatus(1);
        }
        if (profile.getProfileVisible() == null) {
            profile.setProfileVisible(1);
        }
        if (profile.getId() == null) {
            profile.setCreateTime(certifiedTime);
            profile.setUpdateTime(certifiedTime);
            profileMapper.insert(profile);
        } else {
            profile.setUpdateTime(certifiedTime);
            profileMapper.updateById(profile);
        }
        return profile;
    }

    private void updateProfileRejected(BasketPlayerCertApplication app, Date now) {
        BasketPlayerProfile profile = profileMapper.selectByMemberId(app.getMemberId());
        if (profile == null) {
            return;
        }
        profile.setCertificationStatus(CERT_REJECTED);
        profile.setUpdateTime(now);
        profileMapper.updateById(profile);
    }

    private BasketPlayerPlatformAccount insertPrimaryPlatformAccountIfAbsent(BasketPlayerCertApplication app,
            BasketPlayerProfile profile, String normalizedGameAccount, Long auditorId, Date now) {
        BasketPlayerPlatformAccount existing = platformAccountMapper.selectPrimaryByPlayerId(profile.getId());
        if (existing != null) {
            return null;
        }
        BasketPlayerPlatformAccount account = new BasketPlayerPlatformAccount();
        account.setPlayerId(profile.getId());
        account.setPlatform(normalizePlatformCode(app.getGameTerminal()));
        account.setDisplayGameId(app.getGameAccount());
        account.setNormalizedGameId(normalizedGameAccount);
        account.setAccountStatus(PlatformAccountStatusEnum.ACTIVE.getCode());
        account.setIsPrimary(PRIMARY);
        account.setSourceApplicationId(app.getId());
        account.setApprovedBy(auditorId);
        account.setApprovedTime(now);
        account.setEffectiveTime(now);
        account.setCreateTime(now);
        account.setUpdateTime(now);
        account.setIsDelete(NOT_DELETED);
        platformAccountMapper.insert(account);
        return account;
    }

    private String normalizePlatformCode(String platform) {
        if (!StringUtils.hasText(platform)) {
            return platform;
        }
        String normalized = platform.trim().toUpperCase();
        if ("PS5".equals(normalized) || "PSN".equals(normalized)) {
            return "PS";
        }
        return normalized;
    }

    private void writeAudit(BasketPlayerCertApplication app, String action, Integer before, Integer after, String remark) {
        Date now = new Date();
        BasketPlayerCertAudit audit = new BasketPlayerCertAudit();
        audit.setApplicationId(app.getId());
        audit.setAuditAction(action);
        audit.setBeforeStatus(before);
        audit.setAfterStatus(after);
        audit.setAuditorId(SecurityUtils.getUserId());
        audit.setAuditorName(SecurityUtils.getUsername());
        audit.setAuditRemark(remark);
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        auditMapper.insert(audit);
    }

    private BasketPlayerCertApplication requireApplication(Long id) {
        BasketPlayerCertApplication app = applicationMapper.selectById(id);
        if (app == null) {
            throw new ServiceException("认证申请不存在");
        }
        return app;
    }

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}
