package com.zhs.controller.v4;

import com.zhs.request.v4.V4RegistrationIdReq;
import com.zhs.request.v4.V4RegistrationSaveReq;
import com.zhs.service.v4.IBasketV4PlayerRegistrationService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@Api(tags = "V4 球员报名")
@RestController
@RequestMapping("/basket/v4/player/registration")
public class BasketV4PlayerRegistrationController {

    @Resource
    private IBasketV4PlayerRegistrationService playerRegistrationService;

    @ApiOperation("查询可报名 Division")
    @GetMapping("/available-divisions")
    public R listAvailableDivisions() {
        return R.ok(playerRegistrationService.listAvailableDivisions());
    }

    @ApiOperation("保存报名草稿")
    @PostMapping("/save")
    public R save(@RequestBody V4RegistrationSaveReq req) {
        return R.ok(playerRegistrationService.saveRegistration(req));
    }

    @ApiOperation("提交报名")
    @PostMapping("/submit")
    public R submit(@RequestBody V4RegistrationIdReq req) {
        playerRegistrationService.submitRegistration(req);
        return R.ok();
    }

    @ApiOperation("撤回报名")
    @PostMapping("/withdraw")
    public R withdraw(@RequestBody V4RegistrationIdReq req) {
        playerRegistrationService.withdrawRegistration(req);
        return R.ok();
    }

    @ApiOperation("我的报名列表")
    @GetMapping("/my-list")
    public R myList() {
        return R.ok(playerRegistrationService.listMyRegistrations());
    }

    @ApiOperation("报名详情/阵容")
    @GetMapping("/{registrationId}/detail")
    public R detail(@PathVariable Long registrationId) {
        return R.ok(playerRegistrationService.getRegistrationDetail(registrationId));
    }
}
