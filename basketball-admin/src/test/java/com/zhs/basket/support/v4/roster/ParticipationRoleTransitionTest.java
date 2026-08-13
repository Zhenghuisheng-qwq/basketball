package com.zhs.basket.support.v4.roster;

import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParticipationRoleTransitionTest {

    @Test
    void unallocatedToAllocated_ok() {
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.UNALLOCATED, ParticipationRoleEnum.FORMAL));
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.UNALLOCATED, ParticipationRoleEnum.CANDIDATE));
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.UNALLOCATED, ParticipationRoleEnum.EXCLUDED));
    }

    @Test
    void sameRole_idempotent() {
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.FORMAL, ParticipationRoleEnum.FORMAL));
    }

    @Test
    void formalCandidateExcluded_canSwap() {
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.FORMAL, ParticipationRoleEnum.CANDIDATE));
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.CANDIDATE, ParticipationRoleEnum.EXCLUDED));
        assertDoesNotThrow(() -> ParticipationRoleTransition.assertCanTransit(
                ParticipationRoleEnum.EXCLUDED, ParticipationRoleEnum.FORMAL));
    }
}
