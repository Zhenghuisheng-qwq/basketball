package com.zhs.service;

import com.zhs.request.player.PlayerCertDraftReq;
import com.zhs.request.player.PlayerCertMaterialBindReq;
import com.zhs.request.player.PlayerCertSubmitReq;
import com.zhs.response.player.PlayerCertCurrentResp;
import com.zhs.response.player.PlayerCertMaterialResp;

/**
 * 球员认证业务服务
 */
public interface IPlayerCertificationService {

    PlayerCertCurrentResp getCurrent();

    PlayerCertCurrentResp saveDraft(PlayerCertDraftReq req);

    PlayerCertMaterialResp bindMaterial(PlayerCertMaterialBindReq req);

    void deleteMaterial(String materialId);

    PlayerCertCurrentResp submit(PlayerCertSubmitReq req, String clientIp);
}
