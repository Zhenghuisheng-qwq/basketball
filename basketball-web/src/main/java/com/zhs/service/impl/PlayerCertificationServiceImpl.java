package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMessageReceiverDao;
import com.zhs.dao.BasketMessageTaskDao;
import com.zhs.dao.BasketPlayerCertApplicationDao;
import com.zhs.dao.BasketPlayerCertMaterialDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.CertMaterialTypeEnum;
import com.zhs.enums.MessageReadStatusEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMessageReceiver;
import com.zhs.model.BasketMessageTask;
import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.model.BasketPlayerCertMaterial;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.request.player.PlayerCertDraftReq;
import com.zhs.request.player.PlayerCertMaterialBindReq;
import com.zhs.request.player.PlayerCertSubmitReq;
import com.zhs.response.player.PlayerCertCurrentResp;
import com.zhs.response.player.PlayerCertMaterialResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.IPlayerCertificationService;
import com.zhs.service.crypto.IFieldEncryptService;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlayerCertificationServiceImpl implements IPlayerCertificationService {

    private static final Integer NOT_DELETED = 0;
    private static final int DELETED = 1;
    private static final int MATERIAL_STATUS_ACTIVE = 1;
    private static final int SEND_STATUS_SENT = 1;
    private static final int DELIVERY_STATUS_SENT = 1;
    private static final String APPLICATION_TYPE_INITIAL = "INITIAL";
    private static final String OPERATOR_MEMBER = "MEMBER";
    private static final String EVENT_CERT_SUBMIT = "CERT_SUBMIT";
    private static final String MESSAGE_TYPE_SITE = "SITE";
    private static final String BIZ_TYPE_PLAYER_CERT = "PLAYER_CERT";
    private static final String SEND_SCOPE_SINGLE = "SINGLE";
    private static final String DELIVERY_CHANNEL_SITE = "SITE";

    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerCertApplicationDao certApplicationDao;
    @Resource
    private BasketPlayerCertMaterialDao certMaterialDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private BasketAccountSecurityLogDao securityLogDao;
    @Resource
    private BasketMessageTaskDao messageTaskDao;
    @Resource
    private BasketMessageReceiverDao messageReceiverDao;
    @Resource
    private IFieldEncryptService fieldEncryptService;
    @Resource
    private IAccountFileService accountFileService;

    @Override
    public PlayerCertCurrentResp getCurrent() {
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile profile = loadProfile(memberId);
        BasketPlayerCertApplication application = loadLatestNonCancelledApplication(memberId);
        List<BasketPlayerCertMaterial> materials = application == null
                ? List.of()
                : loadActiveMaterials(application.getId());
        return buildCurrentResp(profile, application, materials);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerCertCurrentResp saveDraft(PlayerCertDraftReq req) {
        validateDraftReq(req);

        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile profile = loadProfile(memberId);
        assertNoReviewingApplication(memberId);

        BasketPlayerCertApplication application = loadEditableApplication(memberId);
        if (application == null) {
            assertCanCreateDraft(memberId);
            application = createDraftApplication(memberId, profile);
        }

        applyDraftFields(application, req);
        certApplicationDao.updateById(application);
        return getCurrent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerCertMaterialResp bindMaterial(PlayerCertMaterialBindReq req) {
        if (req == null) {
            throw new ServiceException("????????");
        }

        Long memberId = UserContext.getRequiredUserId();
        Long applicationId = parseLongId(req.getApplicationId(), "??????");
        Long fileId = parseLongId(req.getFileId(), "????");
        CertMaterialTypeEnum materialType = CertMaterialTypeEnum.fromCode(req.getMaterialType());

        BasketPlayerCertApplication application = requireOwnedApplication(applicationId, memberId);
        assertApplicationEditable(application);

        BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
        if (!materialType.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("????????????");
        }
        assertNoDuplicateFile(applicationId, fileId);

        BasketPlayerCertMaterial material = new BasketPlayerCertMaterial();
        material.setApplicationId(applicationId);
        material.setMaterialType(materialType.getCode());
        material.setFileId(fileId);
        material.setMaterialName(resolveMaterialName(asset));
        material.setSortNo(0);
        material.setMaterialStatus(MATERIAL_STATUS_ACTIVE);
        material.setIsDelete(NOT_DELETED);
        certMaterialDao.insert(material);

        return toMaterialResp(material, asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMaterial(String materialId) {
        Long memberId = UserContext.getRequiredUserId();
        Long materialPk = parseLongId(materialId, "????");

        BasketPlayerCertMaterial material = requireActiveMaterial(materialPk);
        BasketPlayerCertApplication application = requireOwnedApplication(material.getApplicationId(), memberId);
        assertMaterialDeletable(application);

        LambdaUpdateWrapper<BasketPlayerCertMaterial> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BasketPlayerCertMaterial::getId, materialPk)
                .set(BasketPlayerCertMaterial::getIsDelete, DELETED);
        certMaterialDao.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerCertCurrentResp submit(PlayerCertSubmitReq req, String clientIp) {
        if (req == null || !StringUtils.hasText(req.getApplicationId())) {
            throw new ServiceException("??????????");
        }

        Long memberId = UserContext.getRequiredUserId();
        Long applicationId = parseLongId(req.getApplicationId(), "??????");
        BasketPlayerCertApplication application = requireOwnedApplication(applicationId, memberId);

        PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(application.getApplicationStatus());
        if (!status.canSubmit()) {
            throw new ServiceException("??????????");
        }

        validateSubmitReady(application);
        List<BasketPlayerCertMaterial> materials = loadActiveMaterials(applicationId);
        validateSubmitMaterials(application, materials);

        Date now = new Date();
        LambdaUpdateWrapper<BasketPlayerCertApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BasketPlayerCertApplication::getId, applicationId)
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .in(BasketPlayerCertApplication::getApplicationStatus,
                        PlayerCertStatusEnum.DRAFT.getCode(),
                        PlayerCertStatusEnum.REJECTED.getCode())
                .set(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.SUBMITTED.getCode())
                .set(BasketPlayerCertApplication::getSubmittedTime, now)
                .set(BasketPlayerCertApplication::getReviewedTime, null)
                .set(BasketPlayerCertApplication::getReviewRemark, null)
                .set(BasketPlayerCertApplication::getCurrentAuditorId, null);
        int updated = certApplicationDao.update(null, updateWrapper);
        if (updated == 0) {
            throw new ServiceException("????????????");
        }

        writeSecurityLog(memberId, EVENT_CERT_SUBMIT, "???????",
                null, application.getApplicationNo(), OPERATOR_MEMBER, memberId, clientIp, now);
        createSubmitMessage(memberId, applicationId, now);

        return getCurrent();
    }

    private BasketPlayerProfile loadProfile(Long memberId) {
        return playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketPlayerCertApplication loadLatestNonCancelledApplication(Long memberId) {
        return certApplicationDao.selectOne(new LambdaQueryWrapper<BasketPlayerCertApplication>()
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .ne(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.CANCELLED.getCode())
                .orderByDesc(BasketPlayerCertApplication::getCreateTime)
                .last("limit 1"));
    }

    private BasketPlayerCertApplication loadEditableApplication(Long memberId) {
        return certApplicationDao.selectOne(new LambdaQueryWrapper<BasketPlayerCertApplication>()
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .in(BasketPlayerCertApplication::getApplicationStatus,
                        PlayerCertStatusEnum.DRAFT.getCode(),
                        PlayerCertStatusEnum.REJECTED.getCode())
                .orderByDesc(BasketPlayerCertApplication::getCreateTime)
                .last("limit 1"));
    }

    private void assertNoReviewingApplication(Long memberId) {
        Long reviewingCount = certApplicationDao.selectCount(new LambdaQueryWrapper<BasketPlayerCertApplication>()
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .in(BasketPlayerCertApplication::getApplicationStatus,
                        PlayerCertStatusEnum.SUBMITTED.getCode(),
                        PlayerCertStatusEnum.REVIEWING.getCode()));
        if (reviewingCount != null && reviewingCount > 0) {
            throw new ServiceException("???????????????");
        }
    }

    private void assertCanCreateDraft(Long memberId) {
        BasketPlayerCertApplication latest = loadLatestNonCancelledApplication(memberId);
        if (latest == null) {
            return;
        }
        PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(latest.getApplicationStatus());
        if (status == PlayerCertStatusEnum.APPROVED) {
            throw new ServiceException("??????????????");
        }
        if (status.isReviewing()) {
            throw new ServiceException("?????????????????");
        }
    }

    private BasketPlayerCertApplication createDraftApplication(Long memberId, BasketPlayerProfile profile) {
        BasketPlayerCertApplication application = new BasketPlayerCertApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setMemberId(memberId);
        if (profile != null) {
            application.setPlayerId(profile.getId());
        }
        application.setApplicationType(APPLICATION_TYPE_INITIAL);
        application.setApplicationStatus(PlayerCertStatusEnum.DRAFT.getCode());
        application.setIsDelete(NOT_DELETED);
        certApplicationDao.insert(application);
        return application;
    }

    private void applyDraftFields(BasketPlayerCertApplication application, PlayerCertDraftReq req) {
        String idNoPlain = req.getIdNo().trim();
        application.setRealName(req.getRealName().trim());
        application.setIdType(req.getIdType().trim());
        application.setIdNoCipher(fieldEncryptService.encrypt(idNoPlain));
        application.setIdNoMasked(DataMaskUtil.maskIdCard(idNoPlain));
        application.setGameTerminal(req.getGameTerminal().trim());
        application.setGameAccount(req.getGameAccount().trim());
        application.setGameNickname(req.getGameNickname().trim());
        application.setPositionCode(trimToNull(req.getPositionCode()));
        application.setContactMobile(req.getContactMobile().trim());

        PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(application.getApplicationStatus());
        if (status == PlayerCertStatusEnum.REJECTED) {
            application.setApplicationStatus(PlayerCertStatusEnum.DRAFT.getCode());
            application.setReviewedTime(null);
            application.setReviewRemark(null);
            application.setCurrentAuditorId(null);
        }
    }

    private void validateDraftReq(PlayerCertDraftReq req) {
        if (req == null) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getRealName())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getIdType())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getIdNo())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getGameTerminal())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getGameAccount())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getGameNickname())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(req.getContactMobile())) {
            throw new ServiceException("????????");
        }
    }

    private void validateSubmitReady(BasketPlayerCertApplication application) {
        if (!StringUtils.hasText(application.getRealName())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(application.getIdType())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(application.getIdNoCipher())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(application.getGameTerminal())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(application.getGameAccount())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(application.getGameNickname())) {
            throw new ServiceException("????????");
        }
        if (!StringUtils.hasText(application.getContactMobile())) {
            throw new ServiceException("????????");
        }
    }

    private void validateSubmitMaterials(BasketPlayerCertApplication application,
                                         List<BasketPlayerCertMaterial> materials) {
        Map<String, Long> typeCounts = materials.stream()
                .collect(Collectors.groupingBy(
                        material -> material.getMaterialType().toUpperCase(Locale.ROOT),
                        Collectors.counting()));

        if (!typeCounts.containsKey(CertMaterialTypeEnum.ID_FRONT.getCode())) {
            throw new ServiceException("?????????");
        }
        if (requiresIdBack(application.getIdType())
                && !typeCounts.containsKey(CertMaterialTypeEnum.ID_BACK.getCode())) {
            throw new ServiceException("?????????");
        }
        if (!typeCounts.containsKey(CertMaterialTypeEnum.GAME_SCREENSHOT.getCode())) {
            throw new ServiceException("???????????");
        }
    }

    private boolean requiresIdBack(String idType) {
        if (!StringUtils.hasText(idType)) {
            return false;
        }
        String normalized = idType.trim().toUpperCase(Locale.ROOT);
        return "ID_CARD".equals(normalized) || "RESIDENT_ID".equals(normalized);
    }

    private List<BasketPlayerCertMaterial> loadActiveMaterials(Long applicationId) {
        return certMaterialDao.selectList(new LambdaQueryWrapper<BasketPlayerCertMaterial>()
                .eq(BasketPlayerCertMaterial::getApplicationId, applicationId)
                .eq(BasketPlayerCertMaterial::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerCertMaterial::getMaterialStatus, MATERIAL_STATUS_ACTIVE)
                .orderByAsc(BasketPlayerCertMaterial::getSortNo)
                .orderByAsc(BasketPlayerCertMaterial::getCreateTime));
    }

    private BasketPlayerCertApplication requireOwnedApplication(Long applicationId, Long memberId) {
        BasketPlayerCertApplication application = certApplicationDao.selectById(applicationId);
        if (application == null || !Integer.valueOf(NOT_DELETED).equals(application.getIsDelete())) {
            throw new ServiceException("???????");
        }
        if (!memberId.equals(application.getMemberId())) {
            throw new ServiceException("?????????", 403);
        }
        return application;
    }

    private void assertApplicationEditable(BasketPlayerCertApplication application) {
        if (!PlayerCertStatusEnum.fromCode(application.getApplicationStatus()).canEdit()) {
            throw new ServiceException("??????????");
        }
    }

    private void assertMaterialDeletable(BasketPlayerCertApplication application) {
        if (!PlayerCertStatusEnum.fromCode(application.getApplicationStatus()).canDeleteMaterial()) {
            throw new ServiceException("????????????");
        }
    }

    private BasketPlayerCertMaterial requireActiveMaterial(Long materialId) {
        BasketPlayerCertMaterial material = certMaterialDao.selectById(materialId);
        if (material == null || !Integer.valueOf(NOT_DELETED).equals(material.getIsDelete())) {
            throw new ServiceException("???????");
        }
        return material;
    }

    private void assertNoDuplicateFile(Long applicationId, Long fileId) {
        Long count = certMaterialDao.selectCount(new LambdaQueryWrapper<BasketPlayerCertMaterial>()
                .eq(BasketPlayerCertMaterial::getApplicationId, applicationId)
                .eq(BasketPlayerCertMaterial::getFileId, fileId)
                .eq(BasketPlayerCertMaterial::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("???????????");
        }
    }

    private PlayerCertCurrentResp buildCurrentResp(BasketPlayerProfile profile,
                                                    BasketPlayerCertApplication application,
                                                    List<BasketPlayerCertMaterial> materials) {
        PlayerCertCurrentResp resp = new PlayerCertCurrentResp();
        resp.setHasPlayerProfile(profile != null);
        if (profile != null) {
            resp.setPlayerId(toStringId(profile.getId()));
            resp.setPlayerNo(profile.getPlayerNo());
            resp.setCertificationStatus(profile.getCertificationStatus());
        }

        if (application != null) {
            PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(application.getApplicationStatus());
            resp.setCurrentApplicationId(toStringId(application.getId()));
            resp.setApplicationNo(application.getApplicationNo());
            resp.setApplicationStatus(application.getApplicationStatus());
            resp.setApplicationStatusText(status.getDescription());
            resp.setSubmittedTime(application.getSubmittedTime());
            resp.setReviewedTime(application.getReviewedTime());
            resp.setRejectReason(application.getReviewRemark());
            resp.setRealName(application.getRealName());
            resp.setIdType(application.getIdType());
            resp.setIdNoMasked(application.getIdNoMasked());
            resp.setGameTerminal(application.getGameTerminal());
            resp.setGameAccount(application.getGameAccount());
            resp.setGameNickname(application.getGameNickname());
            resp.setPositionCode(application.getPositionCode());
            resp.setContactMobile(application.getContactMobile());
            resp.setCanEdit(status.canEdit());
            resp.setCanSubmit(status.canSubmit());
        } else {
            resp.setCanEdit(true);
            resp.setCanSubmit(false);
        }

        resp.setMaterials(buildMaterialRespList(materials));
        return resp;
    }

    private List<PlayerCertMaterialResp> buildMaterialRespList(List<BasketPlayerCertMaterial> materials) {
        if (materials == null || materials.isEmpty()) {
            return List.of();
        }

        List<Long> fileIds = materials.stream()
                .map(BasketPlayerCertMaterial::getFileId)
                .distinct()
                .toList();
        Map<Long, BasketFileAsset> fileMap = fileAssetDao.selectBatchIds(fileIds).stream()
                .filter(asset -> Integer.valueOf(NOT_DELETED).equals(asset.getIsDelete()))
                .collect(Collectors.toMap(BasketFileAsset::getId, asset -> asset, (a, b) -> a));

        List<PlayerCertMaterialResp> result = new ArrayList<>(materials.size());
        for (BasketPlayerCertMaterial material : materials) {
            result.add(toMaterialResp(material, fileMap.get(material.getFileId())));
        }
        return result;
    }

    private PlayerCertMaterialResp toMaterialResp(BasketPlayerCertMaterial material, BasketFileAsset asset) {
        PlayerCertMaterialResp resp = new PlayerCertMaterialResp();
        resp.setMaterialId(toStringId(material.getId()));
        resp.setMaterialType(material.getMaterialType());
        resp.setFileId(toStringId(material.getFileId()));
        resp.setMaterialName(material.getMaterialName());
        if (asset != null) {
            resp.setAccessUrl(asset.getAccessUrl());
            if (!StringUtils.hasText(resp.getMaterialName())) {
                resp.setMaterialName(resolveMaterialName(asset));
            }
        }
        return resp;
    }

    private String resolveMaterialName(BasketFileAsset asset) {
        if (StringUtils.hasText(asset.getOriginalName())) {
            return asset.getOriginalName();
        }
        return asset.getFileName();
    }

    private void writeSecurityLog(Long memberId, String eventType, String title,
                                  String oldMasked, String newMasked, String operatorType,
                                  Long operatorId, String clientIp, Date eventTime) {
        BasketAccountSecurityLog logEntity = new BasketAccountSecurityLog();
        logEntity.setMemberId(memberId);
        logEntity.setEventType(eventType);
        logEntity.setEventTitle(title);
        logEntity.setOldValueMasked(oldMasked);
        logEntity.setNewValueMasked(newMasked);
        logEntity.setOperatorType(operatorType);
        logEntity.setOperatorId(operatorId);
        logEntity.setClientIp(clientIp);
        logEntity.setEventTime(eventTime);
        logEntity.setIsDelete(NOT_DELETED);
        securityLogDao.insert(logEntity);
    }

    private void createSubmitMessage(Long memberId, Long applicationId, Date now) {
        BasketMessageTask task = new BasketMessageTask();
        task.setTaskNo("MT" + IdUtil.getSnowflakeNextIdStr());
        task.setMessageType(MESSAGE_TYPE_SITE);
        task.setTitle("???????");
        task.setContentText("????????????????????");
        task.setBizType(BIZ_TYPE_PLAYER_CERT);
        task.setBizId(applicationId);
        task.setSendScope(SEND_SCOPE_SINGLE);
        task.setSendStatus(SEND_STATUS_SENT);
        task.setSentTime(now);
        task.setTotalCount(1);
        task.setSuccessCount(1);
        task.setFailCount(0);
        task.setCreatorId(memberId);
        task.setIsDelete(NOT_DELETED);
        messageTaskDao.insert(task);

        BasketMessageReceiver receiver = new BasketMessageReceiver();
        receiver.setTaskId(task.getId());
        receiver.setMemberId(memberId);
        receiver.setDeliveryChannel(DELIVERY_CHANNEL_SITE);
        receiver.setDeliveryStatus(DELIVERY_STATUS_SENT);
        receiver.setReadStatus(MessageReadStatusEnum.UNREAD.getCode());
        receiver.setSentTime(now);
        receiver.setIsDelete(NOT_DELETED);
        messageReceiverDao.insert(receiver);
    }

    private String generateApplicationNo() {
        return "CA" + IdUtil.getSnowflakeNextIdStr();
    }

    private Long parseLongId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(fieldName + "????");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "????");
        }
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
