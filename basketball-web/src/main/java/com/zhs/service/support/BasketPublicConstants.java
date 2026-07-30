package com.zhs.service.support;

/**
 * 官网公开查询常量。
 */
public final class BasketPublicConstants {

    public static final int NOT_DELETED = 0;
    public static final int PUBLIC = 1;
    public static final int MATCH_SCHEDULED = 1;
    public static final int MATCH_FINISHED = 3;
    public static final int PUBLISH_PUBLISHED = 1;
    public static final int DATA_DRAFT = 0;
    public static final int DATA_SUBMITTED = 1;
    public static final int DATA_CONFIRMED = 2;
    public static final int DATA_REOPENED = 3;
    public static final int DATA_REJECTED = 4;
    public static final int PROFILE_VISIBLE = 1;
    public static final int TEAM_AUDIT_APPROVED = 1;
    public static final int TEAM_STATUS_ACTIVE = 1;
    public static final int MEMBERSHIP_CURRENT = 1;
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;
    public static final int HOME_LIMIT = 10;
    public static final int HOME_ANNOUNCEMENT_LIMIT = 5;

    /** honor_record.display_status / announcement/banner/partner 已发布 */
    public static final int PUBLISHED = 1;

    public static final String HONOR_SCOPE_TOURNAMENT = "TOURNAMENT";
    public static final String HONOR_SCOPE_PLAYER = "PLAYER";
    public static final String HONOR_SCOPE_TEAM = "TEAM";
    public static final String HONOR_CODE_CHAMPION = "CHAMPION";
    public static final String HONOR_CODE_RUNNER_UP = "RUNNER_UP";

    private BasketPublicConstants() {
    }
}
