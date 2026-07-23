package com.zhs.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_login_session")
public class UserLoginSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime loginTime;

    private LocalDateTime expireTime;

    private String deviceType;

    private String clientIp;

    private Integer status;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
