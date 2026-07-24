package com.zhs.service;

import com.zhs.request.account.AccountAvatarBindReq;
import com.zhs.request.account.AccountProfileUpdateReq;
import com.zhs.response.account.AccountProfileResp;

/**
 * 账户资料业务服务
 */
public interface IAccountProfileService {

    AccountProfileResp getProfile();

    AccountProfileResp updateProfile(AccountProfileUpdateReq req);

    String bindAvatar(AccountAvatarBindReq req);
}
