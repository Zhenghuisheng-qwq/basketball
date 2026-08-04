package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
import com.zhs.response.v4.publicapi.V4PublicStageItemResp;
import com.zhs.response.v4.publicapi.V4PublicStageStandingScopeResp;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/stage")
@Api(value = "V4 公开阶段")
public class BasketV4PublicStageController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("Division 阶段列表")
    @GetMapping("/list")
    public R<List<V4PublicStageItemResp>> listStages(@RequestParam Long divisionId) {
        return R.ok(v4PublicService.listStages(divisionId));
    }

    @ApiOperation("阶段积分榜（按小组 scope）")
    @GetMapping("/{stageId}/standings")
    public R<List<V4PublicStageStandingScopeResp>> listStageStandings(@PathVariable Long stageId) {
        return R.ok(v4PublicService.listStageStandings(stageId));
    }

    @ApiOperation("阶段 BO1 淘汰对阵")
    @GetMapping("/{stageId}/bracket")
    public R<List<V4PublicMatchItemResp>> listStageBracket(@PathVariable Long stageId) {
        return R.ok(v4PublicService.listStageBracket(stageId));
    }
}
