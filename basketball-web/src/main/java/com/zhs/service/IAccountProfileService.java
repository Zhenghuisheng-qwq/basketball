package com.zhs.service;

import com.zhs.request.account.AccountAvatarBindReq;
import com.zhs.request.account.AccountProfileChangeSubmitReq;
import com.zhs.request.account.AccountProfileUpdateReq;
import com.zhs.response.account.AccountProfileChangeResp;
import com.zhs.response.account.AccountProfileResp;

/**
 * 账户资料业务服务
 */
public interface IAccountProfileService {

    AccountProfileResp getProfile();

    AccountProfileResp updateProfile(AccountProfileUpdateReq req);

    /**
     * 仅校验并返回头像预览 URL，不写入正式资料。
     */
    String bindAvatar(AccountAvatarBindReq req);

    AccountProfileChangeResp submitPublicProfileChange(AccountProfileChangeSubmitReq req);
}
