package com.zhs.request.team;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamCaptainTransferReq implements Serializable {
    private String teamMemberId;
}
