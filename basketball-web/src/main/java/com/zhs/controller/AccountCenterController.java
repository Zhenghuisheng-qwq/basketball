package com.zhs.controller;

import com.zhs.response.account.AccountCenterOverviewResp;
import com.zhs.service.IAccountCenterService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/center")
@Api(value = "账户中心")
@Slf4j
public class AccountCenterController {

    @Resource
    private IAccountCenterService accountCenterService;

    @ApiOperation("获取账户中心概览")
    @GetMapping("/overview")
    public R<AccountCenterOverviewResp> getOverview() {
        return R.ok(accountCenterService.getOverview());
    }
}
