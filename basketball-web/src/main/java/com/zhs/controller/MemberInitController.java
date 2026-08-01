package com.zhs.controller;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.member.MemberInitAccountsResp;
import com.zhs.service.IMemberInitService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员初始化入口。
 * <p>
 * 本接口仅完成：批量注册 + 提交球员认证申请。<br>
 * 后续「认证审核 → 授队长 → 建队 → 审队 → 申请入队 → 队长同意」请使用现成 Admin/Web API，
 * 仓库脚本一键编排：{@code node scripts/seed-cnpa-full-pipeline.mjs}
 */
@RestController
@RequestMapping("/basket/member/init")
@Api(value = "会员初始化")
@NoLoginRequest
public class MemberInitController {

    @Resource
    private IMemberInitService memberInitService;

    @ApiOperation("批量初始化测试账号并申请球员认证（不含审核/授队长/建队）")
    @PostMapping("/accounts")
    @NoLoginRequest
    public R<MemberInitAccountsResp> initAccounts(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        return R.ok(memberInitService.initAccounts(clientIp), "初始化完成");
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
