package com.zhs.basket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Data;

@Data
public class EligibleCaptainVo {
    /** 雪花 ID，序列化为字符串避免前端 Number 精度丢失 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;
    /** 用户ID（历史字段 memberId） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String playerNo;
    private String nickname;
    private String gameNickname;
    private Integer certificationStatus;
    /** 有效平台游戏 ID 列表（展示用） */
    private List<PlatformGameIdItemVo> platformGameIds;
    /** 完整摘要 */
    private String gameIdSummary;
}
