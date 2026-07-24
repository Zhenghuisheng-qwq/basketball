package com.zhs.controller;

import com.zhs.request.account.AccountAvatarBindReq;
import com.zhs.request.account.AccountProfileUpdateReq;
import com.zhs.response.account.AccountProfileResp;
import com.zhs.service.IAccountProfileService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/profile")
@Api(value = "账户资料")
@Slf4j
public class AccountProfileController {

    @Resource
    private IAccountProfileService accountProfileService;

    @ApiOperation("获取当前账户资料")
    @GetMapping("")
    public R<AccountProfileResp> getProfile() {
        return R.ok(accountProfileService.getProfile());
    }

    @ApiOperation("更新当前账户资料")
    @PutMapping("")
    public R<AccountProfileResp> updateProfile(@RequestBody AccountProfileUpdateReq req) {
        return R.ok(accountProfileService.updateProfile(req), "资料更新成功");
    }

    @ApiOperation("绑定账户头像")
    @PostMapping("/avatar")
    public R<String> bindAvatar(@RequestBody AccountAvatarBindReq req) {
        return R.ok(accountProfileService.bindAvatar(req), "头像绑定成功");
    }
}
