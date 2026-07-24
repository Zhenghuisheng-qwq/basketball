package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.account.LoginLogItemResp;
import com.zhs.response.account.SecurityLogItemResp;

/**
 * 账户安全日志业务服务
 */
public interface IAccountSecurityService {

    IPage<LoginLogItemResp> pageLoginLogs(Integer currentPage, Integer pageSize);

    IPage<SecurityLogItemResp> pageOperationLogs(Integer currentPage, Integer pageSize);
}
