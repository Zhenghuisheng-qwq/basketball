package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.domain.v4.BasketLeaderboardPolicy;
import com.zhs.basket.support.v4.leaderboard.LeaderboardApplicationService;
import com.zhs.basket.support.v4.leaderboard.LeaderboardConfig;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/v4/division/{divisionId}/leaderboard")
public class BasketV4LeaderboardController extends BaseController {

    @Autowired
    private LeaderboardApplicationService leaderboardApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_VIEW + "')")
    @GetMapping("/scopes")
    public AjaxResult scopes(@PathVariable Long divisionId) {
        return success(leaderboardApplicationService.scopes(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_EDIT + "')")
    @PostMapping("/policies/drafts")
    public AjaxResult createDraft(@PathVariable Long divisionId, @RequestBody DraftCreate body) {
        return success(leaderboardApplicationService.createDraft(divisionId, body == null ? null : body.scopeKey));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_EDIT + "')")
    @PutMapping("/policies/drafts/{draftId}")
    public AjaxResult saveDraft(@PathVariable Long divisionId, @PathVariable Long draftId, @RequestBody DraftSave body) {
        return success(leaderboardApplicationService.saveDraft(divisionId, draftId, body.expectedLock, body.config));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_EDIT + "')")
    @PostMapping("/policies/drafts/{draftId}/load-template")
    public AjaxResult loadTemplate(@PathVariable Long divisionId, @PathVariable Long draftId, @RequestBody LockBody body) {
        return success(leaderboardApplicationService.loadTemplate(divisionId, draftId, body.expectedLock));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_VALIDATE + "')")
    @PostMapping("/policies/drafts/{draftId}/validate")
    public AjaxResult validate(@PathVariable Long divisionId, @PathVariable Long draftId, @RequestBody LockBody body) {
        return success(leaderboardApplicationService.validateDraft(divisionId, draftId, body.expectedLock));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_EDIT + "')")
    @PostMapping("/policies/drafts/{draftId}/reopen")
    public AjaxResult reopen(@PathVariable Long divisionId, @PathVariable Long draftId, @RequestBody LockBody body) {
        return success(leaderboardApplicationService.reopenDraft(divisionId, draftId, body.expectedLock));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_VIEW + "')")
    @PostMapping("/policies/drafts/{draftId}/preview")
    public AjaxResult preview(@PathVariable Long divisionId, @PathVariable Long draftId) {
        return success(leaderboardApplicationService.preview(divisionId, draftId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_VIEW + "')")
    @GetMapping("/eligibility")
    public AjaxResult eligibility(@PathVariable Long divisionId,
                                  @RequestParam Long runId,
                                  @RequestParam(required = false) String subjectType,
                                  @RequestParam(required = false) String status) {
        return success(leaderboardApplicationService.eligibility(divisionId, runId, subjectType, status));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_PUBLISH + "')")
    @PostMapping("/policies/drafts/{draftId}/publish")
    public AjaxResult publish(@PathVariable Long divisionId, @PathVariable Long draftId, @RequestBody PublishBody body) {
        return success(leaderboardApplicationService.publish(divisionId, draftId, body.expectedActiveVersion, body.expectedLock, body.reason));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_RECALC + "')")
    @PostMapping("/recalculate")
    public AjaxResult recalculate(@PathVariable Long divisionId, @RequestParam(required = false) Long stageId) {
        leaderboardApplicationService.recalculate(divisionId, stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_ARCHIVE + "')")
    @PostMapping("/archive")
    public AjaxResult archive(@PathVariable Long divisionId, @RequestBody ArchiveBody body) {
        BasketLeaderboardPolicy policy = leaderboardApplicationService.archive(divisionId, body.scopeKey, body.expectedLock, body.reason);
        return success(policy);
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_VIEW + "')")
    @GetMapping("/runs")
    public AjaxResult runs(@PathVariable Long divisionId, @RequestParam(required = false) String scopeKey) {
        return success(leaderboardApplicationService.runs(divisionId, scopeKey));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.LEADERBOARD_VIEW + "')")
    @GetMapping("/audits")
    public AjaxResult audits(@PathVariable Long divisionId) {
        return success(leaderboardApplicationService.audits(divisionId));
    }

    public static class DraftCreate {
        public String scopeKey;
    }

    public static class DraftSave {
        public Integer expectedLock;
        public LeaderboardConfig config;
    }

    public static class LockBody {
        public Integer expectedLock;
    }

    public static class PublishBody {
        public Integer expectedLock;
        public Integer expectedActiveVersion;
        public String reason;
    }

    public static class ArchiveBody {
        public String scopeKey;
        public Integer expectedLock;
        public String reason;
    }
}
