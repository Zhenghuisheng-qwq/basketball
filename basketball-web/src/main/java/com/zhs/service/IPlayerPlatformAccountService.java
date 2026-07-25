package com.zhs.service;

import com.zhs.response.player.PlatformAccountItemResp;
import com.zhs.response.player.PlatformChangeHistoryItemResp;
import com.zhs.response.player.PlatformChangeSubmitResp;
import com.zhs.response.player.PlayerEligibilityStatusResp;

import java.util.List;

/**
 * 球员平台游戏 ID 业务服务
 */
public interface IPlayerPlatformAccountService {

    List<PlatformAccountItemResp> listPlatformAccounts(Long memberId);

    PlatformChangeSubmitResp submitAddPlatform(Long memberId, String platform, String gameId, Long proofFileId);

    PlatformChangeSubmitResp submitChangeGameId(Long memberId, Long platformAccountId, String newGameId, Long proofFileId);

    List<PlatformChangeHistoryItemResp> listChangeHistory(Long memberId);

    PlayerEligibilityStatusResp getEligibilityStatus(Long memberId);

    void assertGameIdAvailable(String platform, String normalizedGameId, Long excludePlayerId);
}
