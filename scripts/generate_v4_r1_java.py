# -*- coding: utf-8 -*-
"""Generate CNPA V4 Round1 enums/entities/mappers for admin + web."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

ENUMS = {
    "SeriesCodeEnum": [
        ("LEAGUE", "League"),
        ("ARENA", "Arena"),
        ("CUP", "Cup"),
        ("OPEN", "Open"),
    ],
    "PlatformCodeEnum": [
        ("PC", "PC"),
        ("PS", "PS"),
        ("XBOX", "Xbox"),
    ],
    "RegistrationModeEnum": [
        ("TEAM", "球队报名"),
        ("PERSONAL", "个人报名"),
    ],
    "FormatTypeEnum": [
        ("REGULAR_ROUND_ROBIN", "常规赛单循环"),
        ("REGULAR_PLAYOFF", "常规赛+季后赛"),
        ("GROUP_KNOCKOUT", "小组赛+淘汰赛"),
        ("KNOCKOUT", "单淘汰"),
    ],
    "PublishStatusEnum": [
        ("DRAFT", "草稿"),
        ("PUBLISHED", "已发布"),
        ("OFFLINE", "已下架"),
    ],
    "SeasonStatusEnum": [
        ("NOT_STARTED", "未开始"),
        ("REGISTRATION_OPEN", "报名中"),
        ("REGISTRATION_CLOSED", "报名截止"),
        ("PLAYING", "比赛中"),
        ("FINISHED", "已结束"),
        ("ARCHIVED", "已归档"),
    ],
    "DivisionStatusEnum": [
        ("DRAFT", "草稿"),
        ("REGISTRATION_OPEN", "报名中"),
        ("REGISTRATION_CLOSED", "报名截止"),
        ("SCHEDULED", "已排赛"),
        ("PLAYING", "比赛中"),
        ("FINISHED", "已结束"),
        ("ARCHIVED", "已归档"),
    ],
    "CaptainGrantStatusEnum": [
        ("ACTIVE", "有效"),
        ("REVOKED", "已撤销"),
        ("EXPIRED", "已过期"),
    ],
    "RegistrationStatusEnum": [
        ("DRAFT", "草稿"),
        ("PENDING_REVIEW", "待审核"),
        ("APPROVED", "已通过"),
        ("REJECTED", "已驳回"),
        ("WITHDRAWN", "已撤回"),
        ("CANCELLED", "已取消"),
    ],
    "AuditResultEnum": [
        ("APPROVED", "通过"),
        ("REJECTED", "驳回"),
    ],
    "MatchStatusEnum": [
        ("DRAFT", "草稿"),
        ("SCHEDULED", "已编排"),
        ("PLAYING", "比赛中"),
        ("FINISHED", "已结束"),
        ("CANCELLED", "已取消"),
        ("FORFEITED", "弃权"),
    ],
    "MatchPublishStatusEnum": [
        ("DRAFT", "草稿"),
        ("PUBLISHED", "已发布"),
        ("OFFLINE", "已下架"),
    ],
    "SubmissionStatusEnum": [
        ("DRAFT", "草稿"),
        ("PENDING_REVIEW", "待审核"),
        ("APPROVED", "已通过"),
        ("REJECTED", "已驳回"),
        ("SUPERSEDED", "已替代"),
        ("CANCELLED", "已取消"),
    ],
    "RankTypeEnum": [
        ("TEAM_STANDING", "球队积分榜"),
        ("TEAM_SCORE", "球队得分榜"),
        ("PLAYER_SCORE", "球员得分榜"),
        ("PLAYER_ASSIST", "球员助攻榜"),
        ("PLAYER_REBOUND", "球员篮板榜"),
        ("PLAYER_STEAL", "球员抢断榜"),
        ("PLAYER_BLOCK", "球员盖帽榜"),
    ],
}

# table -> (ClassName, fields[(javaName, javaType)])
TABLES = {
    "basket_series": (
        "BasketSeries",
        [
            ("id", "Long"),
            ("seriesCode", "String"),
            ("seriesName", "String"),
            ("seriesDesc", "String"),
            ("defaultFormatType", "String"),
            ("iconFileId", "Long"),
            ("coverFileId", "Long"),
            ("bannerFileId", "Long"),
            ("sortOrder", "Integer"),
            ("enabled", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_season": (
        "BasketSeason",
        [
            ("id", "Long"),
            ("seriesId", "Long"),
            ("seasonYear", "Integer"),
            ("seasonName", "String"),
            ("displayTitle", "String"),
            ("description", "String"),
            ("coverFileId", "Long"),
            ("bannerFileId", "Long"),
            ("registrationMode", "String"),
            ("formatType", "String"),
            ("registrationStartTime", "Date"),
            ("registrationEndTime", "Date"),
            ("matchStartTime", "Date"),
            ("matchEndTime", "Date"),
            ("publishStatus", "String"),
            ("seasonStatus", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_division": (
        "BasketDivision",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("platformCode", "String"),
            ("divisionName", "String"),
            ("registrationMode", "String"),
            ("formatType", "String"),
            ("maxTeamCount", "Integer"),
            ("maxPersonCount", "Integer"),
            ("lineupMinCount", "Integer"),
            ("lineupMaxCount", "Integer"),
            ("registrationStartTime", "Date"),
            ("registrationEndTime", "Date"),
            ("matchStartTime", "Date"),
            ("matchEndTime", "Date"),
            ("divisionStatus", "String"),
            ("sortOrder", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_captain_grant": (
        "BasketCaptainGrant",
        [
            ("id", "Long"),
            ("divisionId", "Long"),
            ("seasonId", "Long"),
            ("teamId", "Long"),
            ("playerId", "Long"),
            ("memberId", "Long"),
            ("grantStatus", "String"),
            ("validFrom", "Date"),
            ("validTo", "Date"),
            ("grantedBy", "Long"),
            ("remark", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_registration": (
        "BasketRegistration",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("registrationMode", "String"),
            ("teamId", "Long"),
            ("captainPlayerId", "Long"),
            ("captainMemberId", "Long"),
            ("registrationStatus", "String"),
            ("lineupCount", "Integer"),
            ("submitTime", "Date"),
            ("approveTime", "Date"),
            ("rejectReason", "String"),
            ("withdrawTime", "Date"),
            ("auditUserId", "Long"),
            ("auditRemark", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_registration_player": (
        "BasketRegistrationPlayer",
        [
            ("id", "Long"),
            ("registrationId", "Long"),
            ("divisionId", "Long"),
            ("seasonId", "Long"),
            ("teamId", "Long"),
            ("playerId", "Long"),
            ("memberId", "Long"),
            ("platformIdentityId", "Long"),
            ("platformCode", "String"),
            ("gameIdSnapshot", "String"),
            ("playerNameSnapshot", "String"),
            ("position", "String"),
            ("jerseyNo", "String"),
            ("isCaptain", "Integer"),
            ("isStarter", "Integer"),
            ("sortOrder", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_registration_audit": (
        "BasketRegistrationAudit",
        [
            ("id", "Long"),
            ("registrationId", "Long"),
            ("divisionId", "Long"),
            ("seasonId", "Long"),
            ("auditAction", "String"),
            ("auditResult", "String"),
            ("beforeStatus", "String"),
            ("afterStatus", "String"),
            ("auditUserId", "Long"),
            ("auditUserName", "String"),
            ("auditTime", "Date"),
            ("auditRemark", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_event_team": (
        "BasketEventTeam",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("registrationId", "Long"),
            ("sourceTeamId", "Long"),
            ("teamNameSnapshot", "String"),
            ("teamLogoFileId", "Long"),
            ("teamShortName", "String"),
            ("captainPlayerId", "Long"),
            ("captainMemberId", "Long"),
            ("eventTeamNo", "String"),
            ("eventTeamStatus", "String"),
            ("platformCode", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_event_player": (
        "BasketEventPlayer",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("sourcePlayerId", "Long"),
            ("memberId", "Long"),
            ("platformIdentityId", "Long"),
            ("platformCode", "String"),
            ("gameIdSnapshot", "String"),
            ("playerNameSnapshot", "String"),
            ("avatarFileId", "Long"),
            ("position", "String"),
            ("jerseyNo", "String"),
            ("isCaptain", "Integer"),
            ("isStarter", "Integer"),
            ("eventPlayerStatus", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_schedule_batch": (
        "BasketScheduleBatch",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("batchNo", "String"),
            ("formatType", "String"),
            ("ruleCode", "String"),
            ("teamCount", "Integer"),
            ("matchCount", "Integer"),
            ("generateType", "String"),
            ("generateStatus", "String"),
            ("publishStatus", "String"),
            ("generateTime", "Date"),
            ("publishTime", "Date"),
            ("generatedBy", "Long"),
            ("publishedBy", "Long"),
            ("remark", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match": (
        "BasketMatch",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("scheduleBatchId", "Long"),
            ("roundNo", "Integer"),
            ("matchNo", "String"),
            ("stageCode", "String"),
            ("matchTime", "Date"),
            ("homeEventTeamId", "Long"),
            ("awayEventTeamId", "Long"),
            ("homeScore", "Integer"),
            ("awayScore", "Integer"),
            ("winnerEventTeamId", "Long"),
            ("matchStatus", "String"),
            ("publishStatus", "String"),
            ("platformCode", "String"),
            ("venueText", "String"),
            ("remark", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_period_score": (
        "BasketMatchPeriodScore",
        [
            ("id", "Long"),
            ("matchId", "Long"),
            ("divisionId", "Long"),
            ("periodNo", "Integer"),
            ("homeScore", "Integer"),
            ("awayScore", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_submission": (
        "BasketMatchSubmission",
        [
            ("id", "Long"),
            ("matchId", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("submitEventTeamId", "Long"),
            ("opponentEventTeamId", "Long"),
            ("submitEventPlayerId", "Long"),
            ("submitMemberId", "Long"),
            ("submitterRole", "String"),
            ("submissionStatus", "String"),
            ("versionNo", "Integer"),
            ("submitTime", "Date"),
            ("auditUserId", "Long"),
            ("auditUserName", "String"),
            ("auditTime", "Date"),
            ("auditRemark", "String"),
            ("rejectReason", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_submission_team_stat": (
        "BasketMatchSubmissionTeamStat",
        [
            ("id", "Long"),
            ("submissionId", "Long"),
            ("matchId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("score", "Integer"),
            ("fieldGoalMade", "Integer"),
            ("fieldGoalAttempt", "Integer"),
            ("threePointMade", "Integer"),
            ("threePointAttempt", "Integer"),
            ("freeThrowMade", "Integer"),
            ("freeThrowAttempt", "Integer"),
            ("rebound", "Integer"),
            ("assist", "Integer"),
            ("steal", "Integer"),
            ("block", "Integer"),
            ("turnover", "Integer"),
            ("foul", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_submission_player_stat": (
        "BasketMatchSubmissionPlayerStat",
        [
            ("id", "Long"),
            ("submissionId", "Long"),
            ("matchId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("eventPlayerId", "Long"),
            ("points", "Integer"),
            ("rebound", "Integer"),
            ("assist", "Integer"),
            ("steal", "Integer"),
            ("block", "Integer"),
            ("turnover", "Integer"),
            ("foul", "Integer"),
            ("minutes", "BigDecimal"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_submission_material": (
        "BasketMatchSubmissionMaterial",
        [
            ("id", "Long"),
            ("submissionId", "Long"),
            ("matchId", "Long"),
            ("divisionId", "Long"),
            ("materialType", "String"),
            ("fileId", "Long"),
            ("fileName", "String"),
            ("remark", "String"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_team_stat": (
        "BasketMatchTeamStat",
        [
            ("id", "Long"),
            ("matchId", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("opponentEventTeamId", "Long"),
            ("isWinner", "Integer"),
            ("score", "Integer"),
            ("pointsFor", "Integer"),
            ("pointsAgainst", "Integer"),
            ("fieldGoalMade", "Integer"),
            ("fieldGoalAttempt", "Integer"),
            ("threePointMade", "Integer"),
            ("threePointAttempt", "Integer"),
            ("freeThrowMade", "Integer"),
            ("freeThrowAttempt", "Integer"),
            ("rebound", "Integer"),
            ("assist", "Integer"),
            ("steal", "Integer"),
            ("block", "Integer"),
            ("turnover", "Integer"),
            ("foul", "Integer"),
            ("sourceSubmissionId", "Long"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_match_player_stat": (
        "BasketMatchPlayerStat",
        [
            ("id", "Long"),
            ("matchId", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("eventPlayerId", "Long"),
            ("points", "Integer"),
            ("score", "Integer"),
            ("rebound", "Integer"),
            ("assist", "Integer"),
            ("steal", "Integer"),
            ("block", "Integer"),
            ("turnover", "Integer"),
            ("foul", "Integer"),
            ("minutes", "BigDecimal"),
            ("sourceSubmissionId", "Long"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_standing": (
        "BasketStanding",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("rankNo", "Integer"),
            ("winCount", "Integer"),
            ("loseCount", "Integer"),
            ("drawCount", "Integer"),
            ("pointFor", "Integer"),
            ("pointAgainst", "Integer"),
            ("pointDiff", "Integer"),
            ("standingPoints", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_team_stat": (
        "BasketTeamStat",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("matchCount", "Integer"),
            ("winCount", "Integer"),
            ("loseCount", "Integer"),
            ("pointsFor", "Integer"),
            ("pointsAgainst", "Integer"),
            ("avgPointsFor", "BigDecimal"),
            ("avgPointsAgainst", "BigDecimal"),
            ("rebound", "Integer"),
            ("assist", "Integer"),
            ("steal", "Integer"),
            ("block", "Integer"),
            ("turnover", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_player_stat": (
        "BasketPlayerStat",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("eventTeamId", "Long"),
            ("eventPlayerId", "Long"),
            ("matchCount", "Integer"),
            ("points", "Integer"),
            ("avgPoints", "BigDecimal"),
            ("rebound", "Integer"),
            ("avgRebound", "BigDecimal"),
            ("assist", "Integer"),
            ("avgAssist", "BigDecimal"),
            ("steal", "Integer"),
            ("block", "Integer"),
            ("turnover", "Integer"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
    "basket_rank": (
        "BasketRank",
        [
            ("id", "Long"),
            ("seasonId", "Long"),
            ("divisionId", "Long"),
            ("rankType", "String"),
            ("rankNo", "Integer"),
            ("targetType", "String"),
            ("eventTeamId", "Long"),
            ("eventPlayerId", "Long"),
            ("rankValue", "BigDecimal"),
            ("createTime", "Date"),
            ("updateTime", "Date"),
            ("isDelete", "Integer"),
        ],
    ),
}


def camel_to_snake(name: str) -> str:
    out = []
    for i, c in enumerate(name):
        if c.isupper() and i:
            out.append("_")
            out.append(c.lower())
        else:
            out.append(c.lower())
    return "".join(out)


def enum_java(package: str, name: str, items):
    lines = [
        f"package {package};",
        "",
        "/**",
        f" * CNPA 赛事域 V4 枚举：{name}",
        " * 存储方式：VARCHAR 字符串 code，与旧整型枚举隔离。",
        " */",
        f"public enum {name} {{",
    ]
    for i, (code, desc) in enumerate(items):
        suffix = "," if i < len(items) - 1 else ";"
        lines.append(f'    {code}("{code}", "{desc}"){suffix}')
    lines += [
        "",
        "    private final String code;",
        "    private final String description;",
        "",
        f"    {name}(String code, String description) {{",
        "        this.code = code;",
        "        this.description = description;",
        "    }",
        "",
        "    public String getCode() {",
        "        return code;",
        "    }",
        "",
        "    public String getDescription() {",
        "        return description;",
        "    }",
        "",
        f"    public static {name} fromCode(String code) {{",
        "        if (code == null || code.isEmpty()) {",
        "            return null;",
        "        }",
        f"        for ({name} value : values()) {{",
        "            if (value.code.equals(code)) {",
        "                return value;",
        "            }",
        "        }",
        f'        throw new IllegalArgumentException("未知{name}: " + code);',
        "    }",
        "}",
        "",
    ]
    return "\n".join(lines)


def admin_entity(class_name: str, fields):
    needs_bd = any(t == "BigDecimal" for _, t in fields)
    imports = ["package com.zhs.basket.domain.v4;", "", "import lombok.Data;", "import java.util.Date;"]
    if needs_bd:
        imports.append("import java.math.BigDecimal;")
    lines = imports + ["", "/**", f" * CNPA 赛事域 V4 实体：{class_name}", " */", "@Data", f"public class {class_name} {{"]
    for fname, ftype in fields:
        lines.append(f"    private {ftype} {fname};")
    lines += ["}", ""]
    return "\n".join(lines)


def web_entity(class_name: str, table: str, fields):
    needs_bd = any(t == "BigDecimal" for _, t in fields)
    lines = [
        "package com.zhs.model.v4;",
        "",
        "import com.baomidou.mybatisplus.annotation.IdType;",
        "import com.baomidou.mybatisplus.annotation.TableId;",
        "import com.baomidou.mybatisplus.annotation.TableName;",
        "import com.fasterxml.jackson.databind.annotation.JsonSerialize;",
        "import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;",
        "import io.swagger.annotations.ApiModel;",
        "import lombok.Data;",
        "",
        "import java.io.Serializable;",
        "import java.util.Date;",
    ]
    if needs_bd:
        lines.append("import java.math.BigDecimal;")
    lines += [
        "",
        "/**",
        f" * CNPA 赛事域 V4 实体：{class_name}",
        " */",
        "@Data",
        "@ApiModel",
        f'@TableName("{table}")',
        f"public class {class_name} implements Serializable {{",
        "",
    ]
    for fname, ftype in fields:
        if fname == "id":
            lines += [
                "    @JsonSerialize(using = ToStringSerializer.class)",
                "    @TableId(value = \"id\", type = IdType.AUTO)",
                f"    private {ftype} {fname};",
                "",
            ]
        elif ftype == "Long":
            lines += [
                "    @JsonSerialize(using = ToStringSerializer.class)",
                f"    private {ftype} {fname};",
                "",
            ]
        else:
            lines += [f"    private {ftype} {fname};", ""]
    lines += ["}", ""]
    return "\n".join(lines)


def admin_mapper(class_name: str):
    return "\n".join(
        [
            "package com.zhs.basket.mapper.v4;",
            "",
            f"import com.zhs.basket.domain.v4.{class_name};",
            "import org.apache.ibatis.annotations.Param;",
            "",
            "import java.util.List;",
            "",
            "/**",
            f" * CNPA 赛事域 V4 Mapper：{class_name}",
            " */",
            f"public interface {class_name}Mapper {{",
            f"    {class_name} selectById(@Param(\"id\") Long id);",
            f"    List<{class_name}> selectList({class_name} query);",
            f"    int insert({class_name} entity);",
            f"    int updateById({class_name} entity);",
            "    int softDelete(@Param(\"id\") Long id);",
            "}",
            "",
        ]
    )


def web_dao(class_name: str):
    return "\n".join(
        [
            "package com.zhs.dao.v4;",
            "",
            f"import com.zhs.model.v4.{class_name};",
            "import com.baomidou.mybatisplus.core.mapper.BaseMapper;",
            "import org.apache.ibatis.annotations.Mapper;",
            "import org.springframework.stereotype.Repository;",
            "",
            "/**",
            f" * CNPA 赛事域 V4 Dao：{class_name}",
            " */",
            "@Mapper",
            "@Repository",
            f"public interface {class_name}Dao extends BaseMapper<{class_name}> {{",
            "}",
            "",
        ]
    )


def admin_xml(table: str, class_name: str, fields):
    fqcn = f"com.zhs.basket.domain.v4.{class_name}"
    cols = [camel_to_snake(f) for f, _ in fields]
    result_items = []
    for fname, _ in fields:
        col = camel_to_snake(fname)
        if fname == "id":
            result_items.append(f'        <id property="{fname}" column="{col}" />')
        else:
            result_items.append(f'        <result property="{fname}" column="{col}" />')
    insert_cols = [c for c in cols if c != "id"]
    insert_vals = [f"#{{{f}}}" for f, _ in fields if f != "id"]
    set_lines = []
    for fname, _ in fields:
        if fname in ("id", "createTime", "isDelete"):
            continue
        col = camel_to_snake(fname)
        set_lines.append(f'            <if test="{fname} != null">{col} = #{{{fname}}},</if>')
    return "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8" ?>',
            '<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">',
            f'<mapper namespace="com.zhs.basket.mapper.v4.{class_name}Mapper">',
            "",
            f'    <resultMap type="{fqcn}" id="{class_name}Result">',
            *result_items,
            "    </resultMap>",
            "",
            f'    <sql id="BaseColumns">{", ".join(cols)}</sql>',
            "",
            f'    <select id="selectById" resultMap="{class_name}Result">',
            f"        select <include refid=\"BaseColumns\" /> from {table}",
            "        where id = #{id} and is_delete = 0",
            "    </select>",
            "",
            f'    <select id="selectList" resultMap="{class_name}Result">',
            f"        select <include refid=\"BaseColumns\" /> from {table}",
            "        where is_delete = 0",
            "        order by id desc",
            "    </select>",
            "",
            '    <insert id="insert" useGeneratedKeys="true" keyProperty="id">',
            f"        insert into {table} (",
            "            " + ", ".join(insert_cols),
            "        ) values (",
            "            " + ", ".join(insert_vals),
            "        )",
            "    </insert>",
            "",
            '    <update id="updateById">',
            f"        update {table}",
            "        <set>",
            *set_lines,
            "            update_time = now()",
            "        </set>",
            "        where id = #{id} and is_delete = 0",
            "    </update>",
            "",
            '    <update id="softDelete">',
            f"        update {table}",
            "        set is_delete = 1, update_time = now()",
            "        where id = #{id} and is_delete = 0",
            "    </update>",
            "",
            "</mapper>",
            "",
        ]
    )


def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)}")


def main():
    admin_enum_pkg = "com.zhs.basket.enums.v4"
    web_enum_pkg = "com.zhs.enums.v4"
    for name, items in ENUMS.items():
        write(
            ROOT / f"basketball-admin/src/main/java/com/zhs/basket/enums/v4/{name}.java",
            enum_java(admin_enum_pkg, name, items),
        )
        write(
            ROOT / f"basketball-web/src/main/java/com/zhs/enums/v4/{name}.java",
            enum_java(web_enum_pkg, name, items),
        )

    for table, (class_name, fields) in TABLES.items():
        write(
            ROOT / f"basketball-admin/src/main/java/com/zhs/basket/domain/v4/{class_name}.java",
            admin_entity(class_name, fields),
        )
        write(
            ROOT / f"basketball-admin/src/main/java/com/zhs/basket/mapper/v4/{class_name}Mapper.java",
            admin_mapper(class_name),
        )
        write(
            ROOT / f"basketball-admin/src/main/resources/mapper/basket/v4/{class_name}Mapper.xml",
            admin_xml(table, class_name, fields),
        )
        write(
            ROOT / f"basketball-web/src/main/java/com/zhs/model/v4/{class_name}.java",
            web_entity(class_name, table, fields),
        )
        write(
            ROOT / f"basketball-web/src/main/java/com/zhs/dao/v4/{class_name}Dao.java",
            web_dao(class_name),
        )


if __name__ == "__main__":
    main()
