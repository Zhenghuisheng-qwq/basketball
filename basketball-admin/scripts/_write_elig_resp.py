from pathlib import Path

# --- V4AvailableDivisionResp ---
Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\response\v4\V4AvailableDivisionResp.java").write_text(
'''package com.zhs.response.v4;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class V4AvailableDivisionResp {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    private String seriesName;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private String registrationMode;
    private String gameMode;
    private String divisionStatus;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long grantTeamId;
    private String grantTeamName;
    private Integer maxTeamCount;
    private Integer approvedTeamCount;
    private Integer remainingTeamCount;
    private String myGrantStatus;
    private String myRegistrationStatus;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long myRegistrationId;
    private Boolean canRegister;
    private Boolean canViewRegistration;
    private String cannotRegisterReason;
    private String cannotRegisterReasonCode;
    private String registrationWindowStatus;
    private String registrationWindowStatusLabel;
}
''', encoding='utf-8', newline='\n')
print('resp ok')

# patch mapper xml - add divisionStatus + myRegistrationId
xml_path = Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\resources\mapper\v4\BasketV4PlayerRegistrationMapper.xml")
xml = xml_path.read_text(encoding='utf-8')
old = '''            d.game_mode as gameMode,
            d.lineup_min_count as lineupMinCount,'''
new = '''            d.game_mode as gameMode,
            d.division_status as divisionStatus,
            d.lineup_min_count as lineupMinCount,'''
if old not in xml:
    raise SystemExit('mapper patch1 failed')
xml = xml.replace(old, new, 1)
old2 = '''            mr.registration_status as myRegistrationStatus
        from basket_division d'''
new2 = '''            mr.registration_status as myRegistrationStatus,
            mr.id as myRegistrationId
        from basket_division d'''
if old2 not in xml:
    raise SystemExit('mapper patch2 failed')
xml = xml.replace(old2, new2, 1)
xml_path.write_text(xml, encoding='utf-8', newline='\n')
print('xml ok')
