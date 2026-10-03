package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.basket.support.v4.V4PublicReadCachePublisher;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4SeasonServiceImplDeleteTest {

    @InjectMocks
    private BasketV4SeasonServiceImpl seasonService;

    @Mock
    private BasketSeasonMapper seasonMapper;
    @Mock
    private BasketSeriesMapper seriesMapper;
    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private V4PublicReadCachePublisher publicReadCachePublisher;

    @Test
    void case1_draftSeasonWithoutDivisionCanDelete() {
        stubSeason(1L, PublishStatusEnum.DRAFT.getCode());
        when(seasonMapper.countDivisionBySeasonId(1L)).thenReturn(0);
        when(seasonMapper.softDelete(1L)).thenReturn(1);

        seasonService.delete(1L);

        verify(seasonMapper).softDelete(1L);
        verify(publicReadCachePublisher).seasonMutation(1L);
    }

    @Test
    void case2_offlineSeasonWithoutDivisionCanDelete() {
        stubSeason(2L, PublishStatusEnum.OFFLINE.getCode());
        when(seasonMapper.countDivisionBySeasonId(2L)).thenReturn(0);
        when(seasonMapper.softDelete(2L)).thenReturn(1);

        seasonService.delete(2L);

        verify(seasonMapper).softDelete(2L);
    }

    @Test
    void case3_publishedSeasonCannotDelete() {
        stubSeason(3L, PublishStatusEnum.PUBLISHED.getCode());

        ServiceException ex = assertThrows(ServiceException.class, () -> seasonService.delete(3L));

        assertEquals("已发布赛季不能直接删除，请先下线", ex.getMessage());
        verify(seasonMapper, never()).softDelete(3L);
    }

    @Test
    void case4_draftOrOfflineSeasonWithDivisionCannotDelete() {
        stubSeason(4L, PublishStatusEnum.DRAFT.getCode());
        when(seasonMapper.countDivisionBySeasonId(4L)).thenReturn(1);

        ServiceException draftEx = assertThrows(ServiceException.class, () -> seasonService.delete(4L));
        assertEquals("存在平台赛区时不允许删除 Season，请先处理赛区", draftEx.getMessage());

        stubSeason(5L, PublishStatusEnum.OFFLINE.getCode());
        when(seasonMapper.countDivisionBySeasonId(5L)).thenReturn(2);
        ServiceException offlineEx = assertThrows(ServiceException.class, () -> seasonService.delete(5L));
        assertEquals("存在平台赛区时不允许删除 Season，请先处理赛区", offlineEx.getMessage());

        verify(seasonMapper, never()).softDelete(4L);
        verify(seasonMapper, never()).softDelete(5L);
    }

    private void stubSeason(Long id, String publishStatus) {
        BasketSeason season = new BasketSeason();
        season.setId(id);
        season.setPublishStatus(publishStatus);
        when(seasonMapper.selectById(id)).thenReturn(season);
    }
}
