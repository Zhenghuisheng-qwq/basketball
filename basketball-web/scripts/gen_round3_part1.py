#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate Round 3 team backend Java sources (UTF-8)."""
import os

ROOT = r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs"


def w(rel, content):
    path = os.path.join(ROOT, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content.strip() + "\n")


FILES = {}

FILES["config/AccountTeamProperties.java"] = """
package com.zhs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "team")
public class AccountTeamProperties {

    private Invitation invitation = new Invitation();

    private int defaultMaxMemberCount = 20;

    @Data
    public static class Invitation {
        private int expireDays = 7;
    }
}
"""

FILES["service/IBusinessMessageService.java"] = """
package com.zhs.service;

public interface IBusinessMessageService {

    void sendSiteMessage(Long receiverMemberId, String title, String content,
                         String bizType, Long bizId, String redirectType, String redirectValue);
}
"""

FILES["service/impl/BusinessMessageServiceImpl.java"] = """
package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.zhs.dao.BasketMessageReceiverDao;
import com.zhs.dao.BasketMessageTaskDao;
import com.zhs.enums.MessageReadStatusEnum;
import com.zhs.model.BasketMessageReceiver;
import com.zhs.model.BasketMessageTask;
import com.zhs.service.IBusinessMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class BusinessMessageServiceImpl implements IBusinessMessageService {

    private static final Integer NOT_DELETED = 0;
    private static final int SEND_STATUS_SENT = 1;
    private static final int DELIVERY_STATUS_SENT = 1;
    private static final String MESSAGE_TYPE_SITE = "SITE";
    private static final String SEND_SCOPE_SINGLE = "SINGLE";
    private static final String DELIVERY_CHANNEL_SITE = "SITE";

    @Resource
    private BasketMessageTaskDao messageTaskDao;
    @Resource
    private BasketMessageReceiverDao messageReceiverDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendSiteMessage(Long receiverMemberId, String title, String content,
                                String bizType, Long bizId, String redirectType, String redirectValue) {
        if (receiverMemberId == null) {
            return;
        }
        Date now = new Date();
        BasketMessageTask task = new BasketMessageTask();
        task.setTaskNo("MT" + IdUtil.getSnowflakeNextIdStr());
        task.setMessageType(MESSAGE_TYPE_SITE);
        task.setTitle(title);
        task.setContentText(content);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setSendScope(SEND_SCOPE_SINGLE);
        task.setSendStatus(SEND_STATUS_SENT);
        task.setSentTime(now);
        task.setTotalCount(1);
        task.setSuccessCount(1);
        task.setFailCount(0);
        task.setCreatorId(receiverMemberId);
        task.setIsDelete(NOT_DELETED);
        messageTaskDao.insert(task);

        BasketMessageReceiver receiver = new BasketMessageReceiver();
        receiver.setTaskId(task.getId());
        receiver.setMemberId(receiverMemberId);
        receiver.setDeliveryChannel(DELIVERY_CHANNEL_SITE);
        receiver.setDeliveryStatus(DELIVERY_STATUS_SENT);
        receiver.setReadStatus(MessageReadStatusEnum.UNREAD.getCode());
        receiver.setSentTime(now);
        receiver.setIsDelete(NOT_DELETED);
        messageReceiverDao.insert(receiver);
    }
}
"""

FILES["util/AccountSecurityLogHelper.java"] = """
package com.zhs.util;

import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.model.BasketAccountSecurityLog;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class AccountSecurityLogHelper {

    private static final Integer NOT_DELETED = 0;
    private static final String OPERATOR_MEMBER = "MEMBER";

    @Resource
    private BasketAccountSecurityLogDao securityLogDao;

    public void write(Long memberId, String eventType, String title,
                      String oldMasked, String newMasked, Long operatorId, String clientIp) {
        BasketAccountSecurityLog logEntity = new BasketAccountSecurityLog();
        logEntity.setMemberId(memberId);
        logEntity.setEventType(eventType);
        logEntity.setEventTitle(title);
        logEntity.setOldValueMasked(oldMasked);
        logEntity.setNewValueMasked(newMasked);
        logEntity.setOperatorType(OPERATOR_MEMBER);
        logEntity.setOperatorId(operatorId);
        logEntity.setClientIp(clientIp);
        logEntity.setEventTime(new Date());
        logEntity.setIsDelete(NOT_DELETED);
        securityLogDao.insert(logEntity);
    }
}
"""

