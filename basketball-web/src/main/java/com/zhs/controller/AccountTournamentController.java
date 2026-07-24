package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.account.AccountTournamentItemResp;
import com.zhs.service.IAccountTournamentService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/account/tournament")
@Api(value = "账户赛事")
@Slf4j
public class AccountTournamentController {

    @Resource
    private IAccountTournamentService accountTournamentService;

    @ApiOperation("分页查询当前会员赛事报名")
    @GetMapping("/page")
    public R<IPage<AccountTournamentItemResp>> pageTournaments(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return R.ok(accountTournamentService.pageTournaments(currentPage, pageSize, status, keyword));
    }

    @ApiOperation("查询赛事报名详情")
    @GetMapping("/{registrationId}")
    public R<AccountTournamentItemResp> getTournamentDetail(@PathVariable Long registrationId) {
        return R.ok(accountTournamentService.getTournamentDetail(registrationId));
    }
}
