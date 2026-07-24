package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicStandingItemVo;
import com.zhs.service.IBasketPublicStandingService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public")
@Api(value = "官网公开积分榜")
public class BasketPublicStandingController {

    @Resource
    private IBasketPublicStandingService publicStandingService;

    @ApiOperation("赛季积分榜（只读 basket_stage_standing）")
    @GetMapping("/season/{seasonId}/standing")
    public R<List<PublicStandingItemVo>> listBySeason(
            @PathVariable Long seasonId,
            @RequestParam(required = false) Long stageId) {
        return R.ok(publicStandingService.listBySeason(seasonId, stageId));
    }

    @ApiOperation("阶段积分榜")
    @GetMapping("/stage/{stageId}/standing")
    public R<List<PublicStandingItemVo>> listByStage(@PathVariable Long stageId) {
        return R.ok(publicStandingService.listByStage(stageId));
    }

    @ApiOperation("分组积分榜")
    @GetMapping("/group/{groupId}/standing")
    public R<List<PublicStandingItemVo>> listByGroup(@PathVariable Long groupId) {
        return R.ok(publicStandingService.listByGroup(groupId));
    }
}
