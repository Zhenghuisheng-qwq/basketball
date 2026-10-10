package com.zhs.controller;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.request.member.EmailCodeSendReq;
import com.zhs.request.member.MemberPasswordLoginReq;
import com.zhs.request.member.MemberPasswordResetReq;
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.response.member.EmailCodeSendResp;
import com.zhs.response.member.MemberLoginResp;
import com.zhs.response.member.MemberRegisterResp;
import com.zhs.response.member.MemberSessionResp;
import com.zhs.service.IBasketMemberAuthService;
import com.zhs.support.AgreementCatalog;
import com.zhs.support.ClientIpResolver;
import com.zhs.support.MemberLocaleSupport;
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
 * 官网用户认证入口
 */
@RestController
@RequestMapping("/basket/member/auth")
@Api(value = "官网用户认证")
@Slf4j
public class BasketMemberAuthController {

    @Resource
    private IBasketMemberAuthService basketMemberAuthService;
    @Resource
    private ClientIpResolver clientIpResolver;

    @NoLoginRequest
    @ApiOperation("发送邮箱验证码")
    @PostMapping("/email-code/send")
    public R<EmailCodeSendResp> sendEmailCode(@RequestBody EmailCodeSendReq req, HttpServletRequest request) {
        if (req != null) {
            req.setLocale(MemberLocaleSupport.resolve(
                    req.getLocale(),
                    request.getHeader("X-CNPA-Locale"),
                    request.getHeader("Accept-Language")));
        }
        return R.ok(basketMemberAuthService.sendEmailCode(req, clientIpResolver.resolve(request)), "验证码已发送");
    }

    @NoLoginRequest
    @ApiOperation("当前协议版本。语言切换不改变同意记录版本。")
    @GetMapping("/agreements")
    public R<java.util.List<java.util.Map<String, String>>> agreements() {
        java.util.List<java.util.Map<String, String>> items = new java.util.ArrayList<>();
        items.add(agreementMeta(AgreementCatalog.USER_AGREEMENT, AgreementCatalog.USER_AGREEMENT_TITLE, "User Agreement"));
        items.add(agreementMeta(AgreementCatalog.PRIVACY_POLICY, AgreementCatalog.PRIVACY_POLICY_TITLE, "Privacy Policy"));
        return R.ok(items);
    }

    private java.util.Map<String, String> agreementMeta(String type, String titleZh, String titleEn) {
        java.util.Map<String, String> item = new java.util.LinkedHashMap<>();
        item.put("agreementType", type);
        item.put("agreementVersion", AgreementCatalog.VERSION);
        item.put("titleZh", titleZh);
        item.put("titleEn", titleEn);
        item.put("englishReviewStatus", AgreementCatalog.ENGLISH_REVIEW_STATUS);
        item.put("publishedAt", "");
        return item;
    }

    @NoLoginRequest
    @ApiOperation("注册官网用户")
    @PostMapping("/register")
    public R<MemberRegisterResp> register(@RequestBody MemberRegisterReq req, HttpServletRequest request) {
        return R.ok(basketMemberAuthService.register(req, clientIpResolver.resolve(request), request.getHeader("User-Agent")), "注册成功");
    }

    @NoLoginRequest
    @ApiOperation("密码登录")
    @PostMapping("/login/password")
    public R<MemberLoginResp> loginByPassword(@RequestBody MemberPasswordLoginReq req, HttpServletRequest request) {
        return R.ok(basketMemberAuthService.loginByPassword(req, clientIpResolver.resolve(request), request.getHeader("User-Agent")), "登录成功");
    }

    @NoLoginRequest
    @ApiOperation("找回密码")
    @PostMapping("/password/reset")
    public R<Void> resetPassword(@RequestBody MemberPasswordResetReq req, HttpServletRequest request) {
        basketMemberAuthService.resetPassword(req, clientIpResolver.resolve(request));
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

}

