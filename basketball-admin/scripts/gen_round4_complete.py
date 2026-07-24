# -*- coding: utf-8 -*-
"""Generate ALL remaining Round 4 CNPA Admin basket backend files."""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
JAVA_BASKET = os.path.join(ROOT, "src", "main", "java", "com", "zhs", "basket")
JAVA_CTRL = os.path.join(ROOT, "src", "main", "java", "com", "zhs", "web", "controller", "basket")
XML_M = os.path.join(ROOT, "src", "main", "resources", "mapper", "basket")
SQL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "sql")
WEB_ENUM = os.path.join(os.path.dirname(__file__), "..", "..", "basketball-web", "src", "main", "java", "com", "zhs", "enums")
APP_YML = os.path.join(ROOT, "src", "main", "resources", "application.yml")

CREATED: list[str] = []


def w(rel_path: str, content: str, base: str = ROOT) -> None:
    path = os.path.join(base, rel_path.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    text = content if content.endswith("\n") else content + "\n"
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    CREATED.append(path)
    print("W", path)


def camel_to_snake(name: str) -> str:
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


TABLE = {
    "BasketPlayerCertApplication": "basket_player_cert_application",
    "BasketPlayerCertAudit": "basket_player_cert_audit",
    "BasketPlayerCertMaterial": "basket_player_cert_material",
    "BasketPlayerProfile": "basket_player_profile",
    "BasketCaptainQualification": "basket_captain_qualification",
    "BasketTeam": "basket_team",
    "BasketTeamAuditRecord": "basket_team_audit_record",
    "BasketTeamMember": "basket_team_member",
    "BasketTournament": "basket_tournament",
    "BasketTournamentSeason": "basket_tournament_season",
    "BasketTournamentRule": "basket_tournament_rule",
    "BasketTournamentRegistration": "basket_tournament_registration",
    "BasketTournamentRegistrationPlayer": "basket_tournament_registration_player",
    "BasketTournamentRegistrationAudit": "basket_tournament_registration_audit",
    "BasketFileAsset": "basket_file_asset",
    "BasketMessageTask": "basket_message_task",
    "BasketMessageReceiver": "basket_message_receiver",
    "BasketAccountSecurityLog": "basket_account_security_log",
    "BasketMemberAccount": "basket_member_account",
    "BasketDisciplineRecord": "basket_discipline_record",
}

FIELDS = {
    "BasketPlayerCertApplication": [
        "id", "applicationNo", "memberId", "playerId", "applicationType", "realName", "idType",
        "idNoCipher", "idNoMasked", "gameTerminal", "gameAccount", "gameNickname", "positionCode",
        "contactMobile", "applicationStatus", "submittedTime", "reviewedTime", "currentAuditorId",
        "reviewRemark", "createTime", "updateTime", "isDelete",
    ],
    "BasketPlayerCertAudit": [
        "id", "applicationId", "auditAction", "beforeStatus", "afterStatus", "auditorId",
        "auditorName", "auditRemark", "auditTime", "createTime", "updateTime", "isDelete",
    ],
    "BasketPlayerCertMaterial": [
        "id", "applicationId", "materialType", "fileId", "materialName", "sortNo", "materialStatus",
        "remark", "createTime", "updateTime", "isDelete",
    ],
    "BasketPlayerProfile": [
        "id", "memberId", "playerNo", "realName", "idType", "idNoCipher", "idNoMasked", "gameTerminal",
        "gameAccount", "gameNickname", "positionCode", "dominantHand", "heightCm", "weightKg",
        "currentTeamId", "certificationStatus", "playerStatus", "certifiedTime", "profileVisible",
        "createTime", "updateTime", "isDelete",
    ],
    "BasketCaptainQualification": [
        "id", "qualificationNo", "memberId", "playerId", "qualificationStatus", "isCurrent",
        "paymentConfirmed", "paymentReference", "grantReason", "grantedBy", "grantedTime", "validFrom",
        "validTo", "revokedBy", "revokedTime", "revokeReason", "createTime", "updateTime", "isDelete",
    ],
    "BasketTeam": [
        "id", "teamNo", "teamName", "shortName", "englishName", "logoFileId", "regionCode", "regionName",
        "gameTerminal", "description", "contactMemberId", "captainMemberId", "captainPlayerId",
        "maxMemberCount", "currentMemberCount", "teamStatus", "auditStatus", "publicStatus",
        "establishedDate", "dissolvedTime", "createTime", "updateTime", "isDelete",
    ],
    "BasketTeamAuditRecord": [
        "id", "teamId", "auditType", "auditAction", "beforeStatus", "afterStatus", "auditorId",
        "auditorName", "auditRemark", "auditTime", "createTime", "updateTime", "isDelete",
    ],
    "BasketTeamMember": [
        "id", "membershipNo", "teamId", "memberId", "playerId", "roleType", "rosterType", "jerseyNo",
        "joinSource", "membershipStatus", "isCurrent", "joinTime", "leaveTime", "leaveReason", "remark",
        "createTime", "updateTime", "isDelete",
    ],
    "BasketTournament": [
        "id", "tournamentNo", "tournamentName", "shortName", "englishName", "logoFileId", "coverFileId",
        "tournamentType", "gameTerminal", "formatCode", "organizerName", "description", "tournamentStatus",
        "publicStatus", "displayOrder", "createTime", "updateTime", "isDelete",
    ],
    "BasketTournamentSeason": [
        "id", "seasonNo", "tournamentId", "seasonName", "seasonYear", "registrationStartTime",
        "registrationEndTime", "seasonStartTime", "seasonEndTime", "venueId", "maxTeamCount",
        "minRosterCount", "maxRosterCount", "seasonStatus", "currentStageId", "championTeamId",
        "runnerUpTeamId", "publicStatus", "description", "createTime", "updateTime", "isDelete",
    ],
    "BasketTournamentRule": [
        "id", "seasonId", "ruleType", "ruleTitle", "ruleContent", "versionNo", "effectiveTime",
        "ruleStatus", "displayOrder", "createTime", "updateTime", "isDelete",
    ],
    "BasketTournamentRegistration": [
        "id", "registrationNo", "seasonId", "teamId", "captainMemberId", "captainPlayerId", "contactName",
        "contactMobile", "contactEmail", "registrationStatus", "lineupCount", "submittedTime", "auditTime",
        "withdrawalTime", "withdrawalReason", "currentAuditorId", "remark", "createTime", "updateTime",
        "isDelete",
    ],
    "BasketTournamentRegistrationPlayer": [
        "id", "registrationId", "seasonId", "teamId", "playerId", "memberId", "teamMemberId", "rosterRole",
        "jerseyNo", "isStarting", "isCaptain", "validationStatus", "rejectReason", "sortNo", "createTime",
        "updateTime", "isDelete",
    ],
    "BasketTournamentRegistrationAudit": [
        "id", "registrationId", "auditAction", "beforeStatus", "afterStatus", "auditorId", "auditorName",
        "auditRemark", "auditTime", "createTime", "updateTime", "isDelete",
    ],
    "BasketFileAsset": [
        "id", "storageProvider", "bucketName", "objectKey", "fileName", "originalName", "fileExt",
        "mimeType", "fileSize", "fileHash", "accessUrl", "usageType", "ownerMemberId", "fileStatus",
        "remark", "createTime", "updateTime", "isDelete",
    ],
    "BasketMessageTask": [
        "id", "taskNo", "messageType", "title", "contentText", "bizType", "bizId", "sendScope",
        "sendStatus", "scheduledTime", "sentTime", "totalCount", "successCount", "failCount", "creatorId",
        "createTime", "updateTime", "isDelete",
    ],
    "BasketMessageReceiver": [
        "id", "taskId", "memberId", "deliveryChannel", "deliveryStatus", "readStatus", "sentTime",
        "readTime", "failureReason", "redirectType", "redirectValue", "createTime", "updateTime", "isDelete",
    ],
    "BasketAccountSecurityLog": [
        "id", "memberId", "eventType", "eventTitle", "oldValueMasked", "newValueMasked", "operatorType",
        "operatorId", "clientIp", "terminalType", "eventTime", "remark", "createTime", "updateTime",
        "isDelete",
    ],
    "BasketMemberAccount": [
        "id", "accountNo", "username", "email", "mobile", "accountStatus", "createTime", "updateTime",
        "isDelete",
    ],
    "BasketDisciplineRecord": [
        "id", "recordNo", "seasonId", "matchId", "teamId", "playerId", "memberId", "disciplineType",
        "title", "disciplineStatus", "effectiveStartTime", "effectiveEndTime", "createTime", "updateTime",
        "isDelete",
    ],
}


def result_map(cls: str) -> str:
    cols = FIELDS[cls]
    lines = [f'        <result property="{c}" column="{camel_to_snake(c)}" />' for c in cols if c != "id"]
    return f"""    <resultMap type="com.zhs.basket.domain.{cls}" id="{cls}Result">
        <id property="id" column="id" />
{chr(10).join(lines)}
    </resultMap>"""


def col_list(cls: str) -> str:
    return ", ".join(camel_to_snake(c) for c in FIELDS[cls])


def insert_block(cls: str, table: str | None = None) -> str:
    table = table or TABLE[cls]
    cols = [c for c in FIELDS[cls] if c != "id"]
    col_sql = ", ".join(camel_to_snake(c) for c in cols)
    val_sql = ", ".join(f"#{{{c}}}" for c in cols)
    return f"""    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        insert into {table} ({col_sql}) values ({val_sql})
    </insert>"""


def xml_header(ns: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{ns}">"""


def write_mapper(java_name: str, java_body: str, xml_body: str) -> None:
    w(f"src/main/java/com/zhs/basket/mapper/{java_name}.java", java_body)
    w(f"src/main/resources/mapper/basket/{java_name}.xml", xml_body)


def gen_mappers() -> None:
    cls = "BasketPlayerCertApplication"
    write_mapper(
        "BasketPlayerCertApplicationMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerCertApplication;
import com.zhs.basket.query.PlayerCertQuery;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketPlayerCertApplicationMapper {
    List<BasketPlayerCertApplication> selectList(PlayerCertQuery query);
    BasketPlayerCertApplication selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("currentAuditorId") Long currentAuditorId,
            @Param("reviewRemark") String reviewRemark, @Param("reviewedTime") Date reviewedTime,
            @Param("auditorId") Long auditorId);
}""",
        xml_header("com.zhs.basket.mapper.BasketPlayerCertApplicationMapper")
        + "\n"
        + result_map(cls)
        + """
    <select id="selectList" resultMap="BasketPlayerCertApplicationResult">
        select * from basket_player_cert_application a
        where a.is_delete = 0
        <if test="applicationNo != null and applicationNo != ''">and a.application_no like concat('%', #{applicationNo}, '%')</if>
        <if test="realName != null and realName != ''">and a.real_name like concat('%', #{realName}, '%')</if>
        <if test="applicationStatus != null">and a.application_status = #{applicationStatus}</if>
        <if test="memberId != null">and a.member_id = #{memberId}</if>
        order by a.submitted_time desc, a.create_time desc
    </select>
    <select id="selectById" resultMap="BasketPlayerCertApplicationResult">
        select * from basket_player_cert_application where id = #{id} and is_delete = 0
    </select>
    <update id="updateStatusIfMatch">
        update basket_player_cert_application
        set application_status = #{toStatus},
            current_auditor_id = #{currentAuditorId},
            review_remark = #{reviewRemark},
            reviewed_time = #{reviewedTime},
            update_time = now()
        where id = #{id} and is_delete = 0 and application_status in
        <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{s}</foreach>
        <if test="auditorId != null">and (current_auditor_id is null or current_auditor_id = #{auditorId})</if>
    </update>
</mapper>""",
    )

    cls = "BasketPlayerProfile"
    write_mapper(
        "BasketPlayerProfileMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerProfile;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketPlayerProfileMapper {
    BasketPlayerProfile selectByMemberId(Long memberId);
    BasketPlayerProfile selectById(Long id);
    int insert(BasketPlayerProfile profile);
    int updateById(BasketPlayerProfile profile);
    int countByPlayerNo(@Param("playerNo") String playerNo);
    List<BasketPlayerProfile> selectEligibleForCaptain();
}""",
        xml_header("com.zhs.basket.mapper.BasketPlayerProfileMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectByMemberId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where member_id = #{{memberId}} and is_delete = 0 limit 1
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
    <select id="countByPlayerNo" resultType="int">
        select count(1) from {TABLE[cls]} where player_no = #{{playerNo}} and is_delete = 0
    </select>
    <select id="selectEligibleForCaptain" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} p
        where p.is_delete = 0 and p.certification_status = 3
          and not exists (
            select 1 from basket_captain_qualification q
            where q.player_id = p.id and q.is_current = 1 and q.qualification_status = 1 and q.is_delete = 0
          )
        order by p.certified_time desc
    </select>
"""
        + insert_block(cls)
        + """
    <update id="updateById">
        update basket_player_profile
        <set>
            <if test="memberId != null">member_id = #{memberId},</if>
            <if test="playerNo != null">player_no = #{playerNo},</if>
            <if test="realName != null">real_name = #{realName},</if>
            <if test="idType != null">id_type = #{idType},</if>
            <if test="idNoCipher != null">id_no_cipher = #{idNoCipher},</if>
            <if test="idNoMasked != null">id_no_masked = #{idNoMasked},</if>
            <if test="gameTerminal != null">game_terminal = #{gameTerminal},</if>
            <if test="gameAccount != null">game_account = #{gameAccount},</if>
            <if test="gameNickname != null">game_nickname = #{gameNickname},</if>
            <if test="positionCode != null">position_code = #{positionCode},</if>
            <if test="dominantHand != null">dominant_hand = #{dominantHand},</if>
            <if test="heightCm != null">height_cm = #{heightCm},</if>
            <if test="weightKg != null">weight_kg = #{weightKg},</if>
            <if test="currentTeamId != null">current_team_id = #{currentTeamId},</if>
            <if test="certificationStatus != null">certification_status = #{certificationStatus},</if>
            <if test="playerStatus != null">player_status = #{playerStatus},</if>
            <if test="certifiedTime != null">certified_time = #{certifiedTime},</if>
            <if test="profileVisible != null">profile_visible = #{profileVisible},</if>
            update_time = now()
        </set>
        where id = #{id} and is_delete = 0
    </update>
</mapper>""",
    )

    cls = "BasketPlayerCertAudit"
    write_mapper(
        "BasketPlayerCertAuditMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerCertAudit;
import java.util.List;

public interface BasketPlayerCertAuditMapper {
    int insert(BasketPlayerCertAudit record);
    List<BasketPlayerCertAudit> selectByApplicationId(Long applicationId);
}""",
        xml_header("com.zhs.basket.mapper.BasketPlayerCertAuditMapper")
        + "\n"
        + result_map(cls)
        + insert_block(cls)
        + f"""
    <select id="selectByApplicationId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where application_id = #{{applicationId}} and is_delete = 0 order by audit_time desc
    </select>
</mapper>""",
    )

    cls = "BasketPlayerCertMaterial"
    write_mapper(
        "BasketPlayerCertMaterialMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPlayerCertMaterial;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketPlayerCertMaterialMapper {
    List<BasketPlayerCertMaterial> selectByApplicationId(Long applicationId);
    int countByFileIdForCert(@Param("fileId") Long fileId);
}""",
        xml_header("com.zhs.basket.mapper.BasketPlayerCertMaterialMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectByApplicationId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where application_id = #{{applicationId}} and is_delete = 0 order by sort_no
    </select>
    <select id="countByFileIdForCert" resultType="int">
        select count(1) from {TABLE[cls]} m
        inner join basket_player_cert_application a on a.id = m.application_id and a.is_delete = 0
        where m.file_id = #{{fileId}} and m.is_delete = 0
    </select>
</mapper>""",
    )

    cls = "BasketCaptainQualification"
    write_mapper(
        "BasketCaptainQualificationMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.query.CaptainQualificationQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketCaptainQualificationMapper {
    List<BasketCaptainQualification> selectList(CaptainQualificationQuery query);
    BasketCaptainQualification selectById(Long id);
    int countCurrentActiveByPlayerId(@Param("playerId") Long playerId);
    int insert(BasketCaptainQualification record);
    int updateById(BasketCaptainQualification record);
    int clearCurrentByPlayerId(@Param("playerId") Long playerId);
}""",
        xml_header("com.zhs.basket.mapper.BasketCaptainQualificationMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectList" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]}
        where is_delete = 0
        <if test="qualificationNo != null and qualificationNo != ''">and qualification_no like concat('%', #{{qualificationNo}}, '%')</if>
        <if test="memberId != null">and member_id = #{{memberId}}</if>
        <if test="playerId != null">and player_id = #{{playerId}}</if>
        <if test="qualificationStatus != null">and qualification_status = #{{qualificationStatus}}</if>
        order by granted_time desc
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
    <select id="countCurrentActiveByPlayerId" resultType="int">
        select count(1) from {TABLE[cls]}
        where player_id = #{{playerId}} and is_current = 1 and qualification_status = 1 and is_delete = 0
    </select>
"""
        + insert_block(cls)
        + """
    <update id="updateById">
        update basket_captain_qualification
        <set>
            <if test="qualificationStatus != null">qualification_status = #{qualificationStatus},</if>
            <if test="isCurrent != null">is_current = #{isCurrent},</if>
            <if test="paymentConfirmed != null">payment_confirmed = #{paymentConfirmed},</if>
            <if test="paymentReference != null">payment_reference = #{paymentReference},</if>
            <if test="grantReason != null">grant_reason = #{grantReason},</if>
            <if test="validFrom != null">valid_from = #{validFrom},</if>
            <if test="validTo != null">valid_to = #{validTo},</if>
            <if test="revokedBy != null">revoked_by = #{revokedBy},</if>
            <if test="revokedTime != null">revoked_time = #{revokedTime},</if>
            <if test="revokeReason != null">revoke_reason = #{revokeReason},</if>
            update_time = now()
        </set>
        where id = #{id} and is_delete = 0
    </update>
    <update id="clearCurrentByPlayerId">
        update basket_captain_qualification set is_current = 0, update_time = now()
        where player_id = #{playerId} and is_current = 1 and is_delete = 0
    </update>
</mapper>""",
    )

    cls = "BasketTeam"
    write_mapper(
        "BasketTeamMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.query.TeamAuditQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTeamMapper {
    List<BasketTeam> selectAuditList(TeamAuditQuery query);
    BasketTeam selectById(Long id);
    int updateAuditStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("auditorId") Long auditorId);
    int updateMemberCount(@Param("id") Long id, @Param("memberCount") Integer memberCount);
}""",
        xml_header("com.zhs.basket.mapper.BasketTeamMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectAuditList" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]}
        where is_delete = 0
        <if test="teamNo != null and teamNo != ''">and team_no like concat('%', #{{teamNo}}, '%')</if>
        <if test="teamName != null and teamName != ''">and team_name like concat('%', #{{teamName}}, '%')</if>
        <if test="auditStatus != null">and audit_status = #{{auditStatus}}</if>
        order by create_time desc
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
    <update id="updateAuditStatusIfMatch">
        update basket_team set audit_status = #{{toStatus}}, update_time = now()
        where id = #{{id}} and is_delete = 0 and audit_status in
        <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{{s}}</foreach>
    </update>
    <update id="updateMemberCount">
        update basket_team set current_member_count = #{{memberCount}}, update_time = now()
        where id = #{{id}} and is_delete = 0
    </update>
</mapper>""",
    )

    cls = "BasketTeamAuditRecord"
    write_mapper(
        "BasketTeamAuditRecordMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamAuditRecord;
import java.util.List;

public interface BasketTeamAuditRecordMapper {
    int insert(BasketTeamAuditRecord record);
    List<BasketTeamAuditRecord> selectByTeamId(Long teamId);
}""",
        xml_header("com.zhs.basket.mapper.BasketTeamAuditRecordMapper")
        + "\n"
        + result_map(cls)
        + insert_block(cls)
        + f"""
    <select id="selectByTeamId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where team_id = #{{teamId}} and is_delete = 0 order by audit_time desc
    </select>
</mapper>""",
    )

    cls = "BasketTeamMember"
    write_mapper(
        "BasketTeamMemberMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTeamMember;
import org.apache.ibatis.annotations.Param;

public interface BasketTeamMemberMapper {
    int countActiveCaptainTeams(@Param("playerId") Long playerId);
    int countCurrentMembers(@Param("teamId") Long teamId);
    BasketTeamMember selectCaptainMember(@Param("teamId") Long teamId);
}""",
        xml_header("com.zhs.basket.mapper.BasketTeamMemberMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="countActiveCaptainTeams" resultType="int">
        select count(1) from basket_team t
        inner join basket_team_member m on m.team_id = t.id and m.is_delete = 0
        where m.player_id = #{{playerId}} and m.role_type = 'CAPTAIN' and m.is_current = 1 and m.membership_status = 1
          and t.team_status = 1 and t.is_delete = 0
    </select>
    <select id="countCurrentMembers" resultType="int">
        select count(1) from {TABLE[cls]}
        where team_id = #{{teamId}} and is_current = 1 and membership_status = 1 and is_delete = 0
    </select>
    <select id="selectCaptainMember" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]}
        where team_id = #{{teamId}} and role_type = 'CAPTAIN' and is_current = 1 and membership_status = 1 and is_delete = 0 limit 1
    </select>
