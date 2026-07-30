package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.account.AccountChangeEmailReq;
import com.zhs.response.account.AccountChangeEmailResp;
import com.zhs.response.account.AccountChangePasswordResp;
import com.zhs.response.account.LoginLogItemResp;
import com.zhs.request.account.AccountChangePasswordReq;
import com.zhs.response.account.SecurityLogItemResp;

/**
 * 账户安全日志业务服务
 */
public interface IAccountSecurityService {

    IPage<LoginLogItemResp> pageLoginLogs(Integer currentPage, Integer pageSize);

    IPage<SecurityLogItemResp> pageOperationLogs(Integer currentPage, Integer pageSize);

    AccountChangePasswordResp changePassword(AccountChangePasswordReq req, String clientIp, String terminalType, String currentToken);

    AccountChangeEmailResp changeEmail(AccountChangeEmailReq req, String clientIp, String currentToken);
}
