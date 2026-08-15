package com.zhs.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.response.v4.publicapi.V4PublicScheduleItemResp;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.sql.Time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlexibleLocalTimeTypeHandlerTest {

    private final FlexibleLocalTimeTypeHandler handler = new FlexibleLocalTimeTypeHandler();

    @Test
    void convertsDurationAndSqlTime() throws Exception {
        var durationMethod = FlexibleLocalTimeTypeHandler.class.getDeclaredMethod("convert", Object.class);
        durationMethod.setAccessible(true);
        assertEquals(LocalTime.of(21, 0), durationMethod.invoke(handler, Duration.ofSeconds(75600)));
        assertEquals(LocalTime.of(22, 0), durationMethod.invoke(handler, Time.valueOf("22:00:00")));
        assertEquals(LocalTime.of(21, 0), durationMethod.invoke(handler, "21:00"));
    }

    @Test
    void scheduleItemSerializesDateTimeAsString() throws Exception {
        V4PublicScheduleItemResp item = new V4PublicScheduleItemResp();
        item.setId(32L);
        item.setUnitType("MATCH_SERIES");
        item.setScheduleDate("2026-08-17");
        item.setStartTime("22:00");
        String json = new ObjectMapper().writeValueAsString(item);
        assertTrue(json.contains("\"scheduleDate\":\"2026-08-17\""));
        assertTrue(json.contains("\"startTime\":\"22:00\""));
    }
}
