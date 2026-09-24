package com.zhs.controller;

import com.zhs.request.player.PlayerCertDraftReq;
import com.zhs.request.player.PlayerCertMaterialBindReq;
import com.zhs.request.player.PlayerCertSubmitReq;
import com.zhs.response.player.PlayerCertCurrentResp;
import com.zhs.response.player.PlayerCertMaterialResp;
import com.zhs.service.IPlayerCertificationService;
import com.zhs.support.ClientIpResolver;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/player/certification")
@Api(value = "球员认证")
@Slf4j
public class PlayerCertificationController {

    @Resource
    private IPlayerCertificationService playerCertificationService;
    @Resource
    private ClientIpResolver clientIpResolver;

    @ApiOperation("获取当前球员认证状态")
    @GetMapping("/current")
    public R<PlayerCertCurrentResp> getCurrent() {
        return R.ok(playerCertificationService.getCurrent());
    }

    @ApiOperation("保存球员认证草稿")
    @PostMapping("/draft")
    public R<PlayerCertCurrentResp> saveDraft(@RequestBody PlayerCertDraftReq req) {
        return R.ok(playerCertificationService.saveDraft(req), "草稿保存成功");
    }

    @ApiOperation("绑定球员认证材料")
    @PostMapping("/material")
    public R<PlayerCertMaterialResp> bindMaterial(@RequestBody PlayerCertMaterialBindReq req) {
        return R.ok(playerCertificationService.bindMaterial(req), "材料绑定成功");
    }

    @ApiOperation("删除球员认证材料")
    @DeleteMapping("/material/{materialId}")
    public R<Void> deleteMaterial(@PathVariable String materialId) {
        playerCertificationService.deleteMaterial(materialId);
        return R.ok(null, "材料删除成功");
    }

    @ApiOperation("撤回球员认证申请")
    @PostMapping("/withdraw")
    public R<PlayerCertCurrentResp> withdraw() {
        return R.ok(playerCertificationService.withdraw(), "申请已撤回");
    }

    @ApiOperation("提交球员认证申请")
    @PostMapping("/submit")
    public R<PlayerCertCurrentResp> submit(@RequestBody PlayerCertSubmitReq req, HttpServletRequest request) {
        return R.ok(playerCertificationService.submit(req, clientIpResolver.resolve(request)), "认证申请已提交");
    }
}
