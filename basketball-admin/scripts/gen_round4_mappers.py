# -*- coding: utf-8 -*-
"""Generate MyBatis mappers and XML for basket module."""
import os, re

ROOT = os.path.join(os.path.dirname(__file__), "..")
JAVA_M = os.path.join(ROOT, "src", "main", "java", "com", "zhs", "basket", "mapper")
XML_M = os.path.join(ROOT, "src", "main", "resources", "mapper", "basket")

def camel_to_snake(name):
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

def w(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content.strip() + "\n")

# table name map
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
    "BasketPlayerCertApplication": ["id","applicationNo","memberId","playerId","applicationType","realName","idType","idNoCipher","idNoMasked","gameTerminal","gameAccount","gameNickname","positionCode","contactMobile","applicationStatus","submittedTime","reviewedTime","currentAuditorId","reviewRemark","createTime","updateTime","isDelete"],
    "BasketPlayerCertAudit": ["id","applicationId","auditAction","beforeStatus","afterStatus","auditorId","auditorName","auditRemark","auditTime","createTime","updateTime","isDelete"],
    "BasketPlayerCertMaterial": ["id","applicationId","materialType","fileId","materialName","sortNo","materialStatus","remark","createTime","updateTime","isDelete"],
    "BasketPlayerProfile": ["id","memberId","playerNo","realName","idType","idNoCipher","idNoMasked","gameTerminal","gameAccount","gameNickname","positionCode","dominantHand","heightCm","weightKg","currentTeamId","certificationStatus","playerStatus","certifiedTime","profileVisible","createTime","updateTime","isDelete"],
    "BasketCaptainQualification": ["id","qualificationNo","memberId","playerId","qualificationStatus","isCurrent","paymentConfirmed","paymentReference","grantReason","grantedBy","grantedTime","validFrom","validTo","revokedBy","revokedTime","revokeReason","createTime","updateTime","isDelete"],
    "BasketTeam": ["id","teamNo","teamName","shortName","englishName","logoFileId","regionCode","regionName","gameTerminal","description","contactMemberId","captainMemberId","captainPlayerId","maxMemberCount","currentMemberCount","teamStatus","auditStatus","publicStatus","establishedDate","dissolvedTime","createTime","updateTime","isDelete"],
    "BasketTeamAuditRecord": ["id","teamId","auditType","auditAction","beforeStatus","afterStatus","auditorId","auditorName","auditRemark","auditTime","createTime","updateTime","isDelete"],
    "BasketTeamMember": ["id","membershipNo","teamId","memberId","playerId","roleType","rosterType","jerseyNo","joinSource","membershipStatus","isCurrent","joinTime","leaveTime","leaveReason","remark","createTime","updateTime","isDelete"],
    "BasketTournament": ["id","tournamentNo","tournamentName","shortName","englishName","logoFileId","coverFileId","tournamentType","gameTerminal","formatCode","organizerName","description","tournamentStatus","publicStatus","displayOrder","createTime","updateTime","isDelete"],
    "BasketTournamentSeason": ["id","seasonNo","tournamentId","seasonName","seasonYear","registrationStartTime","registrationEndTime","seasonStartTime","seasonEndTime","venueId","maxTeamCount","minRosterCount","maxRosterCount","seasonStatus","currentStageId","championTeamId","runnerUpTeamId","publicStatus","description","createTime","updateTime","isDelete"],
    "BasketTournamentRule": ["id","seasonId","ruleType","ruleTitle","ruleContent","versionNo","effectiveTime","ruleStatus","displayOrder","createTime","updateTime","isDelete"],
    "BasketTournamentRegistration": ["id","registrationNo","seasonId","teamId","captainMemberId","captainPlayerId","contactName","contactMobile","contactEmail","registrationStatus","lineupCount","submittedTime","auditTime","withdrawalTime","withdrawalReason","currentAuditorId","remark","createTime","updateTime","isDelete"],
    "BasketTournamentRegistrationPlayer": ["id","registrationId","seasonId","teamId","playerId","memberId","teamMemberId","rosterRole","jerseyNo","isStarting","isCaptain","validationStatus","rejectReason","sortNo","createTime","updateTime","isDelete"],
    "BasketTournamentRegistrationAudit": ["id","registrationId","auditAction","beforeStatus","afterStatus","auditorId","auditorName","auditRemark","auditTime","createTime","updateTime","isDelete"],
    "BasketFileAsset": ["id","storageProvider","bucketName","objectKey","fileName","originalName","fileExt","mimeType","fileSize","fileHash","accessUrl","usageType","ownerMemberId","fileStatus","remark","createTime","updateTime","isDelete"],
    "BasketMessageTask": ["id","taskNo","messageType","title","contentText","bizType","bizId","sendScope","sendStatus","scheduledTime","sentTime","totalCount","successCount","failCount","creatorId","createTime","updateTime","isDelete"],
    "BasketMessageReceiver": ["id","taskId","memberId","deliveryChannel","deliveryStatus","readStatus","sentTime","readTime","failureReason","redirectType","redirectValue","createTime","updateTime","isDelete"],
    "BasketAccountSecurityLog": ["id","memberId","eventType","eventTitle","oldValueMasked","newValueMasked","operatorType","operatorId","clientIp","terminalType","eventTime","remark","createTime","updateTime","isDelete"],
    "BasketMemberAccount": ["id","accountNo","username","email","mobile","accountStatus","createTime","updateTime","isDelete"],
    "BasketDisciplineRecord": ["id","recordNo","seasonId","matchId","teamId","playerId","memberId","disciplineType","title","disciplineStatus","effectiveStartTime","effectiveEndTime","createTime","updateTime","isDelete"],
}

