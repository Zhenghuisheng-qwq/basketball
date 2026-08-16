package com.zhs.basket.support;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.DisciplineTargetTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisciplinePenaltyTargetCompatibilityPolicyTest {

    @ParameterizedTest(name = "{0} + {1} => {2}")
    @MethodSource("allCombinations")
    void matrixCoversAllPenaltyAndTargetPairs(DisciplinePenaltyCodeEnum penalty,
                                              DisciplineTargetTypeEnum target,
                                              boolean allowed) {
        assertEquals(allowed, DisciplinePenaltyTargetCompatibilityPolicy.supports(penalty, target));
        if (allowed) {
            assertDoesNotThrow(() -> DisciplinePenaltyTargetCompatibilityPolicy.validate(penalty, target));
        } else {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> DisciplinePenaltyTargetCompatibilityPolicy.validate(penalty, target));
            assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
        }
    }

    @Test
    void eventDisqualificationOnlyAllowsEventTeam() {
        assertTrue(DisciplinePenaltyTargetCompatibilityPolicy.supports(
                DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION, DisciplineTargetTypeEnum.EVENT_TEAM));
        assertFalse(DisciplinePenaltyTargetCompatibilityPolicy.supports(
                DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION, DisciplineTargetTypeEnum.PLAYER));
        assertFalse(DisciplinePenaltyTargetCompatibilityPolicy.supports(
                DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION, DisciplineTargetTypeEnum.PERMANENT_TEAM));
        assertFalse(DisciplinePenaltyTargetCompatibilityPolicy.supports(
                DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION, DisciplineTargetTypeEnum.ACCOUNT));
    }

    @Test
    void playerPlusEventDisqualificationRejectedEvenWithEventTeamId() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                DisciplinePenaltyTargetCompatibilityPolicy.validateCreate(
                        "EVENT_DISQUALIFICATION", "PLAYER", 231L));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
    }

    @Test
    void eventTeamDisqualificationRequiresEventTeamId() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                DisciplinePenaltyTargetCompatibilityPolicy.validateCreate(
                        "EVENT_DISQUALIFICATION", "EVENT_TEAM", null));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_DQ_EVENT_TEAM_REQUIRED, ex.getMessage());
        assertDoesNotThrow(() -> DisciplinePenaltyTargetCompatibilityPolicy.validateCreate(
                "EVENT_DISQUALIFICATION", "EVENT_TEAM", 231L));
    }

    @Test
    void validateRecordRejectsHistoricalPlayerDisqualificationDraft() {
        BasketDisciplineRecord record = new BasketDisciplineRecord();
        record.setTargetType("PLAYER");
        record.setPenaltyCode("EVENT_DISQUALIFICATION");
        record.setEventTeamId(231L);
        record.setPlayerId(1L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> DisciplinePenaltyTargetCompatibilityPolicy.validateRecord(record));
        assertEquals(DisciplinePenaltyTargetCompatibilityPolicy.MSG_INCOMPATIBLE, ex.getMessage());
    }

    @Test
    void adminUiMatrixMatchesBackendPolicy() throws Exception {
        Path uiFile = Path.of("..", "basketball-admin-ui", "src", "utils", "disciplineDisplay.ts")
                .toAbsolutePath().normalize();
        String source = Files.readString(uiFile, StandardCharsets.UTF_8);
        int start = source.indexOf("export const DISCIPLINE_PENALTY_TARGET_MATRIX");
        assertTrue(start >= 0, "Admin UI matrix missing");
        int brace = source.indexOf('{', start);
        int depth = 0;
        int end = -1;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        assertTrue(end > brace, "Admin UI matrix block not closed");
        String block = source.substring(brace, end + 1);
        for (DisciplinePenaltyCodeEnum penalty : DisciplinePenaltyCodeEnum.values()) {
            for (DisciplineTargetTypeEnum target : DisciplineTargetTypeEnum.values()) {
                boolean backend = DisciplinePenaltyTargetCompatibilityPolicy.supports(penalty, target);
                boolean frontend = uiAllows(block, penalty.name(), target.getCode());
                assertEquals(backend, frontend, penalty + " + " + target);
            }
        }
    }

    static Stream<Arguments> allCombinations() {
        List<Arguments> rows = new ArrayList<>();
        for (DisciplinePenaltyCodeEnum penalty : DisciplinePenaltyCodeEnum.values()) {
            for (DisciplineTargetTypeEnum target : DisciplineTargetTypeEnum.values()) {
                rows.add(Arguments.of(penalty, target, expected(penalty, target)));
            }
        }
        assertEquals(44, rows.size());
        return rows.stream();
    }

    private static boolean expected(DisciplinePenaltyCodeEnum penalty, DisciplineTargetTypeEnum target) {
        return switch (penalty) {
            case WARNING -> target == DisciplineTargetTypeEnum.PLAYER
                    || target == DisciplineTargetTypeEnum.PERMANENT_TEAM
                    || target == DisciplineTargetTypeEnum.EVENT_TEAM;
            case MATCH_SUSPENSION, DATE_RANGE_SUSPENSION, REGISTRATION_BAN, TRANSFER_BAN, CAPTAIN_BAN
                    -> target == DisciplineTargetTypeEnum.PLAYER;
            case POINT_DEDUCTION, EVENT_DISQUALIFICATION -> target == DisciplineTargetTypeEnum.EVENT_TEAM;
            case MATCH_ELIGIBILITY_CANCEL, TEAM_SUSPENSION, PLAYER_QUALIFICATION_SUSPENSION -> false;
        };
    }

    private static boolean uiAllows(String matrixBlock, String penalty, String target) {
        Matcher matcher = Pattern.compile(penalty + "\\s*:\\s*\\[([^]]*)]").matcher(matrixBlock);
        if (!matcher.find()) {
            return false;
        }
        return matcher.group(1).contains("'" + target + "'");
    }
}
