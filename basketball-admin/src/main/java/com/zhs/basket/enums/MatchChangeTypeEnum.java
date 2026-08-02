package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum MatchChangeTypeEnum {
    CREATE("CREATE", "创建"),
    SCHEDULE("SCHEDULE", "编排"),
    POSTPONE("POSTPONE", "延期"),
    CANCEL("CANCEL", "取消"),
    START("START", "开始"),
    FINISH("FINISH", "结束"),
    REOPEN("REOPEN", "重开"),
    LINEUP_SAVE("LINEUP_SAVE", "保存出场名单"),
    LINEUP_CONFIRM("LINEUP_CONFIRM", "确认出场名单"),
    SCORE_SAVE("SCORE_SAVE", "保存比分"),
    SCORE_SUBMIT("SCORE_SUBMIT", "提交比分"),
    SCORE_CONFIRM("SCORE_CONFIRM", "确认比分"),
    STAT_SAVE("STAT_SAVE", "保存技术统计"),
    STAT_SUBMIT("STAT_SUBMIT", "提交技术统计"),
    RESULT_CONFIRM("RESULT_CONFIRM", "确认比赛结果"),
    RESULT_REOPEN("RESULT_REOPEN", "重开比赛结果"),
    AUTO_FINISH_BY_SCHEDULE("AUTO_FINISH_BY_SCHEDULE", "计划时间自动结束");

    private final String code;
    private final String description;

    MatchChangeTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }

    public static MatchChangeTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) throw new ServiceException("未知变更类型");
        for (MatchChangeTypeEnum v : values()) {
            if (v.code.equalsIgnoreCase(code.trim())) return v;
        }
        throw new ServiceException("未知变更类型");
    }
}
