package com.zhs.service;

import com.zhs.request.member.EmailCodeSendReq;
import com.zhs.request.member.MemberCodeLoginReq;
import com.zhs.request.member.MemberPasswordLoginReq;
import com.zhs.request.member.MemberPasswordResetReq;
import com.zhs.request.member.MemberRegisterReq;
import com.zhs.response.member.EmailCodeSendResp;
import com.zhs.response.member.MemberLoginResp;
import com.zhs.response.member.MemberSessionResp;

/**
 * 官网会员认证业务
 */
public interface IBasketMemberAuthService {

    EmailCodeSendResp sendEmailCode(EmailCodeSendReq req, String clientIp);

    void assertUsernameAvailable(String username);

    MemberLoginResp register(MemberRegisterReq req, String clientIp, String userAgent);

    MemberLoginResp loginByPassword(MemberPasswordLoginReq req, String clientIp, String userAgent);

    MemberLoginResp loginByCode(MemberCodeLoginReq req, String clientIp, String userAgent);

    void resetPassword(MemberPasswordResetReq req, String clientIp);

    MemberSessionResp getSession(Long memberId, String token);

    void logout(String token);
}
