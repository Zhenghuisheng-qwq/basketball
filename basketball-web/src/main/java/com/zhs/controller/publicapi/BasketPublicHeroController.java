package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicPageHeroResp;
import com.zhs.service.IBasketPublicHeroService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public/hero")
@Api(value = "官网公开页面头图")
public class BasketPublicHeroController {

    @Resource
    private IBasketPublicHeroService heroService;

    @ApiOperation("已发布页面头图。没有配置时 configured=false，页面继续用静态头图")
    @GetMapping
    public R<PublicPageHeroResp> getHero(
            @RequestParam String pageKey,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(heroService.getPublished(pageKey, seasonId));
    }
}
