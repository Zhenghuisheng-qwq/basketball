package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.request.account.AccountChangeEmailReq;
import com.zhs.request.account.AccountChangePasswordReq;
import com.zhs.response.account.AccountChangeEmailResp;
import com.zhs.response.account.AccountChangePasswordResp;
import com.zhs.response.account.LoginLogItemResp;
import com.zhs.response.account.SecurityLogItemResp;
import com.zhs.service.IBasketMemberAuthService;
import com.zhs.service.IAccountSecurityService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/security")
@Api(value = "账户安全")
@Slf4j
public class AccountSecurityController {

    @Resource
    private IAccountSecurityService accountSecurityService;
    @Resource
    private IBasketMemberAuthService basketMemberAuthService;

    @ApiOperation("分页查询登录日志")
    @GetMapping("/login-log/page")
    public R<IPage<LoginLogItemResp>> pageLoginLogs(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(accountSecurityService.pageLoginLogs(currentPage, pageSize));
    }

    @ApiOperation("分页查询安全操作日志")
    @GetMapping("/operation-log/page")
    public R<IPage<SecurityLogItemResp>> pageOperationLogs(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(accountSecurityService.pageOperationLogs(currentPage, pageSize));
    }

    @ApiOperation("修改登录密码")
    @PostMapping("/change-password")
    public R<AccountChangePasswordResp> changePassword(@RequestBody AccountChangePasswordReq req, HttpServletRequest request) {
        AccountChangePasswordResp resp = accountSecurityService.changePassword(
                req, resolveClientIp(request), resolveTerminal(request), TokenUtil.resolveFrom(request));
        return R.ok(resp, "密码修改成功，请重新登录");
    }

    @ApiOperation("发送修改邮箱验证码")
    @PostMapping("/change-email/send-code")
    public R<com.zhs.response.member.EmailCodeSendResp> sendChangeEmailCode(
            @RequestBody com.zhs.request.member.EmailCodeSendReq req,
            HttpServletRequest request) {
        return R.ok(basketMemberAuthService.sendChangeEmailCode(
                req.getEmail(), UserContext.getRequiredMemberId(), resolveClientIp(request)), "验证码已发送");
    }

    @ApiOperation("修改登录邮箱")
    @PostMapping("/change-email")
    public R<AccountChangeEmailResp> changeEmail(@RequestBody AccountChangeEmailReq req, HttpServletRequest request) {
        AccountChangeEmailResp resp = accountSecurityService.changeEmail(
                req, resolveClientIp(request), TokenUtil.resolveFrom(request));
        return R.ok(resp, "邮箱修改成功，请重新登录");
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

    private String resolveTerminal(HttpServletRequest request) {
        String terminal = request.getHeader("X-Terminal-Type");
        return terminal != null && !terminal.isBlank() ? terminal.trim() : "WEB";
    }
}
