package com.zhs.request.team;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamDissolveApplyReq implements Serializable {
    private String reason;
}
