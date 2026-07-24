package com.zhs.service;

import com.zhs.request.account.NotificationSettingUpdateReq;
import com.zhs.request.account.PrivacySettingUpdateReq;
import com.zhs.response.account.NotificationSettingResp;
import com.zhs.response.account.PrivacySettingResp;

/**
 * 账户设置业务服务
 */
public interface IAccountSettingService {

    PrivacySettingResp getPrivacy();

    PrivacySettingResp updatePrivacy(PrivacySettingUpdateReq req);

    NotificationSettingResp getNotification();

    NotificationSettingResp updateNotification(NotificationSettingUpdateReq req);
}
