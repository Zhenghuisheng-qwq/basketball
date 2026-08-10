package com.zhs.controller;

import com.zhs.request.account.AccountAvatarBindReq;
import com.zhs.request.account.AccountProfileChangeSubmitReq;
import com.zhs.request.account.AccountProfileUpdateReq;
import com.zhs.response.account.AccountProfileChangeResp;
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

    @ApiOperation("获取当前账户资料（正式值 + 待审核变更）")
    @GetMapping("")
    public R<AccountProfileResp> getProfile() {
        return R.ok(accountProfileService.getProfile());
    }

    @ApiOperation("更新非公开扩展资料（不含昵称/头像）")
    @PutMapping("")
    public R<AccountProfileResp> updateProfile(@RequestBody AccountProfileUpdateReq req) {
        return R.ok(accountProfileService.updateProfile(req), "资料更新成功");
    }

    @ApiOperation("预览账户头像（不写入正式资料）")
    @PostMapping("/avatar")
    public R<String> bindAvatar(@RequestBody AccountAvatarBindReq req) {
        return R.ok(accountProfileService.bindAvatar(req), "头像预览成功，保存资料后提交审核");
    }

    @ApiOperation("提交公开昵称/头像变更审核")
    @PostMapping("/change")
    public R<AccountProfileChangeResp> submitPublicProfileChange(@RequestBody AccountProfileChangeSubmitReq req) {
        return R.ok(accountProfileService.submitPublicProfileChange(req), "资料修改申请已提交，请等待管理员审核");
    }
}