</mapper>""",
    )

    cls = "BasketTournament"
    write_mapper(
        "BasketTournamentMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.query.TournamentQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentMapper {
    List<BasketTournament> selectList(TournamentQuery query);
    BasketTournament selectById(Long id);
    int insert(BasketTournament tournament);
    int updateById(BasketTournament tournament);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
}""",
        xml_header("com.zhs.basket.mapper.BasketTournamentMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectList" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where is_delete = 0
        <if test="tournamentNo != null and tournamentNo != ''">and tournament_no like concat('%', #{{tournamentNo}}, '%')</if>
        <if test="tournamentName != null and tournamentName != ''">and tournament_name like concat('%', #{{tournamentName}}, '%')</if>
        <if test="tournamentStatus != null">and tournament_status = #{{tournamentStatus}}</if>
        order by display_order, create_time desc
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
"""
        + insert_block(cls)
        + """
    <update id="updateById">
        update basket_tournament
        <set>
            <if test="tournamentName != null">tournament_name = #{tournamentName},</if>
            <if test="shortName != null">short_name = #{shortName},</if>
            <if test="englishName != null">english_name = #{englishName},</if>
            <if test="logoFileId != null">logo_file_id = #{logoFileId},</if>
            <if test="coverFileId != null">cover_file_id = #{coverFileId},</if>
            <if test="tournamentType != null">tournament_type = #{tournamentType},</if>
            <if test="gameTerminal != null">game_terminal = #{gameTerminal},</if>
            <if test="formatCode != null">format_code = #{formatCode},</if>
            <if test="organizerName != null">organizer_name = #{organizerName},</if>
            <if test="description != null">description = #{description},</if>
            <if test="tournamentStatus != null">tournament_status = #{tournamentStatus},</if>
            <if test="publicStatus != null">public_status = #{publicStatus},</if>
            <if test="displayOrder != null">display_order = #{displayOrder},</if>
            update_time = now()
        </set>
        where id = #{id} and is_delete = 0
    </update>
    <update id="softDelete">
        update basket_tournament set is_delete = 1, update_time = now() where id = #{id} and is_delete = 0
    </update>
    <update id="updateStatusIfMatch">
        update basket_tournament set tournament_status = #{toStatus}, update_time = now()
        where id = #{id} and is_delete = 0 and tournament_status in
        <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{s}</foreach>
    </update>
</mapper>""",
    )

    cls = "BasketTournamentSeason"
    write_mapper(
        "BasketTournamentSeasonMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentSeason;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentSeasonMapper {
    List<BasketTournamentSeason> selectByTournamentId(Long tournamentId);
    BasketTournamentSeason selectById(Long id);
    BasketTournamentSeason selectByIdForUpdate(Long id);
    int insert(BasketTournamentSeason season);
    int updateById(BasketTournamentSeason season);
    int softDelete(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus);
}""",
        xml_header("com.zhs.basket.mapper.BasketTournamentSeasonMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectByTournamentId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where tournament_id = #{{tournamentId}} and is_delete = 0 order by season_year desc, create_time desc
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
    <select id="selectByIdForUpdate" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0 for update
    </select>
"""
        + insert_block(cls)
        + """
    <update id="updateById">
        update basket_tournament_season
        <set>
            <if test="seasonName != null">season_name = #{seasonName},</if>
            <if test="seasonYear != null">season_year = #{seasonYear},</if>
            <if test="registrationStartTime != null">registration_start_time = #{registrationStartTime},</if>
            <if test="registrationEndTime != null">registration_end_time = #{registrationEndTime},</if>
            <if test="seasonStartTime != null">season_start_time = #{seasonStartTime},</if>
            <if test="seasonEndTime != null">season_end_time = #{seasonEndTime},</if>
            <if test="venueId != null">venue_id = #{venueId},</if>
            <if test="maxTeamCount != null">max_team_count = #{maxTeamCount},</if>
            <if test="minRosterCount != null">min_roster_count = #{minRosterCount},</if>
            <if test="maxRosterCount != null">max_roster_count = #{maxRosterCount},</if>
            <if test="seasonStatus != null">season_status = #{seasonStatus},</if>
            <if test="publicStatus != null">public_status = #{publicStatus},</if>
            <if test="description != null">description = #{description},</if>
            update_time = now()
        </set>
        where id = #{id} and is_delete = 0
    </update>
    <update id="softDelete">
        update basket_tournament_season set is_delete = 1, update_time = now() where id = #{id} and is_delete = 0
    </update>
    <update id="updateStatusIfMatch">
        update basket_tournament_season set season_status = #{toStatus}, update_time = now()
        where id = #{id} and is_delete = 0 and season_status in
        <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{s}</foreach>
    </update>
</mapper>""",
    )

    cls = "BasketTournamentRule"
    write_mapper(
        "BasketTournamentRuleMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRule;
import java.util.List;

public interface BasketTournamentRuleMapper {
    List<BasketTournamentRule> selectBySeasonId(Long seasonId);
    BasketTournamentRule selectById(Long id);
    int insert(BasketTournamentRule rule);
    int updateById(BasketTournamentRule rule);
    int softDelete(Long id);
}""",
        xml_header("com.zhs.basket.mapper.BasketTournamentRuleMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectBySeasonId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where season_id = #{{seasonId}} and is_delete = 0 order by display_order, id
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
"""
        + insert_block(cls)
        + """
    <update id="updateById">
        update basket_tournament_rule
        <set>
            <if test="ruleType != null">rule_type = #{ruleType},</if>
            <if test="ruleTitle != null">rule_title = #{ruleTitle},</if>
            <if test="ruleContent != null">rule_content = #{ruleContent},</if>
            <if test="versionNo != null">version_no = #{versionNo},</if>
            <if test="effectiveTime != null">effective_time = #{effectiveTime},</if>
            <if test="ruleStatus != null">rule_status = #{ruleStatus},</if>
            <if test="displayOrder != null">display_order = #{displayOrder},</if>
            update_time = now()
        </set>
        where id = #{id} and is_delete = 0
    </update>
    <update id="softDelete">
        update basket_tournament_rule set is_delete = 1, update_time = now() where id = #{id} and is_delete = 0
    </update>
</mapper>""",
    )

    cls = "BasketTournamentRegistration"
    write_mapper(
        "BasketTournamentRegistrationMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRegistration;
import com.zhs.basket.query.RegistrationAuditQuery;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketTournamentRegistrationMapper {
    List<BasketTournamentRegistration> selectAuditList(RegistrationAuditQuery query);
    BasketTournamentRegistration selectById(Long id);
    int updateStatusIfMatch(@Param("id") Long id, @Param("fromStatuses") List<Integer> fromStatuses,
            @Param("toStatus") Integer toStatus, @Param("currentAuditorId") Long currentAuditorId,
            @Param("remark") String remark, @Param("auditTime") Date auditTime, @Param("auditorId") Long auditorId);
    int countApprovedBySeasonId(@Param("seasonId") Long seasonId);
}""",
        xml_header("com.zhs.basket.mapper.BasketTournamentRegistrationMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectAuditList" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where is_delete = 0
        <if test="registrationNo != null and registrationNo != ''">and registration_no like concat('%', #{{registrationNo}}, '%')</if>
        <if test="seasonId != null">and season_id = #{{seasonId}}</if>
        <if test="teamId != null">and team_id = #{{teamId}}</if>
        <if test="registrationStatus != null">and registration_status = #{{registrationStatus}}</if>
        order by submitted_time desc, create_time desc
    </select>
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
    <select id="countApprovedBySeasonId" resultType="int">
        select count(1) from {TABLE[cls]}
        where season_id = #{{seasonId}} and registration_status = 3 and is_delete = 0
    </select>
    <update id="updateStatusIfMatch">
        update basket_tournament_registration
        set registration_status = #{{toStatus}},
            current_auditor_id = #{{currentAuditorId}},
            remark = #{{remark}},
            audit_time = #{{auditTime}},
            update_time = now()
        where id = #{{id}} and is_delete = 0 and registration_status in
        <foreach collection="fromStatuses" item="s" open="(" separator="," close=")">#{{s}}</foreach>
        <if test="auditorId != null">and (current_auditor_id is null or current_auditor_id = #{{auditorId}})</if>
    </update>
</mapper>""",
    )

    cls = "BasketTournamentRegistrationPlayer"
    write_mapper(
        "BasketTournamentRegistrationPlayerMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRegistrationPlayer;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BasketTournamentRegistrationPlayerMapper {
    List<BasketTournamentRegistrationPlayer> selectByRegistrationId(Long registrationId);
    int updateValidationStatus(@Param("id") Long id, @Param("validationStatus") Integer validationStatus,
            @Param("rejectReason") String rejectReason);
}""",
        xml_header("com.zhs.basket.mapper.BasketTournamentRegistrationPlayerMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectByRegistrationId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where registration_id = #{{registrationId}} and is_delete = 0 order by sort_no
    </select>
    <update id="updateValidationStatus">
        update basket_tournament_registration_player
        set validation_status = #{{validationStatus}}, reject_reason = #{{rejectReason}}, update_time = now()
        where id = #{{id}} and is_delete = 0
    </update>
</mapper>""",
    )

    cls = "BasketTournamentRegistrationAudit"
    write_mapper(
        "BasketTournamentRegistrationAuditMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketTournamentRegistrationAudit;
import java.util.List;

public interface BasketTournamentRegistrationAuditMapper {
    int insert(BasketTournamentRegistrationAudit record);
    List<BasketTournamentRegistrationAudit> selectByRegistrationId(Long registrationId);
}""",
        xml_header("com.zhs.basket.mapper.BasketTournamentRegistrationAuditMapper")
        + "\n"
        + result_map(cls)
        + insert_block(cls)
        + f"""
    <select id="selectByRegistrationId" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where registration_id = #{{registrationId}} and is_delete = 0 order by audit_time desc
    </select>
</mapper>""",
    )

    for simple in [
        ("BasketFileAssetMapper", "BasketFileAsset", "selectById"),
        ("BasketMessageTaskMapper", "BasketMessageTask", "insert"),
        ("BasketMessageReceiverMapper", "BasketMessageReceiver", "insert"),
        ("BasketAccountSecurityLogMapper", "BasketAccountSecurityLog", "insert"),
        ("BasketMemberAccountMapper", "BasketMemberAccount", "selectById"),
    ]:
        name, cls, method = simple
        if method == "selectById":
            java = f"""package com.zhs.basket.mapper;

import com.zhs.basket.domain.{cls};

public interface {name} {{
    {cls} selectById(Long id);
}}"""
            xml = (
                xml_header(f"com.zhs.basket.mapper.{name}")
                + "\n"
                + result_map(cls)
                + f"""
    <select id="selectById" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]} where id = #{{id}} and is_delete = 0
    </select>
</mapper>"""
            )
        else:
            java = f"""package com.zhs.basket.mapper;

import com.zhs.basket.domain.{cls};

public interface {name} {{
    int insert({cls} record);
}}"""
            xml = xml_header(f"com.zhs.basket.mapper.{name}") + "\n" + result_map(cls) + insert_block(cls) + "\n</mapper>"
        write_mapper(name, java, xml)

    cls = "BasketDisciplineRecord"
    write_mapper(
        "BasketDisciplineRecordMapper",
        """package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineRecord;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

public interface BasketDisciplineRecordMapper {
    List<BasketDisciplineRecord> selectActiveBySeasonAndMembers(@Param("seasonId") Long seasonId,
            @Param("memberIds") List<Long> memberIds, @Param("now") Date now);
}""",
        xml_header("com.zhs.basket.mapper.BasketDisciplineRecordMapper")
        + "\n"
        + result_map(cls)
        + f"""
    <select id="selectActiveBySeasonAndMembers" resultMap="{cls}Result">
        select {col_list(cls)} from {TABLE[cls]}
        where season_id = #{{seasonId}} and discipline_status = 1 and is_delete = 0
          and effective_start_time &lt;= #{{now}}
          and (effective_end_time is null or effective_end_time &gt;= #{{now}})
          and member_id in
        <foreach collection="memberIds" item="mid" open="(" separator="," close=")">#{{mid}}</foreach>
    </select>
</mapper>""",
    )


def gen_query_req_vo() -> None:
    queries = {
        "PlayerCertQuery": [
            ("String", "applicationNo"),
            ("String", "realName"),
            ("Integer", "applicationStatus"),
            ("Long", "memberId"),
        ],
        "CaptainQualificationQuery": [
            ("String", "qualificationNo"),
            ("Long", "memberId"),
            ("Long", "playerId"),
            ("Integer", "qualificationStatus"),
        ],
        "TeamAuditQuery": [
            ("String", "teamNo"),
            ("String", "teamName"),
            ("Integer", "auditStatus"),
        ],
        "TournamentQuery": [
            ("String", "tournamentNo"),
            ("String", "tournamentName"),
            ("Integer", "tournamentStatus"),
        ],
        "RegistrationAuditQuery": [
            ("String", "registrationNo"),
            ("Long", "seasonId"),
            ("Long", "teamId"),
            ("Integer", "registrationStatus"),
        ],
    }
    for name, fields in queries.items():
        body = "\n".join(f"    private {t} {f};" for t, f in fields)
        w(
            f"src/main/java/com/zhs/basket/query/{name}.java",
            f"""package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class {name} extends BaseEntity {{
{body}
}}""",
        )

    reqs = {
        "PlayerCertApproveReq": [("String", "auditRemark"), ("String", "positionCode")],
        "PlayerCertRejectReq": [("String", "rejectReason"), ("String", "auditRemark")],
        "CaptainQualificationGrantReq": [
            ("Long", "playerId"),
            ("Integer", "paymentConfirmed"),
            ("String", "paymentReference"),
            ("java.util.Date", "validFrom"),
            ("java.util.Date", "validTo"),
            ("String", "grantRemark"),
        ],
        "CaptainQualificationUpdateReq": [
            ("Integer", "paymentConfirmed"),
            ("String", "paymentReference"),
            ("java.util.Date", "validFrom"),
            ("java.util.Date", "validTo"),
            ("String", "grantRemark"),
        ],
        "CaptainQualificationRevokeReq": [("String", "revokeReason",)],
        "TeamAuditApproveReq": [("String", "auditRemark",)],
        "TeamAuditRejectReq": [("String", "rejectReason"), ("String", "auditRemark")],
        "TournamentCreateReq": [
            ("String", "tournamentName"), ("String", "shortName"), ("String", "englishName"),
            ("Long", "logoFileId"), ("Long", "coverFileId"), ("String", "tournamentType"),
            ("String", "gameTerminal"), ("String", "formatCode"), ("String", "organizerName"),
            ("String", "description"), ("Integer", "publicStatus"), ("Integer", "displayOrder"),
        ],
        "TournamentUpdateReq": [
            ("String", "tournamentName"), ("String", "shortName"), ("String", "englishName"),
            ("Long", "logoFileId"), ("Long", "coverFileId"), ("String", "tournamentType"),
            ("String", "gameTerminal"), ("String", "formatCode"), ("String", "organizerName"),
            ("String", "description"), ("Integer", "publicStatus"), ("Integer", "displayOrder"),
        ],
        "SeasonCreateReq": [
            ("String", "seasonName"), ("Integer", "seasonYear"),
            ("java.util.Date", "registrationStartTime"), ("java.util.Date", "registrationEndTime"),
            ("java.util.Date", "seasonStartTime"), ("java.util.Date", "seasonEndTime"),
            ("Long", "venueId"), ("Integer", "maxTeamCount"), ("Integer", "minRosterCount"),
            ("Integer", "maxRosterCount"), ("Integer", "publicStatus"), ("String", "description"),
        ],
        "SeasonUpdateReq": [
            ("String", "seasonName"), ("Integer", "seasonYear"),
            ("java.util.Date", "registrationStartTime"), ("java.util.Date", "registrationEndTime"),
            ("java.util.Date", "seasonStartTime"), ("java.util.Date", "seasonEndTime"),
            ("Long", "venueId"), ("Integer", "maxTeamCount"), ("Integer", "minRosterCount"),
            ("Integer", "maxRosterCount"), ("Integer", "publicStatus"), ("String", "description"),
        ],
        "TournamentRuleCreateReq": [
            ("String", "ruleType"), ("String", "ruleTitle"), ("String", "ruleContent"),
            ("String", "versionNo"), ("java.util.Date", "effectiveTime"), ("Integer", "ruleStatus"),
            ("Integer", "displayOrder"),
        ],
        "TournamentRuleUpdateReq": [
            ("String", "ruleType"), ("String", "ruleTitle"), ("String", "ruleContent"),
            ("String", "versionNo"), ("java.util.Date", "effectiveTime"), ("Integer", "ruleStatus"),
            ("Integer", "displayOrder"),
        ],
        "RegistrationAuditApproveReq": [("String", "auditRemark",)],
        "RegistrationAuditRejectReq": [("String", "rejectReason"), ("String", "auditRemark")],
    }
    for name, fields in reqs.items():
        imports = sorted({f"import {t};" for t, *_ in fields if t.startswith("java.")})
        body = "\n".join(f"    private {t.split('.')[-1]} {f};" for t, f, *_ in [(x[0], x[1], *x[2:]) for x in fields])
        imp = ("\n" + "\n".join(imports)) if imports else ""
        w(
            f"src/main/java/com/zhs/basket/req/{name}.java",
            f"""package com.zhs.basket.req;
{imp}
import lombok.Data;

@Data
public class {name} {{
{body}
}}""",
        )

    vos = {
        "PlayerCertListVo": [
            ("Long", "id"), ("String", "applicationNo"), ("Long", "memberId"), ("String", "realName"),
            ("Integer", "applicationStatus"), ("String", "applicationStatusName"), ("java.util.Date", "submittedTime"),
            ("java.util.Date", "reviewedTime"),
        ],
        "PlayerCertDetailVo": [
            ("Long", "id"), ("String", "applicationNo"), ("Long", "memberId"), ("String", "realName"),
            ("String", "idType"), ("String", "idNoMasked"), ("String", "gameTerminal"), ("String", "gameAccount"),
            ("String", "gameNickname"), ("String", "positionCode"), ("String", "contactMobile"),
            ("Integer", "applicationStatus"), ("String", "applicationStatusName"), ("String", "reviewRemark"),
            ("java.util.Date", "submittedTime"), ("java.util.Date", "reviewedTime"), ("Long", "currentAuditorId"),
        ],
        "PlayerCertAuditRecordVo": [
            ("Long", "id"), ("String", "auditAction"), ("Integer", "beforeStatus"), ("Integer", "afterStatus"),
            ("String", "auditorName"), ("String", "auditRemark"), ("java.util.Date", "auditTime"),
        ],
        "PlayerCertMaterialVo": [
            ("Long", "id"), ("String", "materialType"), ("Long", "fileId"), ("String", "materialName"), ("Integer", "sortNo"),
        ],
        "EligibleCaptainVo": [
            ("Long", "playerId"), ("Long", "memberId"), ("String", "playerNo"), ("String", "realName"),
            ("Integer", "certificationStatus"),
        ],
        "CaptainQualificationListVo": [
            ("Long", "id"), ("String", "qualificationNo"), ("Long", "memberId"), ("Long", "playerId"),
            ("Integer", "qualificationStatus"), ("Integer", "isCurrent"), ("Integer", "paymentConfirmed"),
            ("java.util.Date", "grantedTime"), ("java.util.Date", "validTo"),
        ],
        "CaptainQualificationDetailVo": [
            ("Long", "id"), ("String", "qualificationNo"), ("Long", "memberId"), ("Long", "playerId"),
            ("Integer", "qualificationStatus"), ("Integer", "isCurrent"), ("Integer", "paymentConfirmed"),
            ("String", "paymentReference"), ("String", "grantReason"), ("java.util.Date", "grantedTime"),
            ("java.util.Date", "validFrom"), ("java.util.Date", "validTo"), ("String", "revokeReason"),
        ],
        "TeamAuditListVo": [
            ("Long", "id"), ("String", "teamNo"), ("String", "teamName"), ("Integer", "auditStatus"),
            ("String", "auditStatusName"), ("Integer", "currentMemberCount"), ("java.util.Date", "createTime"),
        ],
        "TeamAuditDetailVo": [
            ("Long", "id"), ("String", "teamNo"), ("String", "teamName"), ("String", "shortName"),
            ("String", "regionName"), ("String", "gameTerminal"), ("String", "description"),
            ("Long", "captainMemberId"), ("Long", "captainPlayerId"), ("Integer", "auditStatus"),
            ("String", "auditStatusName"), ("Integer", "currentMemberCount"),
        ],
        "TeamAuditRecordVo": [
            ("Long", "id"), ("String", "auditAction"), ("Integer", "beforeStatus"), ("Integer", "afterStatus"),
            ("String", "auditorName"), ("String", "auditRemark"), ("java.util.Date", "auditTime"),
        ],
        "TournamentListVo": [
            ("Long", "id"), ("String", "tournamentNo"), ("String", "tournamentName"), ("Integer", "tournamentStatus"),
            ("String", "tournamentStatusName"), ("Integer", "displayOrder"), ("java.util.Date", "createTime"),
        ],
        "TournamentDetailVo": [
            ("Long", "id"), ("String", "tournamentNo"), ("String", "tournamentName"), ("String", "shortName"),
            ("String", "englishName"), ("Long", "logoFileId"), ("Long", "coverFileId"), ("String", "tournamentType"),
            ("String", "gameTerminal"), ("String", "formatCode"), ("String", "organizerName"),
            ("String", "description"), ("Integer", "tournamentStatus"), ("String", "tournamentStatusName"),
            ("Integer", "publicStatus"), ("Integer", "displayOrder"),
        ],
        "SeasonListVo": [
            ("Long", "id"), ("String", "seasonNo"), ("String", "seasonName"), ("Integer", "seasonYear"),
            ("Integer", "seasonStatus"), ("String", "seasonStatusName"), ("java.util.Date", "registrationStartTime"),
            ("java.util.Date", "registrationEndTime"),
        ],
        "SeasonDetailVo": [
            ("Long", "id"), ("String", "seasonNo"), ("Long", "tournamentId"), ("String", "seasonName"),
            ("Integer", "seasonYear"), ("java.util.Date", "registrationStartTime"), ("java.util.Date", "registrationEndTime"),
            ("java.util.Date", "seasonStartTime"), ("java.util.Date", "seasonEndTime"), ("Long", "venueId"),
            ("Integer", "maxTeamCount"), ("Integer", "minRosterCount"), ("Integer", "maxRosterCount"),
            ("Integer", "seasonStatus"), ("String", "seasonStatusName"), ("String", "description"),
        ],
        "TournamentRuleVo": [
            ("Long", "id"), ("Long", "seasonId"), ("String", "ruleType"), ("String", "ruleTitle"),
            ("String", "ruleContent"), ("String", "versionNo"), ("java.util.Date", "effectiveTime"),
            ("Integer", "ruleStatus"), ("Integer", "displayOrder"),
        ],
        "RegistrationAuditListVo": [
            ("Long", "id"), ("String", "registrationNo"), ("Long", "seasonId"), ("Long", "teamId"),
            ("Integer", "registrationStatus"), ("String", "registrationStatusName"), ("Integer", "lineupCount"),
            ("java.util.Date", "submittedTime"),
        ],
        "RegistrationAuditDetailVo": [
            ("Long", "id"), ("String", "registrationNo"), ("Long", "seasonId"), ("Long", "teamId"),
            ("Long", "captainMemberId"), ("String", "contactName"), ("String", "contactMobile"),
            ("Integer", "registrationStatus"), ("String", "registrationStatusName"), ("Integer", "lineupCount"),
            ("String", "remark"), ("java.util.Date", "submittedTime"), ("java.util.Date", "auditTime"),
        ],
        "RegistrationPlayerVo": [
            ("Long", "id"), ("Long", "playerId"), ("Long", "memberId"), ("String", "rosterRole"), ("String", "jerseyNo"),
            ("Integer", "isCaptain"), ("Integer", "validationStatus"), ("String", "rejectReason"),
        ],
        "RegistrationAuditRecordVo": [
            ("Long", "id"), ("String", "auditAction"), ("Integer", "beforeStatus"), ("Integer", "afterStatus"),
            ("String", "auditorName"), ("String", "auditRemark"), ("java.util.Date", "auditTime"),
        ],
    }
    for name, fields in vos.items():
        imports = sorted({f"import {t};" for t, _ in fields if t.startswith("java.")})
        body = "\n".join(f"    private {t.split('.')[-1]} {f};" for t, f in fields)
        imp = ("\n" + "\n".join(imports)) if imports else ""
        w(
            f"src/main/java/com/zhs/basket/vo/{name}.java",
            f"""package com.zhs.basket.vo;
{imp}
import lombok.Data;

@Data
public class {name} {{
{body}
}}""",
        )


def update_application_yml() -> None:
    with open(APP_YML, encoding="utf-8") as f:
        content = f.read()
    old = "typeAliasesPackage: com.zhs.**.model,com.zhs.common.core.domain"
    new = "typeAliasesPackage: com.zhs.**.model,com.zhs.common.core.domain,com.zhs.basket.domain"
    if old in content:
        content = content.replace(old, new)
    elif "com.zhs.basket.domain" not in content:
        content = content.replace(
            "typeAliasesPackage: com.zhs.**.model,com.zhs.common.core.domain",
            new,
        )
    w("src/main/resources/application.yml", content)


def gen_menu_sql() -> None:
    w(
        "../../sql/cnpa-admin-menu.sql",
        """-- CNPA Admin menu tree and admin role assignment
SET @cnpaParentId = NULL;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
VALUES ('CNPA赛事管理', 0, 5, 'basket', NULL, '', '', 1, 0, 'M', '0', '0', '', 'international', 'admin', sysdate(), 'CNPA赛事管理目录');
SET @cnpaParentId = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark) VALUES
('球员认证审核', @cnpaParentId, 1, 'playerCert', 'basket/playerCert/index', '', '', 1, 0, 'C', '0', '0', 'basket:playerCert:list', 'user', 'admin', sysdate(), ''),
('队长资格管理', @cnpaParentId, 2, 'captainQualification', 'basket/captainQualification/index', '', '', 1, 0, 'C', '0', '0', 'basket:captainQualification:list', 'peoples', 'admin', sysdate(), ''),
('球队审核', @cnpaParentId, 3, 'teamAudit', 'basket/teamAudit/index', '', '', 1, 0, 'C', '0', '0', 'basket:teamAudit:list', 'tree', 'admin', sysdate(), ''),
('赛事管理', @cnpaParentId, 4, 'tournament', 'basket/tournament/index', '', '', 1, 0, 'C', '0', '0', 'basket:tournament:list', 'star', 'admin', sysdate(), ''),
('赛季管理', @cnpaParentId, 5, 'season', 'basket/season/index', '', '', 1, 0, 'C', '0', '0', 'basket:season:list', 'date', 'admin', sysdate(), ''),
('赛事规则', @cnpaParentId, 6, 'tournamentRule', 'basket/tournamentRule/index', '', '', 1, 0, 'C', '0', '0', 'basket:tournamentRule:list', 'documentation', 'admin', sysdate(), ''),
('报名审核', @cnpaParentId, 7, 'registrationAudit', 'basket/registrationAudit/index', '', '', 1, 0, 'C', '0', '0', 'basket:registrationAudit:list', 'form', 'admin', sysdate(), '');

-- player cert buttons
SET @playerCertMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:playerCert:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('球员认证查询', @playerCertMenuId, 1, '', '', 'F', '0', '0', 'basket:playerCert:query', '#', 'admin', sysdate()),
('球员认证审核', @playerCertMenuId, 2, '', '', 'F', '0', '0', 'basket:playerCert:audit', '#', 'admin', sysdate()),
('球员认证导出', @playerCertMenuId, 3, '', '', 'F', '0', '0', 'basket:playerCert:export', '#', 'admin', sysdate());

-- captain qualification buttons
SET @captainMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:captainQualification:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('队长资格查询', @captainMenuId, 1, '', '', 'F', '0', '0', 'basket:captainQualification:query', '#', 'admin', sysdate()),
('队长资格授予', @captainMenuId, 2, '', '', 'F', '0', '0', 'basket:captainQualification:grant', '#', 'admin', sysdate()),
('队长资格编辑', @captainMenuId, 3, '', '', 'F', '0', '0', 'basket:captainQualification:edit', '#', 'admin', sysdate()),
('队长资格撤销', @captainMenuId, 4, '', '', 'F', '0', '0', 'basket:captainQualification:revoke', '#', 'admin', sysdate());

-- team audit buttons
SET @teamAuditMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:teamAudit:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('球队审核查询', @teamAuditMenuId, 1, '', '', 'F', '0', '0', 'basket:teamAudit:query', '#', 'admin', sysdate()),
('球队审核操作', @teamAuditMenuId, 2, '', '', 'F', '0', '0', 'basket:teamAudit:audit', '#', 'admin', sysdate());

-- tournament buttons
SET @tournamentMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:tournament:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('赛事查询', @tournamentMenuId, 1, '', '', 'F', '0', '0', 'basket:tournament:query', '#', 'admin', sysdate()),
('赛事新增', @tournamentMenuId, 2, '', '', 'F', '0', '0', 'basket:tournament:add', '#', 'admin', sysdate()),
('赛事编辑', @tournamentMenuId, 3, '', '', 'F', '0', '0', 'basket:tournament:edit', '#', 'admin', sysdate()),
('赛事发布', @tournamentMenuId, 4, '', '', 'F', '0', '0', 'basket:tournament:publish', '#', 'admin', sysdate()),
('赛事关闭', @tournamentMenuId, 5, '', '', 'F', '0', '0', 'basket:tournament:close', '#', 'admin', sysdate()),
('赛事删除', @tournamentMenuId, 6, '', '', 'F', '0', '0', 'basket:tournament:remove', '#', 'admin', sysdate());

-- season buttons
SET @seasonMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:season:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('赛季查询', @seasonMenuId, 1, '', '', 'F', '0', '0', 'basket:season:query', '#', 'admin', sysdate()),
('赛季新增', @seasonMenuId, 2, '', '', 'F', '0', '0', 'basket:season:add', '#', 'admin', sysdate()),
('赛季编辑', @seasonMenuId, 3, '', '', 'F', '0', '0', 'basket:season:edit', '#', 'admin', sysdate()),
('赛季发布', @seasonMenuId, 4, '', '', 'F', '0', '0', 'basket:season:publish', '#', 'admin', sysdate()),
('开启报名', @seasonMenuId, 5, '', '', 'F', '0', '0', 'basket:season:openRegistration', '#', 'admin', sysdate()),
('截止报名', @seasonMenuId, 6, '', '', 'F', '0', '0', 'basket:season:closeRegistration', '#', 'admin', sysdate()),
('赛季开始', @seasonMenuId, 7, '', '', 'F', '0', '0', 'basket:season:start', '#', 'admin', sysdate()),
('赛季结束', @seasonMenuId, 8, '', '', 'F', '0', '0', 'basket:season:finish', '#', 'admin', sysdate()),
('赛季删除', @seasonMenuId, 9, '', '', 'F', '0', '0', 'basket:season:remove', '#', 'admin', sysdate());

-- rule buttons
SET @ruleMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:tournamentRule:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('规则查询', @ruleMenuId, 1, '', '', 'F', '0', '0', 'basket:tournamentRule:list', '#', 'admin', sysdate()),
('规则新增', @ruleMenuId, 2, '', '', 'F', '0', '0', 'basket:tournamentRule:add', '#', 'admin', sysdate()),
('规则编辑', @ruleMenuId, 3, '', '', 'F', '0', '0', 'basket:tournamentRule:edit', '#', 'admin', sysdate()),
('规则删除', @ruleMenuId, 4, '', '', 'F', '0', '0', 'basket:tournamentRule:remove', '#', 'admin', sysdate());

-- registration audit buttons
SET @regMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'basket:registrationAudit:list' ORDER BY menu_id DESC LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('报名审核查询', @regMenuId, 1, '', '', 'F', '0', '0', 'basket:registrationAudit:query', '#', 'admin', sysdate()),
('报名审核操作', @regMenuId, 2, '', '', 'F', '0', '0', 'basket:registrationAudit:audit', '#', 'admin', sysdate()),
('报名审核导出', @regMenuId, 3, '', '', 'F', '0', '0', 'basket:registrationAudit:export', '#', 'admin', sysdate());

-- assign all CNPA menus to admin role
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu
WHERE menu_id = @cnpaParentId
   OR parent_id = @cnpaParentId
   OR parent_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = @cnpaParentId);
""",
        base=ROOT,
    )


def update_web_enums() -> None:
    team_path = os.path.join(WEB_ENUM, "TeamAuditStatusEnum.java")
    with open(team_path, encoding="utf-8") as f:
        team = f.read()
    if "REVIEWING" not in team:
        team = team.replace(
            'REJECTED(2, "已驳回");',
            'REJECTED(2, "已驳回"),\n    REVIEWING(3, "审核中");',
        )
        w("../../basketball-web/src/main/java/com/zhs/enums/TeamAuditStatusEnum.java", team, base=ROOT)

    season_path = os.path.join(WEB_ENUM, "TournamentSeasonStatusEnum.java")
    with open(season_path, encoding="utf-8") as f:
        season = f.read()
    if "REGISTRATION_CLOSED" not in season:
        season = season.replace(
            'FINISHED(3, "已结束");',
            'FINISHED(3, "已结束"),\n    REGISTRATION_CLOSED(4, "报名截止"),\n    CLOSED(5, "已关闭"),\n    CANCELLED(6, "已取消");',
        )
        w("../../basketball-web/src/main/java/com/zhs/enums/TournamentSeasonStatusEnum.java", season, base=ROOT)

    tour_path = os.path.join(WEB_ENUM, "TournamentStatusEnum.java")
    with open(tour_path, encoding="utf-8") as f:
        tour = f.read()
    if "DRAFT" not in tour:
        tour = tour.replace(
            'ENDED(2, "已结束");',
            'ENDED(2, "已结束"),\n    DRAFT(3, "草稿");',
        )
        w("../../basketball-web/src/main/java/com/zhs/enums/TournamentStatusEnum.java", tour, base=ROOT)


def main() -> int:
    print("=== Round 4 complete generator ===")
    script_dir = os.path.dirname(os.path.abspath(__file__))
    if script_dir not in sys.path:
        sys.path.insert(0, script_dir)
    gen_mappers()
    gen_query_req_vo()
    from _gen_round4_services import generate as gen_svc
    from _gen_round4_controllers import generate as gen_ctrl

    gen_svc(w)
    gen_ctrl(w)
    update_application_yml()
    gen_menu_sql()
    update_web_enums()
    print(f"\nDone. Created/updated {len(CREATED)} files.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
