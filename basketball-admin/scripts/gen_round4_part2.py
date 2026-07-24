# -*- coding: utf-8 -*-
import os, textwrap

ROOT = os.path.join(os.path.dirname(__file__), "..")

def w(rel, content):
    path = os.path.join(ROOT, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content.strip() + "\n")
    print("W", os.path.basename(path))

# ---- domain entities ----
DOMAIN_FIELDS = {
"BasketPlayerCertApplication": [
    ("Long", "id"), ("String", "applicationNo"), ("Long", "memberId"), ("Long", "playerId"),
    ("String", "applicationType"), ("String", "realName"), ("String", "idType"), ("String", "idNoCipher"),
    ("String", "idNoMasked"), ("String", "gameTerminal"), ("String", "gameAccount"), ("String", "gameNickname"),
    ("String", "positionCode"), ("String", "contactMobile"), ("Integer", "applicationStatus"),
    ("java.util.Date", "submittedTime"), ("java.util.Date", "reviewedTime"), ("Long", "currentAuditorId"),
    ("String", "reviewRemark"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketPlayerCertAudit": [
    ("Long", "id"), ("Long", "applicationId"), ("String", "auditAction"), ("Integer", "beforeStatus"),
    ("Integer", "afterStatus"), ("Long", "auditorId"), ("String", "auditorName"), ("String", "auditRemark"),
    ("java.util.Date", "auditTime"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketPlayerCertMaterial": [
    ("Long", "id"), ("Long", "applicationId"), ("String", "materialType"), ("Long", "fileId"),
    ("String", "materialName"), ("Integer", "sortNo"), ("Integer", "materialStatus"), ("String", "remark"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketPlayerProfile": [
    ("Long", "id"), ("Long", "memberId"), ("String", "playerNo"), ("String", "realName"), ("String", "idType"),
    ("String", "idNoCipher"), ("String", "idNoMasked"), ("String", "gameTerminal"), ("String", "gameAccount"),
    ("String", "gameNickname"), ("String", "positionCode"), ("String", "dominantHand"),
    ("java.math.BigDecimal", "heightCm"), ("java.math.BigDecimal", "weightKg"), ("Long", "currentTeamId"),
    ("Integer", "certificationStatus"), ("Integer", "playerStatus"), ("java.util.Date", "certifiedTime"),
    ("Integer", "profileVisible"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketCaptainQualification": [
    ("Long", "id"), ("String", "qualificationNo"), ("Long", "memberId"), ("Long", "playerId"),
    ("Integer", "qualificationStatus"), ("Integer", "isCurrent"), ("Integer", "paymentConfirmed"),
    ("String", "paymentReference"), ("String", "grantReason"), ("Long", "grantedBy"), ("java.util.Date", "grantedTime"),
    ("java.util.Date", "validFrom"), ("java.util.Date", "validTo"), ("Long", "revokedBy"), ("java.util.Date", "revokedTime"),
    ("String", "revokeReason"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTeam": [
    ("Long", "id"), ("String", "teamNo"), ("String", "teamName"), ("String", "shortName"), ("String", "englishName"),
    ("Long", "logoFileId"), ("String", "regionCode"), ("String", "regionName"), ("String", "gameTerminal"),
    ("String", "description"), ("Long", "contactMemberId"), ("Long", "captainMemberId"), ("Long", "captainPlayerId"),
    ("Integer", "maxMemberCount"), ("Integer", "currentMemberCount"), ("Integer", "teamStatus"), ("Integer", "auditStatus"),
    ("Integer", "publicStatus"), ("java.util.Date", "establishedDate"), ("java.util.Date", "dissolvedTime"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTeamAuditRecord": [
    ("Long", "id"), ("Long", "teamId"), ("String", "auditType"), ("String", "auditAction"), ("Integer", "beforeStatus"),
    ("Integer", "afterStatus"), ("Long", "auditorId"), ("String", "auditorName"), ("String", "auditRemark"),
    ("java.util.Date", "auditTime"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTeamMember": [
    ("Long", "id"), ("String", "membershipNo"), ("Long", "teamId"), ("Long", "memberId"), ("Long", "playerId"),
    ("String", "roleType"), ("String", "rosterType"), ("String", "jerseyNo"), ("String", "joinSource"),
    ("Integer", "membershipStatus"), ("Integer", "isCurrent"), ("java.util.Date", "joinTime"), ("java.util.Date", "leaveTime"),
    ("String", "leaveReason"), ("String", "remark"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTournament": [
    ("Long", "id"), ("String", "tournamentNo"), ("String", "tournamentName"), ("String", "shortName"), ("String", "englishName"),
    ("Long", "logoFileId"), ("Long", "coverFileId"), ("String", "tournamentType"), ("String", "gameTerminal"),
    ("String", "formatCode"), ("String", "organizerName"), ("String", "description"), ("Integer", "tournamentStatus"),
    ("Integer", "publicStatus"), ("Integer", "displayOrder"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTournamentSeason": [
    ("Long", "id"), ("String", "seasonNo"), ("Long", "tournamentId"), ("String", "seasonName"), ("Integer", "seasonYear"),
    ("java.util.Date", "registrationStartTime"), ("java.util.Date", "registrationEndTime"), ("java.util.Date", "seasonStartTime"),
    ("java.util.Date", "seasonEndTime"), ("Long", "venueId"), ("Integer", "maxTeamCount"), ("Integer", "minRosterCount"),
    ("Integer", "maxRosterCount"), ("Integer", "seasonStatus"), ("Long", "currentStageId"), ("Long", "championTeamId"),
    ("Long", "runnerUpTeamId"), ("Integer", "publicStatus"), ("String", "description"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTournamentRule": [
    ("Long", "id"), ("Long", "seasonId"), ("String", "ruleType"), ("String", "ruleTitle"), ("String", "ruleContent"),
    ("String", "versionNo"), ("java.util.Date", "effectiveTime"), ("Integer", "ruleStatus"), ("Integer", "displayOrder"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTournamentRegistration": [
    ("Long", "id"), ("String", "registrationNo"), ("Long", "seasonId"), ("Long", "teamId"), ("Long", "captainMemberId"),
    ("Long", "captainPlayerId"), ("String", "contactName"), ("String", "contactMobile"), ("String", "contactEmail"),
    ("Integer", "registrationStatus"), ("Integer", "lineupCount"), ("java.util.Date", "submittedTime"), ("java.util.Date", "auditTime"),
    ("java.util.Date", "withdrawalTime"), ("String", "withdrawalReason"), ("Long", "currentAuditorId"), ("String", "remark"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTournamentRegistrationPlayer": [
    ("Long", "id"), ("Long", "registrationId"), ("Long", "seasonId"), ("Long", "teamId"), ("Long", "playerId"),
    ("Long", "memberId"), ("Long", "teamMemberId"), ("String", "rosterRole"), ("String", "jerseyNo"),
    ("Integer", "isStarting"), ("Integer", "isCaptain"), ("Integer", "validationStatus"), ("String", "rejectReason"),
    ("Integer", "sortNo"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketTournamentRegistrationAudit": [
    ("Long", "id"), ("Long", "registrationId"), ("String", "auditAction"), ("Integer", "beforeStatus"),
    ("Integer", "afterStatus"), ("Long", "auditorId"), ("String", "auditorName"), ("String", "auditRemark"),
    ("java.util.Date", "auditTime"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketFileAsset": [
    ("Long", "id"), ("String", "storageProvider"), ("String", "bucketName"), ("String", "objectKey"), ("String", "fileName"),
    ("String", "originalName"), ("String", "fileExt"), ("String", "mimeType"), ("Long", "fileSize"), ("String", "fileHash"),
    ("String", "accessUrl"), ("String", "usageType"), ("Long", "ownerMemberId"), ("Integer", "fileStatus"), ("String", "remark"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketMessageTask": [
    ("Long", "id"), ("String", "taskNo"), ("String", "messageType"), ("String", "title"), ("String", "contentText"),
    ("String", "bizType"), ("Long", "bizId"), ("String", "sendScope"), ("Integer", "sendStatus"), ("java.util.Date", "scheduledTime"),
    ("java.util.Date", "sentTime"), ("Integer", "totalCount"), ("Integer", "successCount"), ("Integer", "failCount"),
    ("Long", "creatorId"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketMessageReceiver": [
    ("Long", "id"), ("Long", "taskId"), ("Long", "memberId"), ("String", "deliveryChannel"), ("Integer", "deliveryStatus"),
    ("Integer", "readStatus"), ("java.util.Date", "sentTime"), ("java.util.Date", "readTime"), ("String", "failureReason"),
    ("String", "redirectType"), ("String", "redirectValue"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketAccountSecurityLog": [
    ("Long", "id"), ("Long", "memberId"), ("String", "eventType"), ("String", "eventTitle"), ("String", "oldValueMasked"),
    ("String", "newValueMasked"), ("String", "operatorType"), ("Long", "operatorId"), ("String", "clientIp"),
    ("String", "terminalType"), ("java.util.Date", "eventTime"), ("String", "remark"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketMemberAccount": [
    ("Long", "id"), ("String", "accountNo"), ("String", "username"), ("String", "email"), ("String", "mobile"),
    ("Integer", "accountStatus"), ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
"BasketDisciplineRecord": [
    ("Long", "id"), ("String", "recordNo"), ("Long", "seasonId"), ("Long", "matchId"), ("Long", "teamId"),
    ("Long", "playerId"), ("Long", "memberId"), ("String", "disciplineType"), ("String", "title"),
    ("Integer", "disciplineStatus"), ("java.util.Date", "effectiveStartTime"), ("java.util.Date", "effectiveEndTime"),
    ("java.util.Date", "createTime"), ("java.util.Date", "updateTime"), ("Integer", "isDelete"),
],
}

def camel_to_snake(name):
    import re
    s1 = re.sub('(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub('([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

for cls, fields in DOMAIN_FIELDS.items():
    imports = set()
    body = []
    for typ, fname in fields:
        if typ.startswith("java."):
            imports.add(f"import {typ};")
        body.append(f"    private {typ.split('.')[-1] if typ.startswith('java.') else typ} {fname};")
    imp = "\n".join(sorted(imports))
    content = f"""package com.zhs.basket.domain;

import lombok.Data;
{imp}

@Data
public class {cls} {{
{chr(10).join(body)}
}}
"""
    w(f"src/main/java/com/zhs/basket/domain/{cls}.java", content)

print("Domain done")
