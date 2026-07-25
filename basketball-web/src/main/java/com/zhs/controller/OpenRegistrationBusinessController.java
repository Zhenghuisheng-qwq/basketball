package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.participation.OpenRegistrationDraftReq;
import com.zhs.request.participation.OpenRegistrationWithdrawReq;
import com.zhs.response.participation.OpenAvailableSeasonItemResp;
import com.zhs.response.participation.OpenRegistrationDetailResp;
import com.zhs.response.participation.OpenRegistrationItemResp;
import com.zhs.service.IOpenRegistrationBusinessService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "Open 个人报名")
@Slf4j
public class OpenRegistrationBusinessController {

    @Resource
    private IOpenRegistrationBusinessService openRegistrationBusinessService;

    @ApiOperation("Open 可报名赛季分页")
    @GetMapping("/basket/player/open-registration/available-season/page")
    public R<IPage<OpenAvailableSeasonItemResp>> pageAvailableOpenSeasons(@RequestParam(required = false) Integer currentPage,
                                                                         @RequestParam(required = false) Integer pageSize) {
        return R.ok(openRegistrationBusinessService.pageAvailableOpenSeasons(currentPage, pageSize));
    }

    @ApiOperation("创建 Open 报名草稿")
    @PostMapping("/basket/player/open-registration/draft")
    public R<OpenRegistrationDetailResp> createDraft(@RequestBody OpenRegistrationDraftReq req) {
        return R.ok(openRegistrationBusinessService.createDraft(req), "草稿创建成功");
    }

    @ApiOperation("Open 报名详情")
    @GetMapping("/basket/player/open-registration/{registrationId}")
    public R<OpenRegistrationDetailResp> getDetail(@PathVariable String registrationId) {
        return R.ok(openRegistrationBusinessService.getDetail(registrationId));
    }

    @ApiOperation("提交 Open 报名")
    @PostMapping("/basket/player/open-registration/{registrationId}/submit")
    public R<OpenRegistrationDetailResp> submit(@PathVariable String registrationId) {
        return R.ok(openRegistrationBusinessService.submit(registrationId), "报名提交成功");
    }

    @ApiOperation("撤回 Open 报名")
    @PostMapping("/basket/player/open-registration/{registrationId}/withdraw")
    public R<OpenRegistrationDetailResp> withdraw(@PathVariable String registrationId,
                                                  @RequestBody(required = false) OpenRegistrationWithdrawReq req) {
        return R.ok(openRegistrationBusinessService.withdraw(registrationId, req), "报名撤回成功");
    }

    @ApiOperation("我的 Open 报名分页")
    @GetMapping("/basket/player/open-registration/page")
    public R<IPage<OpenRegistrationItemResp>> pageMyRegistrations(@RequestParam(required = false) Integer currentPage,
                                                                  @RequestParam(required = false) Integer pageSize,
                                                                  @RequestParam(required = false) Integer status) {
        return R.ok(openRegistrationBusinessService.pageMyRegistrations(currentPage, pageSize, status));
    }
}
