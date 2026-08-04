package com.zhs.basket.constant;

/**
 * CNPA 赛事域 V4 后台权限。
 */
public final class BasketEventV4Perms {

    private BasketEventV4Perms() {}

    public static final String SERIES_LIST = "basket:v4Series:list";
    public static final String SERIES_QUERY = "basket:v4Series:query";
    public static final String SERIES_EDIT = "basket:v4Series:edit";

    public static final String SEASON_LIST = "basket:v4Season:list";
    public static final String SEASON_QUERY = "basket:v4Season:query";
    public static final String SEASON_ADD = "basket:v4Season:add";
    public static final String SEASON_EDIT = "basket:v4Season:edit";
    public static final String SEASON_PUBLISH = "basket:v4Season:publish";
    public static final String SEASON_OFFLINE = "basket:v4Season:offline";
    public static final String SEASON_REMOVE = "basket:v4Season:remove";

    public static final String DIVISION_LIST = "basket:v4Division:list";
    public static final String DIVISION_QUERY = "basket:v4Division:query";
    public static final String DIVISION_ADD = "basket:v4Division:add";
    public static final String DIVISION_EDIT = "basket:v4Division:edit";
    public static final String DIVISION_OPEN = "basket:v4Division:openRegistration";
    public static final String DIVISION_CLOSE = "basket:v4Division:closeRegistration";
    public static final String DIVISION_REMOVE = "basket:v4Division:remove";

    public static final String CAPTAIN_GRANT_LIST = "basket:v4CaptainGrant:list";
    public static final String CAPTAIN_GRANT_QUERY = "basket:v4CaptainGrant:query";
    public static final String CAPTAIN_GRANT_ADD = "basket:v4CaptainGrant:add";
    public static final String CAPTAIN_GRANT_REVOKE = "basket:v4CaptainGrant:revoke";

    public static final String REGISTRATION_AUDIT_LIST = "basket:v4RegistrationAudit:list";
    public static final String REGISTRATION_AUDIT_QUERY = "basket:v4RegistrationAudit:query";
    public static final String REGISTRATION_AUDIT_APPROVE = "basket:v4RegistrationAudit:approve";
    public static final String REGISTRATION_AUDIT_REJECT = "basket:v4RegistrationAudit:reject";

    public static final String EVENT_TEAM_LIST = "basket:v4EventTeam:list";
    public static final String EVENT_TEAM_QUERY = "basket:v4EventTeam:query";

    public static final String EVENT_PLAYER_LIST = "basket:v4EventPlayer:list";

    public static final String SCHEDULE_BATCH_LIST = "basket:v4ScheduleBatch:list";
    public static final String SCHEDULE_BATCH_QUERY = "basket:v4ScheduleBatch:query";
    public static final String SCHEDULE_BATCH_PUBLISH = "basket:v4ScheduleBatch:publish";
    public static final String SCHEDULE_BATCH_CANCEL = "basket:v4ScheduleBatch:cancel";

    public static final String MATCH_LIST = "basket:v4Match:list";
    public static final String MATCH_QUERY = "basket:v4Match:query";
    public static final String MATCH_EDIT = "basket:v4Match:edit";
    public static final String MATCH_REMOVE = "basket:v4Match:remove";

    public static final String MATCH_SUBMISSION_AUDIT_LIST = "basket:v4MatchSubmissionAudit:list";
    public static final String MATCH_SUBMISSION_AUDIT_QUERY = "basket:v4MatchSubmissionAudit:query";
    public static final String MATCH_SUBMISSION_AUDIT_APPROVE = "basket:v4MatchSubmissionAudit:approve";
    public static final String MATCH_SUBMISSION_AUDIT_REJECT = "basket:v4MatchSubmissionAudit:reject";

    public static final String V4_STATS_QUERY = "basket:v4Stats:query";

    public static final String FORMAT_RULE_VIEW = "basket:v4FormatRule:view";
    public static final String FORMAT_RULE_EDIT = "basket:v4FormatRule:edit";
    public static final String FORMAT_RULE_LOCK = "basket:v4FormatRule:lock";

    public static final String PERSONAL_REG_LIST = "basket:v4PersonalReg:list";
    public static final String PERSONAL_REG_AUDIT = "basket:v4PersonalReg:audit";

    public static final String PERSONAL_FORMATION_LIST = "basket:v4PersonalFormation:list";
    public static final String PERSONAL_FORMATION_EDIT = "basket:v4PersonalFormation:edit";
    public static final String PERSONAL_FORMATION_CONFIRM = "basket:v4PersonalFormation:confirm";

    public static final String GROUPING_LIST = "basket:v4Grouping:list";
    public static final String GROUPING_EDIT = "basket:v4Grouping:edit";
    public static final String GROUPING_LOCK = "basket:v4Grouping:lock";

    public static final String STAGE_LIST = "basket:v4Stage:list";
    public static final String STAGE_GENERATE = "basket:v4Stage:generate";
    public static final String STAGE_PUBLISH = "basket:v4Stage:publish";
    public static final String STAGE_QUALIFICATION_PREVIEW = "basket:v4Stage:qualificationPreview";
    public static final String STAGE_QUALIFICATION_CONFIRM = "basket:v4Stage:qualificationConfirm";
    public static final String STANDING_VIEW = "basket:v4Standing:view";
    public static final String STANDING_MANUAL_RANK = "basket:v4Standing:manualRank";

    public static final String STAGE_INITIALIZE = "basket:v4Stage:initialize";
    public static final String STAGE_QUALIFICATION_CANCEL = "basket:v4Stage:qualificationCancel";
}
