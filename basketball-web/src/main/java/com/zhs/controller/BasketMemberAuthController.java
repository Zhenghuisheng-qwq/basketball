package com.zhs.controller;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.request.member.EmailCodeSendReq;
import com.zhs.request.member.MemberCodeLoginReq;
import com.zhs.request.member.MemberPasswordLoginReq;
import com.zhs.request.member.MemberPasswordResetReq;
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.response.member.EmailCodeSendResp;
import com.zhs.response.member.MemberLoginResp;
import com.zhs.response.member.MemberSessionResp;
import com.zhs.service.IBasketMemberAuthService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 官网会员认证入口
 */
@RestController
@RequestMapping("/basket/member/auth")
@Api(value = "官网会员认证")
@Slf4j
public class BasketMemberAuthController {

    @Resource
    private IBasketMemberAuthService basketMemberAuthService;

    @NoLoginRequest
    @ApiOperation("发送邮箱验证码")
    @PostMapping("/email-code/send")
    public R<EmailCodeSendResp> sendEmailCode(@RequestBody EmailCodeSendReq req, HttpServletRequest request) {
        return R.ok(basketMemberAuthService.sendEmailCode(req, resolveClientIp(request)), "验证码已发送");
    }

    @NoLoginRequest
    @ApiOperation("检查登录账号是否可用")
    @GetMapping("/username/available")
    public R<Void> checkUsernameAvailable(@org.springframework.web.bind.annotation.RequestParam String username) {
        basketMemberAuthService.assertUsernameAvailable(username);
        return R.ok(null, "登录账号可用");
    }

    @NoLoginRequest
    @ApiOperation("注册官网会员")
    @PostMapping("/register")
    public R<MemberLoginResp> register(@RequestBody MemberRegisterReq req, HttpServletRequest request) {
        return R.ok(basketMemberAuthService.register(req, resolveClientIp(request), request.getHeader("User-Agent")), "注册成功");
    }

    @NoLoginRequest
    @ApiOperation("密码登录")
    @PostMapping("/login/password")
    public R<MemberLoginResp> loginByPassword(@RequestBody MemberPasswordLoginReq req, HttpServletRequest request) {
        return R.ok(basketMemberAuthService.loginByPassword(req, resolveClientIp(request), request.getHeader("User-Agent")), "登录成功");
    }

    @NoLoginRequest
    @ApiOperation("邮箱验证码登录")
    @PostMapping("/login/code")
    public R<MemberLoginResp> loginByCode(@RequestBody MemberCodeLoginReq req, HttpServletRequest request) {
        return R.ok(basketMemberAuthService.loginByCode(req, resolveClientIp(request), request.getHeader("User-Agent")), "登录成功");
    }

    @NoLoginRequest
    @ApiOperation("找回密码")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@RequestBody MemberPasswordResetReq req, HttpServletRequest request) {
        basketMemberAuthService.resetPassword(req, resolveClientIp(request));
        return R.ok(null, "密码已重置");
    }

    @ApiOperation("当前登录会话")
    @GetMapping("/session")
    public R<MemberSessionResp> session(HttpServletRequest request) {
        return R.ok(basketMemberAuthService.getSession(UserContext.getRequiredMemberId(), TokenUtil.resolveFrom(request)));
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        basketMemberAuthService.logout(TokenUtil.resolveFrom(request));
        return R.ok(null, "已退出登录");
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
