package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户敏感信息表
 */
@Data
@TableName("user_sensitive_info")
public class UserSensitiveInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID（关联 user.id） */
    private Long userId;

    /** 微信openId */
    private String openId;

    /** 微信unionId */
    private String unionId;

    /** 小程序appId */
    private String miniAppId;

    /** 公众号appId */
    private String officialAppId;

    /** 删除标志：0=未删除 1=已删除 */
    private Integer isDelete;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
