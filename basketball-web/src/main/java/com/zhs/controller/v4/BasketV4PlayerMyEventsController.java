package com.zhs.controller.v4;

import com.zhs.service.v4.IBasketV4PlayerMyEventsService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "V4 我的赛事")
@RestController
@RequestMapping("/basket/v4/player/my-events")
public class BasketV4PlayerMyEventsController {

    @Resource
    private IBasketV4PlayerMyEventsService myEventsService;

    @ApiOperation("已参加赛事（EventTeam/EventPlayer 快照）")
    @GetMapping
    public R myEvents() {
        return R.ok(myEventsService.listMyEvents());
    }
}