FILES["response/team/CaptainQualificationDetailResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("队长资格详情")
public class CaptainQualificationDetailResp implements Serializable {

    @ApiModelProperty("是否拥有有效资格")
    private Boolean hasQualification;

    @ApiModelProperty("资格编号")
    private String qualificationNo;

    @ApiModelProperty("资格状态")
    private Integer status;

    @ApiModelProperty("资格状态描述")
    private String statusText;

    @ApiModelProperty("缴费确认状态")
    private Integer paymentConfirmed;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("生效时间")
    private Date validFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("失效时间")
    private Date validTo;

    @ApiModelProperty("当前有效标记")
    private Integer isCurrent;

    @ApiModelProperty("是否可创建球队")
    private Boolean canCreateTeam;

    @ApiModelProperty("无效原因")
    private String invalidReason;
}
"""

FILES["request/team/TeamCreateReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("创建球队请求")
public class TeamCreateReq implements Serializable {

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("地区编码")
    private String regionCode;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("球队介绍")
    private String description;

    @ApiModelProperty("公开状态：0 非公开，1 公开")
    private Integer publicStatus;

    @ApiModelProperty("Logo 文件主键")
    private String logoFileId;
}
"""

FILES["request/team/TeamUpdateReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("更新球队请求")
public class TeamUpdateReq implements Serializable {

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("地区编码")
    private String regionCode;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("球队介绍")
    private String description;

    @ApiModelProperty("公开状态")
    private Integer publicStatus;
}
"""

FILES["request/team/TeamLogoBindReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球队 Logo 绑定请求")
public class TeamLogoBindReq implements Serializable {

    @ApiModelProperty("Logo 文件主键")
    private String fileId;
}
"""

FILES["request/team/TeamMemberUpdateReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("更新球队成员请求")
public class TeamMemberUpdateReq implements Serializable {

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("阵容类型：OFFICIAL / TRIAL")
    private String rosterType;

    @ApiModelProperty("备注")
    private String remark;
}
"""

FILES["request/team/TeamInvitationCreateReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("创建球队邀请请求")
public class TeamInvitationCreateReq implements Serializable {

    @ApiModelProperty("被邀请会员主键")
    private String inviteeMemberId;

    @ApiModelProperty("邀请说明")
    private String invitationMessage;
}
"""

FILES["request/team/TeamInvitationRespondReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("响应球队邀请请求")
public class TeamInvitationRespondReq implements Serializable {

    @ApiModelProperty("是否接受")
    private Boolean accept;

    @ApiModelProperty("响应说明")
    private String responseRemark;
}
"""

FILES["request/team/TeamJoinApplicationCreateReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("提交加入球队申请请求")
public class TeamJoinApplicationCreateReq implements Serializable {

    @ApiModelProperty("申请原因")
    private String applyReason;
}
"""

FILES["request/team/TeamJoinApplicationProcessReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("处理加入球队申请请求")
public class TeamJoinApplicationProcessReq implements Serializable {

    @ApiModelProperty("是否通过")
    private Boolean approve;

    @ApiModelProperty("处理说明")
    private String processRemark;
}
"""

FILES["request/team/TeamRegistrationDraftReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("创建赛事报名草稿请求")
public class TeamRegistrationDraftReq implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系电话")
    private String contactMobile;

    @ApiModelProperty("联系邮箱")
    private String contactEmail;

    @ApiModelProperty("备注")
    private String remark;
}
"""

FILES["request/team/TeamRegistrationUpdateReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("更新赛事报名请求")
public class TeamRegistrationUpdateReq implements Serializable {

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系电话")
    private String contactMobile;

    @ApiModelProperty("联系邮箱")
    private String contactEmail;

    @ApiModelProperty("备注")
    private String remark;
}
"""

FILES["request/team/TeamRegistrationRosterItemReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("赛事报名阵容项")
public class TeamRegistrationRosterItemReq implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("是否首发：0 否，1 是")
    private Integer isStarting;

    @ApiModelProperty("是否场上队长：0 否，1 是")
    private Integer isCaptain;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
"""

FILES["request/team/TeamRegistrationRosterReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("保存赛事报名阵容请求")
public class TeamRegistrationRosterReq implements Serializable {

    @ApiModelProperty("阵容列表")
    private List<TeamRegistrationRosterItemReq> rosterItems;
}
"""

FILES["request/team/TeamRegistrationWithdrawReq.java"] = """
package com.zhs.request.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("撤回赛事报名请求")
public class TeamRegistrationWithdrawReq implements Serializable {

    @ApiModelProperty("撤回原因")
    private String withdrawalReason;
}
"""

# Response DTOs
FILES["response/team/TeamSummaryResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队摘要")
public class TeamSummaryResp implements Serializable {

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队编号")
    private String teamNo;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("当前成员数")
    private Integer currentMemberCount;

    @ApiModelProperty("最大成员数")
    private Integer maxMemberCount;

    @ApiModelProperty("球队状态")
    private Integer teamStatus;

    @ApiModelProperty("审核状态")
    private Integer auditStatus;

    @ApiModelProperty("公开状态")
    private Integer publicStatus;

    @ApiModelProperty("我的角色")
    private String myRole;

    @ApiModelProperty("是否队长")
    private Boolean isCaptain;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
"""

FILES["response/team/TeamDetailResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队详情")
public class TeamDetailResp implements Serializable {

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队编号")
    private String teamNo;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("球队简称")
    private String shortName;

    @ApiModelProperty("Logo 文件主键")
    private String logoFileId;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("地区编码")
    private String regionCode;

    @ApiModelProperty("地区名称")
    private String regionName;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("球队介绍")
    private String description;

    @ApiModelProperty("当前成员数")
    private Integer currentMemberCount;

    @ApiModelProperty("最大成员数")
    private Integer maxMemberCount;

    @ApiModelProperty("球队状态")
    private Integer teamStatus;

    @ApiModelProperty("审核状态")
    private Integer auditStatus;

    @ApiModelProperty("公开状态")
    private Integer publicStatus;

    @ApiModelProperty("是否队长")
    private Boolean isCaptain;

    @ApiModelProperty("我的角色")
    private String myRole;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("成立日期")
    private Date establishedDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
"""

FILES["response/team/TeamMemberItemResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队成员项")
public class TeamMemberItemResp implements Serializable {

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("认证姓名")
    private String realName;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("角色")
    private String roleType;

    @ApiModelProperty("阵容类型")
    private String rosterType;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("成员状态")
    private Integer membershipStatus;

    @ApiModelProperty("备注")
    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("加入时间")
    private Date joinTime;
}
"""

FILES["response/team/CandidatePlayerItemResp.java"] = """
package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("候选球员项")
public class CandidatePlayerItemResp implements Serializable {

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("球员编号")
    private String playerNo;

    @ApiModelProperty("认证姓名")
    private String realName;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("场上位置")
    private String positionCode;
}
"""

FILES["response/team/TeamInvitationItemResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("球队邀请项")
public class TeamInvitationItemResp implements Serializable {

    @ApiModelProperty("邀请主键")
    private String invitationId;

    @ApiModelProperty("邀请编号")
    private String invitationNo;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("邀请人会员主键")
    private String inviterMemberId;

    @ApiModelProperty("被邀请会员主键")
    private String inviteeMemberId;

    @ApiModelProperty("被邀请人昵称")
    private String inviteeNickname;

    @ApiModelProperty("邀请说明")
    private String invitationMessage;

    @ApiModelProperty("邀请状态")
    private Integer invitationStatus;

    @ApiModelProperty("邀请状态描述")
    private String invitationStatusText;

    @ApiModelProperty("是否可响应")
    private Boolean canRespond;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("过期时间")
    private Date expireTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
"""

FILES["response/team/TeamJoinApplicationItemResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("加入球队申请项")
public class TeamJoinApplicationItemResp implements Serializable {

    @ApiModelProperty("申请主键")
    private String applicationId;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("申请会员主键")
    private String applicantMemberId;

    @ApiModelProperty("申请人昵称")
    private String applicantNickname;

    @ApiModelProperty("申请原因")
    private String applyReason;

    @ApiModelProperty("申请状态")
    private Integer applicationStatus;

    @ApiModelProperty("申请状态描述")
    private String applicationStatusText;

    @ApiModelProperty("是否可取消")
    private Boolean canCancel;

    @ApiModelProperty("是否可处理")
    private Boolean canProcess;

    @ApiModelProperty("处理说明")
    private String processRemark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
"""

FILES["response/team/AvailableSeasonItemResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("可报名赛季项")
public class AvailableSeasonItemResp implements Serializable {

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季编号")
    private String seasonNo;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("赛事主键")
    private String tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季状态")
    private Integer seasonStatus;

    @ApiModelProperty("最小阵容人数")
    private Integer minRosterCount;

    @ApiModelProperty("最大阵容人数")
    private Integer maxRosterCount;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名开始时间")
    private Date registrationStartTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名结束时间")
    private Date registrationEndTime;

    @ApiModelProperty("是否已存在报名")
    private Boolean hasRegistration;

    @ApiModelProperty("已有报名主键")
    private String registrationId;
}
"""

FILES["response/team/TeamRegistrationItemResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事报名列表项")
public class TeamRegistrationItemResp implements Serializable {

    @ApiModelProperty("报名主键")
    private String registrationId;

    @ApiModelProperty("报名编号")
    private String registrationNo;

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("报名状态")
    private Integer registrationStatus;

    @ApiModelProperty("报名状态描述")
    private String registrationStatusText;

    @ApiModelProperty("阵容人数")
    private Integer lineupCount;

    @ApiModelProperty("是否可编辑")
    private Boolean canEdit;

    @ApiModelProperty("是否可提交")
    private Boolean canSubmit;

    @ApiModelProperty("是否可撤回")
    private Boolean canWithdraw;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submittedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("创建时间")
    private Date createTime;
}
"""

FILES["response/team/TeamRegistrationPlayerItemResp.java"] = """
package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("赛事报名球员项")
public class TeamRegistrationPlayerItemResp implements Serializable {

    @ApiModelProperty("报名球员主键")
    private String registrationPlayerId;

    @ApiModelProperty("球队成员主键")
    private String teamMemberId;

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("认证姓名")
    private String realName;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("球衣号码")
    private String jerseyNo;

    @ApiModelProperty("是否首发")
    private Integer isStarting;

    @ApiModelProperty("是否场上队长")
    private Integer isCaptain;

    @ApiModelProperty("校验状态")
    private Integer validationStatus;

    @ApiModelProperty("排序号")
    private Integer sortNo;
}
"""

FILES["response/team/TeamRegistrationDetailResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("赛事报名详情")
public class TeamRegistrationDetailResp implements Serializable {

    @ApiModelProperty("报名主键")
    private String registrationId;

    @ApiModelProperty("报名编号")
    private String registrationNo;

    @ApiModelProperty("赛季主键")
    private String seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("球队主键")
    private String teamId;

    @ApiModelProperty("球队名称")
    private String teamName;

    @ApiModelProperty("联系人")
    private String contactName;

    @ApiModelProperty("联系电话（脱敏）")
    private String contactMobileMasked;

    @ApiModelProperty("联系邮箱（脱敏）")
    private String contactEmailMasked;

    @ApiModelProperty("报名状态")
    private Integer registrationStatus;

    @ApiModelProperty("报名状态描述")
    private String registrationStatusText;

    @ApiModelProperty("阵容人数")
    private Integer lineupCount;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("是否可编辑")
    private Boolean canEdit;

    @ApiModelProperty("是否可提交")
    private Boolean canSubmit;

    @ApiModelProperty("是否可撤回")
    private Boolean canWithdraw;

    @ApiModelProperty("是否可删除草稿")
    private Boolean canDeleteDraft;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("提交时间")
    private Date submittedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("报名截止时间")
    private Date registrationEndTime;

    @ApiModelProperty("阵容列表")
    private List<TeamRegistrationPlayerItemResp> rosterItems;
}
"""

FILES["response/team/TeamRegistrationAuditItemResp.java"] = """
package com.zhs.response.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("赛事报名审核记录项")
public class TeamRegistrationAuditItemResp implements Serializable {

    @ApiModelProperty("审核记录主键")
    private String auditId;

    @ApiModelProperty("审核动作")
    private String auditAction;

    @ApiModelProperty("审核前状态")
    private Integer beforeStatus;

    @ApiModelProperty("审核后状态")
    private Integer afterStatus;

    @ApiModelProperty("审核人名称")
    private String auditorName;

    @ApiModelProperty("审核说明")
    private String auditRemark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty("审核时间")
    private Date auditTime;
}
"""

for rel, content in FILES.items():
    w(rel, content)

print(f"Generated {len(FILES)} files")
