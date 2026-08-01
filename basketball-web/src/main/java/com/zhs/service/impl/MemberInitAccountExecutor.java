package com.zhs.service.impl;

import com.zhs.common.UserContext;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.request.player.PlayerCertDraftReq;
import com.zhs.request.player.PlayerCertMaterialBindReq;
import com.zhs.request.player.PlayerCertSubmitReq;
import com.zhs.response.account.FileUploadResp;
import com.zhs.response.member.MemberInitAccountItemResp;
import com.zhs.response.player.PlayerCertCurrentResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.IPlayerCertificationService;
import com.zhs.support.ByteArrayMultipartFile;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class MemberInitAccountExecutor {

    private static final String[] POSITIONS = {"PG", "SG", "SF", "PF", "C"};
    private static final String[] TERMINALS = {"PS", "PC", "XBOX"};

    @Resource
    private MemberInitAccountCreator memberInitAccountCreator;
    @Resource
    private IAccountFileService accountFileService;
    @Resource
    private IPlayerCertificationService playerCertificationService;

    @Transactional(rollbackFor = Exception.class)
    public MemberInitAccountItemResp createAccountAndApplyCert(String email, long emailNo, Long avatarFileId,
                                                               byte[] screenshotBytes, String clientIp, int index) {
        Long memberId = memberInitAccountCreator.createMemberAccount(email, avatarFileId);
        PlayerCertCurrentResp certResp = applyPlayerCert(memberId, emailNo, screenshotBytes, clientIp, index);

        MemberInitAccountItemResp item = new MemberInitAccountItemResp();
        item.setEmail(email);
        item.setMemberId(String.valueOf(memberId));
        item.setApplicationId(certResp.getCurrentApplicationId());
        item.setApplicationStatus(certResp.getApplicationStatus());
        return item;
    }

    private PlayerCertCurrentResp applyPlayerCert(Long memberId, long emailNo, byte[] screenshotBytes,
                                                  String clientIp, int index) {
        UserContext.setUserId(memberId);
        try {
            memberInitAccountCreator.assertCanApplyCert(memberId);

            PlayerCertDraftReq draftReq = new PlayerCertDraftReq();
            draftReq.setGameTerminal(TERMINALS[index % TERMINALS.length]);
            draftReq.setGameAccount("player" + emailNo);
            draftReq.setPositionCode(POSITIONS[index % POSITIONS.length]);
            draftReq.setGameNickname("CNPA用户" + emailNo);
            PlayerCertCurrentResp draftResp = playerCertificationService.saveDraft(draftReq);

            String applicationId = draftResp.getCurrentApplicationId();
            if (!StringUtils.hasText(applicationId)) {
                throw new ServiceException("创建球员认证草稿失败");
            }

            ByteArrayMultipartFile screenshot = new ByteArrayMultipartFile(
                    "file", "cert-screenshot.png", "image/png", screenshotBytes);
            FileUploadResp uploadResp = accountFileService.upload(
                    screenshot, FileUsageTypeEnum.PLAYER_CERT_GAME_SCREENSHOT.getCode());

            PlayerCertMaterialBindReq bindReq = new PlayerCertMaterialBindReq();
            bindReq.setApplicationId(applicationId);
            bindReq.setFileId(uploadResp.getFileId());
            bindReq.setMaterialType(FileUsageTypeEnum.PLAYER_CERT_GAME_SCREENSHOT.getCode());
            playerCertificationService.bindMaterial(bindReq);

            PlayerCertSubmitReq submitReq = new PlayerCertSubmitReq();
            submitReq.setApplicationId(applicationId);
            return playerCertificationService.submit(submitReq, clientIp);
        } finally {
            UserContext.clear();
        }
    }
}