def rm(cls):
    cols = FIELDS[cls]
    lines = [f'        <result property="{c}" column="{camel_to_snake(c)}" />' for c in cols if c != "id"]
    return f'''    <resultMap type="com.zhs.basket.domain.{cls}" id="{cls}Result">
        <id property="id" column="id" />
{chr(10).join(lines)}
    </resultMap>'''

def cols_sql(cls):
    return ", ".join(camel_to_snake(c) for c in FIELDS[cls])

def gen_basic_xml(cls):
    table = TABLE[cls]
    ns = f"com.zhs.basket.mapper.{cls}Mapper"
    rm_block = rm(cls)
    col_list = cols_sql(cls)
    insert_cols = ", ".join(camel_to_snake(c) for c in FIELDS[cls] if c != "id")
    insert_vals = ", ".join("#{" + c + "}" for c in FIELDS[cls] if c != "id")
    return f'''<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{ns}">
{rm_block}

    <sql id="selectVo">select {col_list} from {table}</sql>

    <select id="selectById" resultMap="{cls}Result">
        <include refid="selectVo"/> where id = #{{id}} and is_delete = 0
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        insert into {table} ({insert_cols}) values ({insert_vals})
    </insert>

    <update id="updateById">
        update {table}
        <set>
            <foreach collection="_parameter.getClass().getDeclaredFields()" item="f" separator=""> </foreach>
        </set>
        where id = #{{id}} and is_delete = 0
    </update>
</mapper>'''

# Generate simplified XML with explicit update for each entity used in services
# We'll hand-craft specialized mappers below

SPECIAL = {}

# PlayerCertApplicationMapper
w(os.path.join(XML_M, "BasketPlayerCertApplicationMapper.xml"), '''<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.zhs.basket.mapper.BasketPlayerCertApplicationMapper">
''' + rm("BasketPlayerCertApplication") + '''
    <select id="selectList" resultMap="BasketPlayerCertApplicationResult">
        select a.* from basket_player_cert_application a
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
</mapper>''')

w(os.path.join(JAVA_M, "BasketPlayerCertApplicationMapper.java"), '''package com.zhs.basket.mapper;
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
}''')

# Continue generating other mappers similarly...
print("Special mappers part1 done")
