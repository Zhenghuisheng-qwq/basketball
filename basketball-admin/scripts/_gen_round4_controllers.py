# -*- coding: utf-8 -*-
"""Controller generation for Round 4."""


def generate(w):
    ctrl_base = "src/main/java/com/zhs/web/controller/basket"

    w(
        f"{ctrl_base}/BasketPlayerCertController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.req.PlayerCertApproveReq;
import com.zhs.basket.req.PlayerCertRejectReq;
import com.zhs.basket.service.IBasketPlayerCertService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/playerCert")
public class BasketPlayerCertController extends BaseController {

    @Autowired
    private IBasketPlayerCertService playerCertService;

    @PreAuthorize("@ss.hasPermi('basket:playerCert:list')")
    @GetMapping("/list")
    public TableDataInfo list(PlayerCertQuery query) {
        startPage();
        return getDataTable(playerCertService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:query')")
    @GetMapping("/{applicationId}")
    public AjaxResult getInfo(@PathVariable Long applicationId) {
        return success(playerCertService.selectDetail(applicationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:query')")
    @GetMapping("/{applicationId}/audit-record/list")
    public AjaxResult auditRecords(@PathVariable Long applicationId) {
        return success(playerCertService.selectAuditRecords(applicationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:audit')")
    @Log(title = "球员认证审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/start-review")
    public AjaxResult startReview(@PathVariable Long applicationId) {
        playerCertService.startReview(applicationId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:audit')")
    @Log(title = "球员认证审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/approve")
    public AjaxResult approve(@PathVariable Long applicationId, @RequestBody PlayerCertApproveReq req) {
        playerCertService.approve(applicationId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:audit')")
    @Log(title = "球员认证审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/reject")
    public AjaxResult reject(@PathVariable Long applicationId, @RequestBody PlayerCertRejectReq req) {
        playerCertService.reject(applicationId, req);
        return success();
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketPrivateFileController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketPrivateFileService;
import com.zhs.common.core.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/basket/private-file")
public class BasketPrivateFileController extends BaseController {

    @Autowired
    private IBasketPrivateFileService privateFileService;

    @PreAuthorize("@ss.hasPermi('basket:playerCert:query')")
    @GetMapping("/{fileId}")
    public void stream(@PathVariable Long fileId, HttpServletResponse response) {
        privateFileService.streamCertMaterialFile(fileId, response);
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketCaptainQualificationController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.service.IBasketCaptainQualificationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/captainQualification")
public class BasketCaptainQualificationController extends BaseController {

    @Autowired
    private IBasketCaptainQualificationService captainQualificationService;

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:list')")
    @GetMapping("/eligible-list")
    public AjaxResult eligibleList() {
        return success(captainQualificationService.selectEligibleList());
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:list')")
    @GetMapping("/list")
    public TableDataInfo list(CaptainQualificationQuery query) {
        startPage();
        return getDataTable(captainQualificationService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:query')")
    @GetMapping("/{qualificationId}")
    public AjaxResult getInfo(@PathVariable Long qualificationId) {
        return success(captainQualificationService.selectDetail(qualificationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:grant')")
    @Log(title = "队长资格", businessType = BusinessType.INSERT)
    @PostMapping("/grant")
    public AjaxResult grant(@RequestBody CaptainQualificationGrantReq req) {
        captainQualificationService.grant(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:edit')")
    @Log(title = "队长资格", businessType = BusinessType.UPDATE)
    @PutMapping("/{qualificationId}")
    public AjaxResult update(@PathVariable Long qualificationId, @RequestBody CaptainQualificationUpdateReq req) {
        captainQualificationService.update(qualificationId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:revoke')")
    @Log(title = "队长资格", businessType = BusinessType.UPDATE)
    @PostMapping("/{qualificationId}/revoke")
    public AjaxResult revoke(@PathVariable Long qualificationId, @RequestBody CaptainQualificationRevokeReq req) {
        captainQualificationService.revoke(qualificationId, req);
        return success();
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketTeamAuditController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.query.TeamAuditQuery;
import com.zhs.basket.req.TeamAuditApproveReq;
import com.zhs.basket.req.TeamAuditRejectReq;
import com.zhs.basket.service.IBasketTeamAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/teamAudit")
public class BasketTeamAuditController extends BaseController {

    @Autowired
    private IBasketTeamAuditService teamAuditService;

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeamAuditQuery query) {
        startPage();
        return getDataTable(teamAuditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:query')")
    @GetMapping("/{teamId}")
    public AjaxResult getInfo(@PathVariable Long teamId) {
        return success(teamAuditService.selectDetail(teamId));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:audit')")
    @Log(title = "球队审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{teamId}/start-review")
    public AjaxResult startReview(@PathVariable Long teamId) {
        teamAuditService.startReview(teamId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:audit')")
    @Log(title = "球队审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{teamId}/approve")
    public AjaxResult approve(@PathVariable Long teamId, @RequestBody TeamAuditApproveReq req) {
        teamAuditService.approve(teamId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:teamAudit:audit')")
    @Log(title = "球队审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{teamId}/reject")
    public AjaxResult reject(@PathVariable Long teamId, @RequestBody TeamAuditRejectReq req) {
        teamAuditService.reject(teamId, req);
        return success();
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketTournamentController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/tournament")
public class BasketTournamentController extends BaseController {

    @Autowired
    private IBasketTournamentService tournamentService;

    @PreAuthorize("@ss.hasPermi('basket:tournament:list')")
    @GetMapping("/list")
    public TableDataInfo list(TournamentQuery query) {
        startPage();
        return getDataTable(tournamentService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(tournamentService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:add')")
    @Log(title = "赛事管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TournamentCreateReq req) {
        return success(tournamentService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:edit')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody TournamentUpdateReq req) {
        tournamentService.update(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:publish')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        tournamentService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:publish')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/unpublish")
    public AjaxResult unpublish(@PathVariable Long id) {
        tournamentService.unpublish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:close')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/close")
    public AjaxResult close(@PathVariable Long id) {
        tournamentService.close(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:remove')")
    @Log(title = "赛事管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        tournamentService.delete(id);
        return success();
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketSeasonController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.req.SeasonCreateReq;
import com.zhs.basket.req.SeasonUpdateReq;
import com.zhs.basket.service.IBasketSeasonService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasketSeasonController extends BaseController {

    @Autowired
    private IBasketSeasonService seasonService;

    @PreAuthorize("@ss.hasPermi('basket:season:list')")
    @GetMapping("/basket/tournament/{tournamentId}/season/list")
    public AjaxResult listByTournament(@PathVariable Long tournamentId) {
        return success(seasonService.selectByTournamentId(tournamentId));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:query')")
    @GetMapping("/basket/season/{seasonId}")
    public AjaxResult getInfo(@PathVariable Long seasonId) {
        return success(seasonService.selectById(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:add')")
    @Log(title = "赛季管理", businessType = BusinessType.INSERT)
    @PostMapping("/basket/tournament/{tournamentId}/season")
    public AjaxResult add(@PathVariable Long tournamentId, @RequestBody SeasonCreateReq req) {
        return success(seasonService.create(tournamentId, req));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:edit')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/season/{seasonId}")
    public AjaxResult edit(@PathVariable Long seasonId, @RequestBody SeasonUpdateReq req) {
        seasonService.update(seasonId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:publish')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/publish")
    public AjaxResult publish(@PathVariable Long seasonId) {
        seasonService.publish(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:openRegistration')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/open-registration")
    public AjaxResult openRegistration(@PathVariable Long seasonId) {
        seasonService.openRegistration(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:closeRegistration')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/close-registration")
    public AjaxResult closeRegistration(@PathVariable Long seasonId) {
        seasonService.closeRegistration(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:start')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/start")
    public AjaxResult start(@PathVariable Long seasonId) {
        seasonService.start(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:finish')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/finish")
    public AjaxResult finish(@PathVariable Long seasonId) {
        seasonService.finish(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:remove')")
    @Log(title = "赛季管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/basket/season/{seasonId}")
    public AjaxResult remove(@PathVariable Long seasonId) {
        seasonService.delete(seasonId);
        return success();
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketTournamentRuleController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.req.TournamentRuleCreateReq;
import com.zhs.basket.req.TournamentRuleUpdateReq;
import com.zhs.basket.service.IBasketTournamentRuleService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasketTournamentRuleController extends BaseController {

    @Autowired
    private IBasketTournamentRuleService tournamentRuleService;

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:list')")
    @GetMapping("/basket/season/{seasonId}/rule/list")
    public AjaxResult list(@PathVariable Long seasonId) {
        return success(tournamentRuleService.selectBySeasonId(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:add')")
    @Log(title = "赛事规则", businessType = BusinessType.INSERT)
    @PostMapping("/basket/season/{seasonId}/rule")
    public AjaxResult add(@PathVariable Long seasonId, @RequestBody TournamentRuleCreateReq req) {
        return success(tournamentRuleService.create(seasonId, req));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:edit')")
    @Log(title = "赛事规则", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/tournamentRule/{ruleId}")
    public AjaxResult edit(@PathVariable Long ruleId, @RequestBody TournamentRuleUpdateReq req) {
        tournamentRuleService.update(ruleId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:remove')")
    @Log(title = "赛事规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/basket/tournamentRule/{ruleId}")
    public AjaxResult remove(@PathVariable Long ruleId) {
        tournamentRuleService.delete(ruleId);
        return success();
    }
}""",
    )

    w(
        f"{ctrl_base}/BasketRegistrationAuditController.java",
        """package com.zhs.web.controller.basket;

import com.zhs.basket.query.RegistrationAuditQuery;
import com.zhs.basket.req.RegistrationAuditApproveReq;
import com.zhs.basket.req.RegistrationAuditRejectReq;
import com.zhs.basket.service.IBasketRegistrationAuditService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/registrationAudit")
public class BasketRegistrationAuditController extends BaseController {

    @Autowired
    private IBasketRegistrationAuditService registrationAuditService;

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(RegistrationAuditQuery query) {
        startPage();
        return getDataTable(registrationAuditService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(registrationAuditService.selectDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/revalidate")
    public AjaxResult revalidate(@PathVariable Long id) {
        return success(registrationAuditService.revalidate(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/start-review")
    public AjaxResult startReview(@PathVariable Long id) {
        registrationAuditService.startReview(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody RegistrationAuditApproveReq req) {
        registrationAuditService.approve(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody RegistrationAuditRejectReq req) {
        registrationAuditService.reject(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:query')")
    @GetMapping("/{id}/audit-record/list")
    public AjaxResult auditRecords(@PathVariable Long id) {
        return success(registrationAuditService.selectAuditRecords(id));
    }
}""",
    )
