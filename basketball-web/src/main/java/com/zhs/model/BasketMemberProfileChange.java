package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_member_profile_change")
public class BasketMemberProfileChange implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String changeNo;
    private Long memberId;
    private Long playerId;
    private String oldNickname;
    private String newNickname;
    private Long oldAvatarFileId;
    private Long newAvatarFileId;
    private String oldAvatarUrl;
    private String newAvatarUrl;
    private String changeSummary;
    private Integer auditStatus;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private Integer activeFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
