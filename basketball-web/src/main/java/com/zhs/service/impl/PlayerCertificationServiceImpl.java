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
import com.zhs.service.IPlayerPlatformAccountService;
import com.zhs.util.GameAccountNormalizer;
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
    private static final String EVENT_CERT_WITHDRAW = "CERT_WITHDRAW";
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
    private IAccountFileService accountFileService;
    @Resource
    private IPlayerPlatformAccountService playerPlatformAccountService;

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
            BasketPlayerCertApplication rejected = loadLatestRejectedApplication(memberId);
            if (rejected != null) {
                application = createDraftFromRejected(memberId, profile, rejected, req);
                certApplicationDao.insert(application);
                copyMaterials(rejected.getId(), application.getId());
                return getCurrent();
            }
            assertCanCreateDraft(memberId);
            application = createDraftApplication(memberId, profile);
        }

        applyDraftFields(application, req);
        if (application.getId() == null) {
            certApplicationDao.insert(application);
        } else {
            certApplicationDao.updateById(application);
        }
        return getCurrent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerCertMaterialResp bindMaterial(PlayerCertMaterialBindReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }

        Long memberId = UserContext.getRequiredUserId();
        Long applicationId = parseLongId(req.getApplicationId(), "申请编号");
        Long fileId = parseLongId(req.getFileId(), "文件编号");
        CertMaterialTypeEnum materialType = CertMaterialTypeEnum.fromCode(req.getMaterialType());

        BasketPlayerCertApplication application = requireOwnedApplication(applicationId, memberId);
        assertApplicationEditable(application);

        BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
        if (!materialType.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("材料类型与文件用途不匹配");
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
        Long materialPk = parseLongId(materialId, "材料编号");

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
            throw new ServiceException("请提供有效的申请编号");
        }

        Long memberId = UserContext.getRequiredUserId();
        Long applicationId = parseLongId(req.getApplicationId(), "申请编号");
        BasketPlayerCertApplication application = requireOwnedApplication(applicationId, memberId);
        PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(application.getApplicationStatus());

        if (status == PlayerCertStatusEnum.REJECTED) {
            return submitFromRejected(application, memberId, clientIp);
        }
        if (!status.canSubmit()) {
            throw new ServiceException("当前申请状态不可提交");
        }

        validateSubmitReady(application);
        List<BasketPlayerCertMaterial> materials = loadActiveMaterials(applicationId);
        validateSubmitMaterials(materials);
        assertGameIdNotDuplicateForCert(application, loadProfile(memberId));

        Date now = new Date();
        LambdaUpdateWrapper<BasketPlayerCertApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BasketPlayerCertApplication::getId, applicationId)
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.DRAFT.getCode())
                .set(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.SUBMITTED.getCode())
                .set(BasketPlayerCertApplication::getSubmittedTime, now)
                .set(BasketPlayerCertApplication::getReviewedTime, null)
                .set(BasketPlayerCertApplication::getReviewRemark, null)
                .set(BasketPlayerCertApplication::getCurrentAuditorId, null)
                .set(BasketPlayerCertApplication::getNormalizedGameAccount, application.getNormalizedGameAccount())
                .set(BasketPlayerCertApplication::getGameAccount, application.getGameAccount());
        int updated = certApplicationDao.update(null, updateWrapper);
        if (updated == 0) {
            throw new ServiceException("提交失败，请刷新后重试");
        }

        writeSecurityLog(memberId, EVENT_CERT_SUBMIT, "球员认证申请已提交",
                null, application.getApplicationNo(), OPERATOR_MEMBER, memberId, clientIp, now);
        createSubmitMessage(memberId, applicationId, now);
        return getCurrent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerCertCurrentResp withdraw() {
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerCertApplication application = certApplicationDao.selectOne(
                new LambdaQueryWrapper<BasketPlayerCertApplication>()
                        .eq(BasketPlayerCertApplication::getMemberId, memberId)
                        .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                        .in(BasketPlayerCertApplication::getApplicationStatus,
                                PlayerCertStatusEnum.SUBMITTED.getCode(),
                                PlayerCertStatusEnum.REVIEWING.getCode())
                        .orderByDesc(BasketPlayerCertApplication::getCreateTime)
                        .last("limit 1"));
        if (application == null) {
            throw new ServiceException("没有可撤回的申请");
        }

        Date now = new Date();
        LambdaUpdateWrapper<BasketPlayerCertApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BasketPlayerCertApplication::getId, application.getId())
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .in(BasketPlayerCertApplication::getApplicationStatus,
                        PlayerCertStatusEnum.SUBMITTED.getCode(),
                        PlayerCertStatusEnum.REVIEWING.getCode())
                .set(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.CANCELLED.getCode())
                .set(BasketPlayerCertApplication::getReviewedTime, now);
        int updated = certApplicationDao.update(null, updateWrapper);
        if (updated == 0) {
            throw new ServiceException("撤回失败，请刷新后重试");
        }

        writeSecurityLog(memberId, EVENT_CERT_WITHDRAW, "球员认证申请已撤回",
                null, application.getApplicationNo(), OPERATOR_MEMBER, memberId, null, now);
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
                .eq(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.DRAFT.getCode())
                .orderByDesc(BasketPlayerCertApplication::getCreateTime)
                .last("limit 1"));
    }

    private BasketPlayerCertApplication loadLatestRejectedApplication(Long memberId) {
        return certApplicationDao.selectOne(new LambdaQueryWrapper<BasketPlayerCertApplication>()
                .eq(BasketPlayerCertApplication::getMemberId, memberId)
                .eq(BasketPlayerCertApplication::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerCertApplication::getApplicationStatus, PlayerCertStatusEnum.REJECTED.getCode())
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
            throw new ServiceException("已存在审核中的申请，请等待审核结果");
        }
    }

    private void assertCanCreateDraft(Long memberId) {
        BasketPlayerCertApplication latest = loadLatestNonCancelledApplication(memberId);
        if (latest == null) {
            return;
        }
        PlayerCertStatusEnum status = PlayerCertStatusEnum.fromCode(latest.getApplicationStatus());
        if (status == PlayerCertStatusEnum.APPROVED) {
            throw new ServiceException("您已通过球员认证，无需再次申请");
        }
        if (status.isReviewing()) {
            throw new ServiceException("当前有审核中的申请，无法新建草稿");
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
        return application;
    }

    private BasketPlayerCertApplication createDraftFromRejected(Long memberId, BasketPlayerProfile profile,
                                                                  BasketPlayerCertApplication rejected,
                                                                  PlayerCertDraftReq req) {
        BasketPlayerCertApplication application = new BasketPlayerCertApplication();
        application.setApplicationNo(generateApplicationNo());
        application.setMemberId(memberId);
        if (profile != null) {
            application.setPlayerId(profile.getId());
        } else if (rejected.getPlayerId() != null) {
            application.setPlayerId(rejected.getPlayerId());
        }
        application.setApplicationType(rejected.getApplicationType());
        application.setApplicationStatus(PlayerCertStatusEnum.DRAFT.getCode());
        application.setIsDelete(NOT_DELETED);
        applyDraftFields(application, req);
        return application;
    }

    private void applyDraftFields(BasketPlayerCertApplication application, PlayerCertDraftReq req) {
        application.setGameTerminal(normalizeCertPlatform(req.getGameTerminal()));
        application.setGameAccount(GameAccountNormalizer.display(req.getGameAccount()));
        application.setNormalizedGameAccount(GameAccountNormalizer.normalize(req.getGameAccount()));
        application.setPositionCode(req.getPositionCode().trim());
        application.setGameNickname(trimToNull(req.getGameNickname()));
    }

    private void copyMaterials(Long sourceApplicationId, Long targetApplicationId) {
        for (BasketPlayerCertMaterial material : loadActiveMaterials(sourceApplicationId)) {
            BasketPlayerCertMaterial copy = new BasketPlayerCertMaterial();
            copy.setApplicationId(targetApplicationId);
            copy.setMaterialType(material.getMaterialType());
            copy.setFileId(material.getFileId());
            copy.setMaterialName(material.getMaterialName());
            copy.setSortNo(material.getSortNo());
            copy.setMaterialStatus(MATERIAL_STATUS_ACTIVE);
            copy.setIsDelete(NOT_DELETED);
            certMaterialDao.insert(copy);
        }
    }

    private PlayerCertCurrentResp submitFromRejected(BasketPlayerCertApplication rejectedApplication,
                                                     Long memberId, String clientIp) {
        validateSubmitReady(rejectedApplication);
        validateSubmitMaterials(loadActiveMaterials(rejectedApplication.getId()));
        assertGameIdNotDuplicateForCert(rejectedApplication, loadProfile(memberId));

        Date now = new Date();
        BasketPlayerCertApplication newApplication = new BasketPlayerCertApplication();
        newApplication.setApplicationNo(generateApplicationNo());
        newApplication.setMemberId(memberId);
        newApplication.setPlayerId(rejectedApplication.getPlayerId());
        newApplication.setApplicationType(rejectedApplication.getApplicationType());
        newApplication.setGameTerminal(rejectedApplication.getGameTerminal());
        newApplication.setGameAccount(rejectedApplication.getGameAccount());
        newApplication.setNormalizedGameAccount(rejectedApplication.getNormalizedGameAccount());
        newApplication.setGameNickname(rejectedApplication.getGameNickname());
        newApplication.setPositionCode(rejectedApplication.getPositionCode());
        newApplication.setApplicationStatus(PlayerCertStatusEnum.SUBMITTED.getCode());
        newApplication.setSubmittedTime(now);
        newApplication.setIsDelete(NOT_DELETED);
        certApplicationDao.insert(newApplication);
        copyMaterials(rejectedApplication.getId(), newApplication.getId());

        writeSecurityLog(memberId, EVENT_CERT_SUBMIT, "球员认证申请已提交",
                null, newApplication.getApplicationNo(), OPERATOR_MEMBER, memberId, clientIp, now);
        createSubmitMessage(memberId, newApplication.getId(), now);
        return getCurrent();
    }

    private void assertGameIdNotDuplicateForCert(BasketPlayerCertApplication application,
                                                 BasketPlayerProfile profile) {
        String normalized = application.getNormalizedGameAccount();
        if (!StringUtils.hasText(normalized)) {
            normalized = GameAccountNormalizer.normalize(application.getGameAccount());
            application.setNormalizedGameAccount(normalized);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new ServiceException("请填写有效的游戏账号");
        }
        application.setGameAccount(GameAccountNormalizer.display(application.getGameAccount()));
        Long excludePlayerId = profile == null ? null : profile.getId();
        playerPlatformAccountService.assertGameIdAvailable(
                normalizeCertPlatform(application.getGameTerminal()), normalized, excludePlayerId);
    }

    private String normalizeCertPlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new ServiceException("请填写比赛终端");
        }
        String value = platform.trim().toUpperCase();
        if ("PS5".equals(value) || "PSN".equals(value)) {
            return "PS";
        }
        return value;
    }

    private void validateDraftReq(PlayerCertDraftReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (!StringUtils.hasText(req.getGameTerminal())) {
            throw new ServiceException("请填写比赛终端");
        }
        if (!StringUtils.hasText(req.getGameAccount())) {
            throw new ServiceException("请填写游戏账号");
        }
        if (!StringUtils.hasText(req.getPositionCode())) {
            throw new ServiceException("请选择场上位置");
        }
    }

    private void validateSubmitReady(BasketPlayerCertApplication application) {
        if (!StringUtils.hasText(application.getGameTerminal())) {
            throw new ServiceException("请填写比赛终端");
        }
        if (!StringUtils.hasText(application.getGameAccount())) {
            throw new ServiceException("请填写游戏账号");
        }
        if (!StringUtils.hasText(application.getPositionCode())) {
            throw new ServiceException("请选择场上位置");
        }
    }

    private void validateSubmitMaterials(List<BasketPlayerCertMaterial> materials) {
        Map<String, Long> typeCounts = materials.stream()
                .collect(Collectors.groupingBy(
                        material -> material.getMaterialType().toUpperCase(Locale.ROOT),
                        Collectors.counting()));
        if (!typeCounts.containsKey(CertMaterialTypeEnum.GAME_SCREENSHOT.getCode())) {
            throw new ServiceException("请上传能够清晰显示游戏平台与游戏 ID 的证明截图");
        }
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
            throw new ServiceException("申请不存在");
        }
        if (!memberId.equals(application.getMemberId())) {
            throw new ServiceException("无权操作该申请", 403);
        }
        return application;
    }

    private void assertApplicationEditable(BasketPlayerCertApplication application) {
        if (!PlayerCertStatusEnum.fromCode(application.getApplicationStatus()).canEdit()) {
            throw new ServiceException("当前申请状态不可编辑");
        }
    }

    private void assertMaterialDeletable(BasketPlayerCertApplication application) {
        if (!PlayerCertStatusEnum.fromCode(application.getApplicationStatus()).canDeleteMaterial()) {
            throw new ServiceException("当前状态不可删除材料");
        }
    }

    private BasketPlayerCertMaterial requireActiveMaterial(Long materialId) {
        BasketPlayerCertMaterial material = certMaterialDao.selectById(materialId);
        if (material == null || !Integer.valueOf(NOT_DELETED).equals(material.getIsDelete())) {
            throw new ServiceException("材料不存在");
        }
        return material;
    }

    private void assertNoDuplicateFile(Long applicationId, Long fileId) {
        Long count = certMaterialDao.selectCount(new LambdaQueryWrapper<BasketPlayerCertMaterial>()
                .eq(BasketPlayerCertMaterial::getApplicationId, applicationId)
                .eq(BasketPlayerCertMaterial::getFileId, fileId)
                .eq(BasketPlayerCertMaterial::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该文件已绑定到当前申请");
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
            resp.setGameTerminal(application.getGameTerminal());
            resp.setGameAccount(application.getGameAccount());
            resp.setGameNickname(application.getGameNickname());
            resp.setPositionCode(application.getPositionCode());
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
        task.setTitle("球员认证申请已提交");
        task.setContentText("您的球员认证申请已提交，请耐心等待审核。");
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
            throw new ServiceException(fieldName + "无效");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "无效");
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
