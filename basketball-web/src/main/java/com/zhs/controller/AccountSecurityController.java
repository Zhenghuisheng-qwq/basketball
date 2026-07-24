package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.account.LoginLogItemResp;
import com.zhs.response.account.SecurityLogItemResp;
import com.zhs.service.IAccountSecurityService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/security")
@Api(value = "账户安全")
@Slf4j
public class AccountSecurityController {

    @Resource
    private IAccountSecurityService accountSecurityService;

    @ApiOperation("分页查询登录日志")
    @GetMapping("/login-log/page")
    public R<IPage<LoginLogItemResp>> pageLoginLogs(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(accountSecurityService.pageLoginLogs(currentPage, pageSize));
    }

    @ApiOperation("分页查询安全操作日志")
    @GetMapping("/operation-log/page")
    public R<IPage<SecurityLogItemResp>> pageOperationLogs(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(accountSecurityService.pageOperationLogs(currentPage, pageSize));
    }
}
