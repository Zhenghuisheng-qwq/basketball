package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.UserContext;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.response.publicapi.PublicPlayerDetailVo;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.service.IBasketPublicPlayerService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "我的表现")
@RestController
@RequestMapping("/basket/account/performance")
public class AccountPerformanceController {

    @Resource
    private IBasketPublicPlayerService publicPlayerService;

    @ApiOperation("当前账号自己的球员页（已发布数据，不受公开开关影响）")
    @GetMapping("/player")
    public R<PublicPlayerDetailVo> ownPlayer() {
        return R.ok(publicPlayerService.getOwnPlayer(UserContext.getRequiredMemberId()));
    }

    @ApiOperation("当前账号自己的赛季数据")
    @GetMapping("/season-stat")
    public R<List<PublicPlayerSeasonStatVo>> ownSeasonStat(@RequestParam(required = false) Long seasonId) {
        return R.ok(publicPlayerService.listOwnSeasonStat(UserContext.getRequiredMemberId(), seasonId));
    }

    @ApiOperation("当前账号自己的已发布荣誉")
    @GetMapping("/honor")
    public R<IPage<PublicHonorItemResp>> ownHonors(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(publicPlayerService.pageOwnHonors(UserContext.getRequiredMemberId(), currentPage, pageSize));
    }
}
