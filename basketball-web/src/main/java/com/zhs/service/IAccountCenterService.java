package com.zhs.service;

import com.zhs.response.account.AccountCenterOverviewResp;

/**
 * 账户中心业务服务
 */
public interface IAccountCenterService {

    AccountCenterOverviewResp getOverview();
}
