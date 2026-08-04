package com.zhs.controller.v4;

import com.zhs.request.v4.V4PersonalFormationBindTeamReq;
import com.zhs.service.v4.IBasketV4PersonalFormationService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@Api(tags = "V4 个人组队")
@RestController
@RequestMapping("/basket/v4/player/personal-formation")
public class BasketV4PersonalFormationController {

    @Resource
    private IBasketV4PersonalFormationService personalFormationService;

    @ApiOperation("我的组队任务列表")
    @GetMapping("/tasks")
    public R listTasks() {
        return R.ok(personalFormationService.listFormationTasks());
    }

    @ApiOperation("组队任务详情")
    @GetMapping("/tasks/{drawTeamId}")
    public R detail(@PathVariable Long drawTeamId) {
        return R.ok(personalFormationService.getFormationDetail(drawTeamId));
    }

    @ApiOperation("绑定长期球队")
    @PostMapping("/bind-team")
    public R bindTeam(@RequestBody V4PersonalFormationBindTeamReq req) {
        personalFormationService.bindLongTermTeam(req);
        return R.ok();
    }

    @ApiOperation("确认组队并转换正式报名")
    @PostMapping("/tasks/{drawTeamId}/confirm")
    public R confirm(@PathVariable Long drawTeamId) {
        return R.ok(personalFormationService.confirmFormation(drawTeamId));
    }
}
