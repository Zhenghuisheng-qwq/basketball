package com.zhs.controller;

import com.zhs.request.account.NotificationSettingUpdateReq;
import com.zhs.request.account.PrivacySettingUpdateReq;
import com.zhs.response.account.NotificationSettingResp;
import com.zhs.response.account.PrivacySettingResp;
import com.zhs.service.IAccountSettingService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/setting")
@Api(value = "账户设置")
@Slf4j
public class AccountSettingController {

    @Resource
    private IAccountSettingService accountSettingService;

    @ApiOperation("获取隐私设置")
    @GetMapping("/privacy")
    public R<PrivacySettingResp> getPrivacy() {
        return R.ok(accountSettingService.getPrivacy());
    }

    @ApiOperation("更新隐私设置")
    @PutMapping("/privacy")
    public R<PrivacySettingResp> updatePrivacy(@RequestBody PrivacySettingUpdateReq req) {
        return R.ok(accountSettingService.updatePrivacy(req), "隐私设置更新成功");
    }

    @ApiOperation("获取通知设置")
    @GetMapping("/notification")
    public R<NotificationSettingResp> getNotification() {
        return R.ok(accountSettingService.getNotification());
    }

    @ApiOperation("更新通知设置")
    @PutMapping("/notification")
    public R<NotificationSettingResp> updateNotification(@RequestBody NotificationSettingUpdateReq req) {
        return R.ok(accountSettingService.updateNotification(req), "通知设置更新成功");
    }
}
