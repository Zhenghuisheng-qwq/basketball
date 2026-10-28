package com.zhs.response.v4.publicapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class V4PublicTeamArchiveResp {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceTeamId;

    private List<V4PublicTeamNameHistoryItem> nameHistory = new ArrayList<>();

    private List<V4PublicTeamLogoHistoryItem> logoHistory = new ArrayList<>();

    private List<V4PublicTeamHonorItem> honors = new ArrayList<>();

    @Data
    public static class V4PublicTeamNameHistoryItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long changeId;
        private String oldName;
        private String newName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date submittedAt;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date approvedAt;
    }

    @Data
    public static class V4PublicTeamLogoHistoryItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long changeId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long oldLogoFileId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long newLogoFileId;
        private String oldLogoUrl;
        private String newLogoUrl;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date submittedAt;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date approvedAt;
    }

    @Data
    public static class V4PublicTeamHonorItem {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long honorId;
        private String honorCode;
        private String honorTitle;
        private String competitionName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date awardDate;
    }
}