package com.zhs.controller;

import com.zhs.request.player.PlayerCertDraftReq;
import com.zhs.request.player.PlayerCertMaterialBindReq;
import com.zhs.request.player.PlayerCertSubmitReq;
import com.zhs.response.player.PlayerCertCurrentResp;
import com.zhs.response.player.PlayerCertMaterialResp;
import com.zhs.service.IPlayerCertificationService;
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

    @ApiOperation("提交球员认证申请")
    @PostMapping("/submit")
    public R<PlayerCertCurrentResp> submit(@RequestBody PlayerCertSubmitReq req, HttpServletRequest request) {
        return R.ok(playerCertificationService.submit(req, resolveClientIp(request)), "认证申请已提交");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
