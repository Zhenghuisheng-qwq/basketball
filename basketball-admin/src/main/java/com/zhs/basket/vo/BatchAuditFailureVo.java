package com.zhs.basket.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class BatchAuditFailureVo {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String message;
}
