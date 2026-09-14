package com.zhs.service.support;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.zhs.response.v4.publicapi.V4PublicMatchDetailResp;
import com.zhs.response.v4.publicapi.V4PublicStandingItemResp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class BasketPublicCacheJsonRoundtripTest {

    private ObjectMapper cacheMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY);
        return mapper;
    }

    @Test
    void matchDetailRoundtripKeepsLongDateAndStatus() throws Exception {
        V4PublicMatchDetailResp vo = new V4PublicMatchDetailResp();
        vo.setMatchId(2099501719001060300L);
        vo.setDivisionId(177L);
        vo.setMatchStatus("FINISHED");
        vo.setMatchTime(new Date(1_700_000_000_000L));
        vo.setHomeScore(88);
        vo.setAwayScore(70);
        String json = cacheMapper().writeValueAsString(vo);
        Object raw = cacheMapper().readValue(json, Object.class);
        assertInstanceOf(V4PublicMatchDetailResp.class, raw);
        V4PublicMatchDetailResp back = (V4PublicMatchDetailResp) raw;
        assertEquals(2099501719001060300L, back.getMatchId());
        assertEquals("FINISHED", back.getMatchStatus());
        assertEquals(88, back.getHomeScore());
    }

    @Test
    void standingListRoundtripKeepsBigDecimal() throws Exception {
        V4PublicStandingItemResp row = new V4PublicStandingItemResp();
        row.setEventTeamId(11L);
        row.setStandingPoints(12);
        row.setWinRate(new BigDecimal("0.75"));
        row.setWinCount(4);
        List<V4PublicStandingItemResp> list = new ArrayList<>();
        list.add(row);
        String json = cacheMapper().writeValueAsString(list);
        Object raw = cacheMapper().readValue(json, Object.class);
        assertInstanceOf(List.class, raw);
        @SuppressWarnings("unchecked")
        List<V4PublicStandingItemResp> back = (List<V4PublicStandingItemResp>) raw;
        assertEquals(1, back.size());
        assertEquals(11L, back.get(0).getEventTeamId());
        assertEquals(0, new BigDecimal("0.75").compareTo(back.get(0).getWinRate()));
    }
}