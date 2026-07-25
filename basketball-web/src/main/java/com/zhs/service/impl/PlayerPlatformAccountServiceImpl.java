package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerPlatformChangeRequestDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.PlatformAccountStatusEnum;
import com.zhs.enums.PlatformChangeStatusEnum;
import com.zhs.enums.PlatformChangeTypeEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.PlayerStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerPlatformChangeRequest;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.player.PlatformAccountItemResp;
import com.zhs.response.player.PlatformChangeHistoryItemResp;
import com.zhs.response.player.PlatformChangeSubmitResp;
import com.zhs.response.player.PlayerEligibilityStatusResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.IPlayerPlatformAccountService;
import com.zhs.util.GameAccountNormalizer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerPlatformAccountServiceImpl implements IPlayerPlatformAccountService {

    private static final Integer NOT_DELETED = 0;
    private static final int PRIMARY = 1;
    private static final List<String> STANDARD_PLATFORMS = List.of("PC", "PS", "XBOX");

    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketPlayerPlatformChangeRequestDao platformChangeRequestDao;
    @Resource
    private IAccountFileService accountFileService;

    @Override
    public List<PlatformAccountItemResp> listPlatformAccounts(Long memberId) {
        BasketPlayerProfile profile = requireApprovedPlayer(memberId);
        List<BasketPlayerPlatformAccount> accounts = platformAccountDao.selectList(
                new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                        .eq(BasketPlayerPlatformAccount::getPlayerId, profile.getId())
                        .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketPlayerPlatformAccount::getIsPrimary)
                        .orderByAsc(BasketPlayerPlatformAccount::getPlatform));

        Map<String, BasketPlayerPlatformAccount> accountByPlatform = accounts.stream()
                .collect(Collectors.toMap(
                        account -> normalizePlatform(account.getPlatform()),
                        account -> account,
                        (a, b) -> a,
                        LinkedHashMap::new));

        Set<String> pendingPlatforms = loadPendingChangePlatforms(profile.getId());

        List<PlatformAccountItemResp> result = new ArrayList<>(STANDARD_PLATFORMS.size());
        for (String platform : STANDARD_PLATFORMS) {
            BasketPlayerPlatformAccount account = accountByPlatform.get(platform);
            PlatformAccountItemResp item = new PlatformAccountItemResp();
            item.setPlatform(platform);
            item.setPendingChange(pendingPlatforms.contains(platform));
            if (account != null) {
                PlatformAccountStatusEnum status = PlatformAccountStatusEnum.fromCode(account.getAccountStatus());
                item.setPlatformAccountId(String.valueOf(account.getId()));
                item.setDisplayGameId(account.getDisplayGameId());
                item.setAccountStatus(status.getCode());
                item.setAccountStatusText(status.getLabel());
                item.setPrimary(Integer.valueOf(PRIMARY).equals(account.getIsPrimary()));
            } else {
                item.setAccountStatus(null);
                item.setAccountStatusText("未绑定");
                item.setPrimary(false);
            }
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformChangeSubmitResp submitAddPlatform(Long memberId, String platform, String gameId, Long proofFileId) {
        BasketPlayerProfile profile = requireApprovedPlayer(memberId);
        String normalizedPlatform = requirePlatform(platform);
        String displayGameId = GameAccountNormalizer.display(gameId);
        String normalizedGameId = requireNormalizedGameId(displayGameId);

        assertNoPendingChange(profile.getId(), normalizedPlatform);
        assertPlatformNotBound(profile.getId(), normalizedPlatform);
        assertGameIdAvailable(normalizedPlatform, normalizedGameId, profile.getId());
        requireProofFile(proofFileId, memberId);

        Date now = new Date();
        BasketPlayerPlatformChangeRequest request = new BasketPlayerPlatformChangeRequest();
        request.setRequestNo(generateRequestNo());
        request.setPlayerId(profile.getId());
        request.setMemberId(memberId);
        request.setChangeType(PlatformChangeTypeEnum.ADD_PLATFORM.getCode());
        request.setPlatform(normalizedPlatform);
        request.setNewDisplayGameId(displayGameId);
        request.setNewNormalizedGameId(normalizedGameId);
        request.setProofFileId(proofFileId);
        request.setRequestStatus(PlatformChangeStatusEnum.PENDING.getCode());
        request.setSubmitTime(now);
        request.setIsDelete(NOT_DELETED);
        platformChangeRequestDao.insert(request);

        PlatformChangeSubmitResp resp = new PlatformChangeSubmitResp();
        resp.setRequestNo(request.getRequestNo());
        resp.setRequestId(String.valueOf(request.getId()));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformChangeSubmitResp submitChangeGameId(Long memberId, Long platformAccountId,
                                                       String newGameId, Long proofFileId) {
        BasketPlayerProfile profile = requireApprovedPlayer(memberId);
        BasketPlayerPlatformAccount account = requireOwnedPlatformAccount(platformAccountId, profile.getId());

        String normalizedPlatform = normalizePlatform(account.getPlatform());
        String displayGameId = GameAccountNormalizer.display(newGameId);
        String normalizedGameId = requireNormalizedGameId(displayGameId);

        if (normalizedGameId.equals(account.getNormalizedGameId())) {
            throw new ServiceException("新游戏 ID 与当前一致");
        }

        assertNoPendingChange(profile.getId(), normalizedPlatform);
        assertGameIdAvailable(normalizedPlatform, normalizedGameId, profile.getId());
        requireProofFile(proofFileId, memberId);

        if (!PlatformAccountStatusEnum.ACTIVE.getCode().equals(account.getAccountStatus())) {
            throw new ServiceException("当前平台账号不可修改");
        }

        Date now = new Date();
        BasketPlayerPlatformChangeRequest request = new BasketPlayerPlatformChangeRequest();
        request.setRequestNo(generateRequestNo());
        request.setPlayerId(profile.getId());
        request.setMemberId(memberId);
        request.setChangeType(PlatformChangeTypeEnum.CHANGE_GAME_ID.getCode());
        request.setPlatform(normalizedPlatform);
        request.setPlatformAccountId(account.getId());
        request.setOldDisplayGameId(account.getDisplayGameId());
        request.setNewDisplayGameId(displayGameId);
        request.setNewNormalizedGameId(normalizedGameId);
        request.setProofFileId(proofFileId);
        request.setRequestStatus(PlatformChangeStatusEnum.PENDING.getCode());
        request.setSubmitTime(now);
        request.setIsDelete(NOT_DELETED);
        platformChangeRequestDao.insert(request);

        PlatformChangeSubmitResp resp = new PlatformChangeSubmitResp();
        resp.setRequestNo(request.getRequestNo());
        resp.setRequestId(String.valueOf(request.getId()));
        return resp;
    }

    @Override
    public List<PlatformChangeHistoryItemResp> listChangeHistory(Long memberId) {
        BasketPlayerProfile profile = requireApprovedPlayer(memberId);
        List<BasketPlayerPlatformChangeRequest> requests = platformChangeRequestDao.selectList(
                new LambdaQueryWrapper<BasketPlayerPlatformChangeRequest>()
                        .eq(BasketPlayerPlatformChangeRequest::getPlayerId, profile.getId())
                        .eq(BasketPlayerPlatformChangeRequest::getIsDelete, NOT_DELETED)
                        .in(BasketPlayerPlatformChangeRequest::getRequestStatus,
                                PlatformChangeStatusEnum.APPROVED.getCode(),
                                PlatformChangeStatusEnum.REJECTED.getCode())
                        .orderByDesc(BasketPlayerPlatformChangeRequest::getReviewedTime)
                        .orderByDesc(BasketPlayerPlatformChangeRequest::getSubmitTime));

        return requests.stream()
                .sorted(Comparator.comparing(BasketPlayerPlatformChangeRequest::getReviewedTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BasketPlayerPlatformChangeRequest::getSubmitTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toHistoryItem)
                .collect(Collectors.toList());
    }

    @Override
    public PlayerEligibilityStatusResp getEligibilityStatus(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("尚未完成球员认证");
        }

        PlayerStatusEnum playerStatus = PlayerStatusEnum.fromCode(profile.getPlayerStatus());
        PlayerEligibilityStatusResp resp = new PlayerEligibilityStatusResp();
        resp.setPlayerNo(profile.getPlayerNo());
        resp.setPlayerStatus(playerStatus.getCode());
        resp.setPlayerStatusText(playerStatus.getLabel());
        resp.setEligible(!playerStatus.isRestricted());
        if (playerStatus.isRestricted()) {
            resp.setNotice("您的参赛资格受限，当前不能加入赛事球队或取得本届赛事参赛资格。");
        } else {
            resp.setNotice(null);
        }
        return resp;
    }

    /**
     * 校验平台 + 规范化游戏 ID 是否已被占用（生效账号或待审核变更）。
     */
    @Override
    public void assertGameIdAvailable(String platform, String normalizedGameId, Long excludePlayerId) {
        Long activeCount = platformAccountDao.selectCount(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlatform, platform)
                .eq(BasketPlayerPlatformAccount::getNormalizedGameId, normalizedGameId)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerPlatformAccount::getAccountStatus, PlatformAccountStatusEnum.ACTIVE.getCode())
                .ne(excludePlayerId != null, BasketPlayerPlatformAccount::getPlayerId, excludePlayerId));
        if (activeCount != null && activeCount > 0) {
            throw new ServiceException("该游戏 ID 已被其他球员使用");
        }

        Long pendingCount = platformChangeRequestDao.selectCount(new LambdaQueryWrapper<BasketPlayerPlatformChangeRequest>()
                .eq(BasketPlayerPlatformChangeRequest::getPlatform, platform)
                .eq(BasketPlayerPlatformChangeRequest::getNewNormalizedGameId, normalizedGameId)
                .eq(BasketPlayerPlatformChangeRequest::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerPlatformChangeRequest::getRequestStatus, PlatformChangeStatusEnum.PENDING.getCode())
                .ne(excludePlayerId != null, BasketPlayerPlatformChangeRequest::getPlayerId, excludePlayerId));
        if (pendingCount != null && pendingCount > 0) {
            throw new ServiceException("该游戏 ID 已有待审核申请");
        }
    }

    private BasketPlayerProfile requireApprovedPlayer(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("尚未完成球员认证");
        }
        if (!Integer.valueOf(PlayerCertStatusEnum.APPROVED.getCode()).equals(profile.getCertificationStatus())) {
            throw new ServiceException("球员认证尚未通过，无法管理平台游戏 ID");
        }
        return profile;
    }

    private BasketPlayerPlatformAccount requireOwnedPlatformAccount(Long platformAccountId, Long playerId) {
        BasketPlayerPlatformAccount account = platformAccountDao.selectById(platformAccountId);
        if (account == null || !NOT_DELETED.equals(account.getIsDelete())) {
            throw new ServiceException("平台账号不存在");
        }
        if (!playerId.equals(account.getPlayerId())) {
            throw new ServiceException("无权操作该平台账号", 403);
        }
        return account;
    }

    private void assertNoPendingChange(Long playerId, String platform) {
        Long count = platformChangeRequestDao.selectCount(new LambdaQueryWrapper<BasketPlayerPlatformChangeRequest>()
                .eq(BasketPlayerPlatformChangeRequest::getPlayerId, playerId)
                .eq(BasketPlayerPlatformChangeRequest::getPlatform, platform)
                .eq(BasketPlayerPlatformChangeRequest::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerPlatformChangeRequest::getRequestStatus, PlatformChangeStatusEnum.PENDING.getCode()));
        if (count != null && count > 0) {
            throw new ServiceException("该平台已有待审核变更申请");
        }
    }

    private void assertPlatformNotBound(Long playerId, String platform) {
        Long count = platformAccountDao.selectCount(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, playerId)
                .eq(BasketPlayerPlatformAccount::getPlatform, platform)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                .in(BasketPlayerPlatformAccount::getAccountStatus,
                        PlatformAccountStatusEnum.ACTIVE.getCode(),
                        PlatformAccountStatusEnum.PENDING.getCode()));
        if (count != null && count > 0) {
            throw new ServiceException("该平台已绑定游戏 ID");
        }
    }

    private void requireProofFile(Long proofFileId, Long memberId) {
        if (proofFileId == null) {
            throw new ServiceException("请上传证明材料");
        }
        accountFileService.requireOwnedFile(proofFileId, memberId);
    }

    private Set<String> loadPendingChangePlatforms(Long playerId) {
        return platformChangeRequestDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformChangeRequest>()
                        .eq(BasketPlayerPlatformChangeRequest::getPlayerId, playerId)
                        .eq(BasketPlayerPlatformChangeRequest::getIsDelete, NOT_DELETED)
                        .eq(BasketPlayerPlatformChangeRequest::getRequestStatus, PlatformChangeStatusEnum.PENDING.getCode()))
                .stream()
                .map(request -> normalizePlatform(request.getPlatform()))
                .collect(Collectors.toSet());
    }

    private PlatformChangeHistoryItemResp toHistoryItem(BasketPlayerPlatformChangeRequest request) {
        PlatformChangeHistoryItemResp resp = new PlatformChangeHistoryItemResp();
        PlatformChangeStatusEnum status = PlatformChangeStatusEnum.fromCode(request.getRequestStatus());
        resp.setRequestNo(request.getRequestNo());
        resp.setChangeType(request.getChangeType());
        resp.setPlatform(normalizePlatform(request.getPlatform()));
        resp.setOldDisplayGameId(request.getOldDisplayGameId());
        resp.setNewDisplayGameId(request.getNewDisplayGameId());
        resp.setRequestStatus(status.getCode());
        resp.setRequestStatusText(status.getLabel());
        resp.setSubmitTime(request.getSubmitTime());
        resp.setReviewedTime(request.getReviewedTime());
        return resp;
    }

    private String requirePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new ServiceException("请选择游戏平台");
        }
        String normalized = normalizePlatform(platform);
        if (!STANDARD_PLATFORMS.contains(normalized)) {
            throw new ServiceException("不支持的游戏平台");
        }
        return normalized;
    }

    private String requireNormalizedGameId(String displayGameId) {
        if (!StringUtils.hasText(displayGameId)) {
            throw new ServiceException("请填写游戏 ID");
        }
        String normalized = GameAccountNormalizer.normalize(displayGameId);
        if (!StringUtils.hasText(normalized)) {
            throw new ServiceException("请填写有效的游戏 ID");
        }
        return normalized;
    }

    private String normalizePlatform(String platform) {
        String value = platform.trim().toUpperCase();
        if ("PS5".equals(value) || "PSN".equals(value)) {
            return "PS";
        }
        return value;
    }

    private String generateRequestNo() {
        return "PCR" + IdUtil.getSnowflakeNextIdStr();
    }
}
