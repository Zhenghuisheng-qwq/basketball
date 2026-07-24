package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicMatchDetailVo;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.service.IBasketPublicMatchService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public")
@Api(value = "官网公开赛程赛果")
public class BasketPublicMatchController {

    @Resource
    private IBasketPublicMatchService publicMatchService;

    @ApiOperation("公开赛程分页")
    @GetMapping("/season/{seasonId}/schedule/page")
    public R<IPage<PublicMatchItemVo>> pageSchedule(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long teamId) {
        return R.ok(publicMatchService.pageSchedule(seasonId, currentPage, pageSize, stageId, groupId, teamId));
    }

    @ApiOperation("公开赛果分页")
    @GetMapping("/season/{seasonId}/result/page")
    public R<IPage<PublicMatchItemVo>> pageResult(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long teamId) {
        return R.ok(publicMatchService.pageResult(seasonId, currentPage, pageSize, stageId, groupId, teamId));
    }

    @ApiOperation("公开比赛详情")
    @GetMapping("/match/{matchId}")
    public R<PublicMatchDetailVo> getMatch(@PathVariable Long matchId) {
        return R.ok(publicMatchService.getMatch(matchId));
    }
}
