package com.zhs.controller.v4;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.v4.V4MyAvailableEventItemResp;
import com.zhs.response.v4.V4MyEventResp;
import com.zhs.service.v4.IBasketV4PlayerMyEventsService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "V4 我的赛事")
@RestController
@RequestMapping("/basket/v4/player/my-events")
public class BasketV4PlayerMyEventsController {

    @Resource
    private IBasketV4PlayerMyEventsService myEventsService;

    @ApiOperation("已参加赛事（EventTeam/EventPlayer 快照）")
    @GetMapping
    public R myEvents(@RequestParam(required = false) Integer currentPage,
                      @RequestParam(required = false) Integer pageSize) {
        if (currentPage == null && pageSize == null) {
            return R.ok(myEventsService.listMyEvents());
        }
        return R.ok(myEventsService.pageMyEvents(currentPage, pageSize));
    }

    @ApiOperation("可参与赛事分页（个人 + 球队报名合并）")
    @GetMapping("/available")
    public R<IPage<V4MyAvailableEventItemResp>> pageAvailable(
            @RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        return R.ok(myEventsService.pageAvailableEvents(currentPage, pageSize));
    }
}
