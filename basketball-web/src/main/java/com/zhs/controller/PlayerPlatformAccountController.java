package com.zhs.controller;

import com.zhs.common.UserContext;
import com.zhs.exception.ServiceException;
import com.zhs.request.player.PlatformAccountAddReq;
import com.zhs.request.player.PlatformAccountChangeGameIdReq;
import com.zhs.response.player.PlatformAccountItemResp;
import com.zhs.response.player.PlatformChangeHistoryItemResp;
import com.zhs.response.player.PlatformChangeSubmitResp;
import com.zhs.response.player.PlayerEligibilityStatusResp;
import com.zhs.service.IPlayerPlatformAccountService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/basket/player/platform-account")
@Api(value = "球员平台游戏 ID")
@Slf4j
public class PlayerPlatformAccountController {

    @Resource
    private IPlayerPlatformAccountService playerPlatformAccountService;

    @ApiOperation("平台游戏 ID 列表")
    @GetMapping("/list")
    public R<List<PlatformAccountItemResp>> list() {
        return R.ok(playerPlatformAccountService.listPlatformAccounts(UserContext.getRequiredMemberId()));
    }

    @ApiOperation("新增平台游戏 ID")
    @PostMapping("/add")
    public R<PlatformChangeSubmitResp> add(@RequestBody PlatformAccountAddReq req) {
        Long proofFileId = parseLongId(req.getProofFileId(), "证明材料");
        PlatformChangeSubmitResp resp = playerPlatformAccountService.submitAddPlatform(
                UserContext.getRequiredMemberId(),
                req.getPlatform(),
                req.getGameId(),
                proofFileId);
        return R.ok(resp, "申请已提交");
    }

    @ApiOperation("修改平台游戏 ID")
    @PostMapping("/{id}/change-game-id")
    public R<PlatformChangeSubmitResp> changeGameId(@PathVariable("id") String id,
                                                    @RequestBody PlatformAccountChangeGameIdReq req) {
        Long platformAccountId = parseLongId(id, "平台账号");
        Long proofFileId = parseLongId(req.getProofFileId(), "证明材料");
        PlatformChangeSubmitResp resp = playerPlatformAccountService.submitChangeGameId(
                UserContext.getRequiredMemberId(),
                platformAccountId,
                req.getNewGameId(),
                proofFileId);
        return R.ok(resp, "申请已提交");
    }

    @ApiOperation("平台变更历史")
    @GetMapping("/change-history")
    public R changeHistory(@RequestParam(required = false) Integer currentPage,
                           @RequestParam(required = false) Integer pageSize) {
        Long memberId = UserContext.getRequiredMemberId();
        if (currentPage == null && pageSize == null) {
            return R.ok(playerPlatformAccountService.listChangeHistory(memberId));
        }
        return R.ok(playerPlatformAccountService.pageChangeHistory(memberId, currentPage, pageSize));
    }

    @ApiOperation("参赛资格状态")
    @GetMapping("/eligibility")
    public R<PlayerEligibilityStatusResp> eligibility() {
        return R.ok(playerPlatformAccountService.getEligibilityStatus(UserContext.getRequiredMemberId()));
    }

    private Long parseLongId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new ServiceException(fieldName + "无效");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "无效");
        }
    }
}
