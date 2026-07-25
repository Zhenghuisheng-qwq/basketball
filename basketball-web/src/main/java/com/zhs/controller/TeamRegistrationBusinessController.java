package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamRegistrationDraftReq;
import com.zhs.request.team.TeamRegistrationRemindReq;
import com.zhs.request.team.TeamRegistrationRosterReq;
import com.zhs.request.team.TeamRegistrationUpdateReq;
import com.zhs.request.team.TeamRegistrationWithdrawReq;
import com.zhs.response.account.MyEventsContextResp;
import com.zhs.response.team.AvailableSeasonItemResp;
import com.zhs.response.team.AvailableSeasonPageResp;
import com.zhs.response.team.TeamEventRecordItemResp;
import com.zhs.response.team.TeamRegistrationAuditItemResp;
import com.zhs.response.team.TeamRegistrationDetailResp;
import com.zhs.response.team.TeamRegistrationItemResp;
import com.zhs.service.ITeamRegistrationBusinessService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "球队赛事报名")
@Slf4j
public class TeamRegistrationBusinessController {

    @Resource
    private ITeamRegistrationBusinessService teamRegistrationBusinessService;

    @ApiOperation("可报名赛事分页")
    @GetMapping("/basket/team/{teamId}/registration/available-season/page")
    public R<AvailableSeasonPageResp> pageAvailableSeasons(@PathVariable String teamId,
                                                                @RequestParam(required = false) Integer currentPage,
                                                                @RequestParam(required = false) Integer pageSize) {
        return R.ok(teamRegistrationBusinessService.pageAvailableSeasons(teamId, currentPage, pageSize));
    }

    @ApiOperation("队员视角-可参与赛事分页")
    @GetMapping("/basket/team/{teamId}/registration/member/available-season/page")
    public R<AvailableSeasonPageResp> pageMemberAvailableSeasons(@PathVariable String teamId,
                                                                 @RequestParam(required = false) Integer currentPage,
                                                                 @RequestParam(required = false) Integer pageSize) {
        return R.ok(teamRegistrationBusinessService.pageMemberAvailableSeasons(teamId, currentPage, pageSize));
    }

    @ApiOperation("队员视角-球队赛事记录分页")
    @GetMapping("/basket/team/{teamId}/registration/member/event/page")
    public R<IPage<TeamEventRecordItemResp>> pageMemberTeamEvents(@PathVariable String teamId,
                                                                  @RequestParam(required = false) Integer currentPage,
                                                                  @RequestParam(required = false) Integer pageSize) {
        return R.ok(teamRegistrationBusinessService.pageMemberTeamEvents(teamId, currentPage, pageSize));
    }

    @ApiOperation("提醒队长报名")
    @PostMapping("/basket/team/{teamId}/registration/remind-captain")
    public R<Void> remindCaptainToRegister(@PathVariable String teamId,
                                           @RequestBody TeamRegistrationRemindReq req) {
        teamRegistrationBusinessService.remindCaptainToRegister(teamId, req);
        return R.ok(null, "已提醒队长");
    }

    @ApiOperation("我的赛事页面身份上下文")
    @GetMapping("/basket/account/my-events/context")
    public R<MyEventsContextResp> getMyEventsContext() {
        return R.ok(teamRegistrationBusinessService.getMyEventsContext());
    }

    @ApiOperation("球队报名分页")
    @GetMapping("/basket/team/{teamId}/registration/page")
    public R<IPage<TeamRegistrationItemResp>> pageRegistrations(@PathVariable String teamId,
                                                                @RequestParam(required = false) Integer currentPage,
                                                                @RequestParam(required = false) Integer pageSize,
                                                                @RequestParam(required = false) Integer status) {
        return R.ok(teamRegistrationBusinessService.pageRegistrations(teamId, currentPage, pageSize, status));
    }

    @ApiOperation("创建报名草稿")
    @PostMapping("/basket/team/{teamId}/registration/draft")
    public R<TeamRegistrationDetailResp> createDraft(@PathVariable String teamId,
                                                   @RequestBody TeamRegistrationDraftReq req) {
        return R.ok(teamRegistrationBusinessService.createDraft(teamId, req), "草稿创建成功");
    }

    @ApiOperation("报名详情")
    @GetMapping("/basket/team/registration/{registrationId}")
    public R<TeamRegistrationDetailResp> getDetail(@PathVariable String registrationId) {
        return R.ok(teamRegistrationBusinessService.getDetail(registrationId));
    }

    @ApiOperation("队员只读-报名详情")
    @GetMapping("/basket/team/registration/{registrationId}/member-view")
    public R<TeamRegistrationDetailResp> getMemberDetail(@PathVariable String registrationId) {
        return R.ok(teamRegistrationBusinessService.getMemberDetail(registrationId));
    }

    @ApiOperation("更新报名")
    @PutMapping("/basket/team/registration/{registrationId}")
    public R<TeamRegistrationDetailResp> update(@PathVariable String registrationId,
                                              @RequestBody TeamRegistrationUpdateReq req) {
        return R.ok(teamRegistrationBusinessService.update(registrationId, req), "报名更新成功");
    }

    @ApiOperation("保存报名阵容")
    @PutMapping("/basket/team/registration/{registrationId}/roster")
    public R<TeamRegistrationDetailResp> saveRoster(@PathVariable String registrationId,
                                                    @RequestBody TeamRegistrationRosterReq req) {
        return R.ok(teamRegistrationBusinessService.saveRoster(registrationId, req), "阵容保存成功");
    }

    @ApiOperation("提交报名")
    @PostMapping("/basket/team/registration/{registrationId}/submit")
    public R<TeamRegistrationDetailResp> submit(@PathVariable String registrationId) {
        return R.ok(teamRegistrationBusinessService.submit(registrationId), "报名已提交，正在等待后台审核");
    }

    @ApiOperation("撤回报名")
    @PostMapping("/basket/team/registration/{registrationId}/withdraw")
    public R<TeamRegistrationDetailResp> withdraw(@PathVariable String registrationId,
                                                  @RequestBody(required = false) TeamRegistrationWithdrawReq req) {
        return R.ok(teamRegistrationBusinessService.withdraw(registrationId, req), "报名撤回成功");
    }

    @ApiOperation("删除报名草稿")
    @DeleteMapping("/basket/team/registration/{registrationId}/draft")
    public R<Void> deleteDraft(@PathVariable String registrationId) {
        teamRegistrationBusinessService.deleteDraft(registrationId);
        return R.ok(null, "草稿删除成功");
    }

    @ApiOperation("报名审核记录分页")
    @GetMapping("/basket/team/registration/{registrationId}/audit-record/page")
    public R<IPage<TeamRegistrationAuditItemResp>> pageAuditRecords(@PathVariable String registrationId,
                                                                    @RequestParam(required = false) Integer currentPage,
                                                                    @RequestParam(required = false) Integer pageSize) {
        return R.ok(teamRegistrationBusinessService.pageAuditRecords(registrationId, currentPage, pageSize));
    }
}
