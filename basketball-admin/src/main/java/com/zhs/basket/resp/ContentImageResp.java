package com.zhs.basket.resp;

import lombok.Data;

@Data
public class ContentImageResp {
    private Long fileId;
    private String accessUrl;
    private String originalName;
}
