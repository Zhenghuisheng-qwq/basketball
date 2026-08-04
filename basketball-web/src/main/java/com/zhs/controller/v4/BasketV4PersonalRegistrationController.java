package com.zhs.controller.v4;

import com.zhs.request.v4.V4PersonalRegistrationIdReq;
import com.zhs.request.v4.V4PersonalRegistrationSaveReq;
import com.zhs.service.v4.IBasketV4PersonalRegistrationService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@Api(tags = "V4 个人报名")
@RestController
@RequestMapping("/basket/v4/player/personal-registration")
public class BasketV4PersonalRegistrationController {

    @Resource
    private IBasketV4PersonalRegistrationService personalRegistrationService;

    @ApiOperation("查询可个人报名 Division")
    @GetMapping("/available-divisions")
    public R listAvailableDivisions() {
        return R.ok(personalRegistrationService.listAvailableDivisions());
    }

    @ApiOperation("保存个人报名草稿")
    @PostMapping("/save")
    public R save(@RequestBody V4PersonalRegistrationSaveReq req) {
        return R.ok(personalRegistrationService.saveRegistration(req));
    }

    @ApiOperation("提交个人报名")
    @PostMapping("/submit")
    public R submit(@RequestBody V4PersonalRegistrationIdReq req) {
        personalRegistrationService.submitRegistration(req);
        return R.ok();
    }

    @ApiOperation("撤回个人报名")
    @PostMapping("/withdraw")
    public R withdraw(@RequestBody V4PersonalRegistrationIdReq req) {
        personalRegistrationService.withdrawRegistration(req);
        return R.ok();
    }

    @ApiOperation("我的个人报名列表")
    @GetMapping("/my-list")
    public R myList() {
        return R.ok(personalRegistrationService.listMyRegistrations());
    }
}
