package com.zhs.service;

import com.zhs.request.account.AccountChangeEmailReq;
import com.zhs.request.member.EmailCodeSendReq;
import com.zhs.request.member.MemberPasswordLoginReq;
import com.zhs.request.member.MemberPasswordResetReq;
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.response.account.AccountChangeEmailResp;
import com.zhs.response.member.EmailCodeSendResp;
import com.zhs.response.member.MemberLoginResp;
import com.zhs.response.member.MemberRegisterResp;
import com.zhs.response.member.MemberSessionResp;

/**
 * 官网用户认证业务
 */
public interface IBasketMemberAuthService {

    EmailCodeSendResp sendEmailCode(EmailCodeSendReq req, String clientIp);

    MemberRegisterResp register(MemberRegisterReq req, String clientIp, String userAgent);

    EmailCodeSendResp sendChangeEmailCode(String newEmail, Long memberId, String clientIp);

    AccountChangeEmailResp changeEmail(AccountChangeEmailReq req, Long memberId, String clientIp, String currentToken);

    MemberLoginResp loginByPassword(MemberPasswordLoginReq req, String clientIp, String userAgent);

    void resetPassword(MemberPasswordResetReq req, String clientIp);

    MemberSessionResp getSession(Long memberId, String token);

    void logout(String token);
}
