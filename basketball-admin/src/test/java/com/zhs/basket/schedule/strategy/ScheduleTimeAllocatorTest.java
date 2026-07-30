package com.zhs.basket.schedule.strategy;

import com.zhs.basket.enums.ScheduleAllocationModeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleTimeAllocatorTest {

    @Test
    void distribute_fillFirst_sixMatches_allInFirstSlot() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(6, 2, 12, ScheduleAllocationModeEnum.FILL_FIRST);
        assertArrayEquals(new int[]{6, 0}, counts);
    }

    @Test
    void distribute_fillFirst_twelveMatches_allInFirstSlot() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(12, 2, 12, ScheduleAllocationModeEnum.FILL_FIRST);
        assertArrayEquals(new int[]{12, 0}, counts);
    }

    @Test
    void distribute_fillFirst_thirteenMatches_splitAcrossSlots() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(13, 2, 12, ScheduleAllocationModeEnum.FILL_FIRST);
        assertArrayEquals(new int[]{12, 1}, counts);
    }

    @Test
    void distribute_fillFirst_twentyFourMatches_evenSplitAcrossSlots() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(24, 2, 12, ScheduleAllocationModeEnum.FILL_FIRST);
        assertArrayEquals(new int[]{12, 12}, counts);
    }

    @Test
    void distribute_evenSplit_twelveMatches_sixAndSix() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(12, 2, 12, ScheduleAllocationModeEnum.EVEN_SPLIT);
        assertArrayEquals(new int[]{6, 6}, counts);
    }

    @Test
    void distribute_evenSplit_thirteenMatches_sevenAndSix() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(13, 2, 12, ScheduleAllocationModeEnum.EVEN_SPLIT);
        assertArrayEquals(new int[]{7, 6}, counts);
    }

    @Test
    void distribute_evenSplit_twentyFourMatches_twelveAndTwelve() {
        int[] counts = ScheduleTimeAllocator.distributeMatchCounts(24, 2, 12, ScheduleAllocationModeEnum.EVEN_SPLIT);
        assertArrayEquals(new int[]{12, 12}, counts);
    }

    @Test
    void distribute_exceedsCapacity_shouldThrow() {
        assertThrows(Exception.class, () ->
                ScheduleTimeAllocator.distributeMatchCounts(25, 2, 12, ScheduleAllocationModeEnum.FILL_FIRST));
    }
}
