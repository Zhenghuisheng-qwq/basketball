package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.publicapi.PublicAnnouncementAdjacentResp;
import com.zhs.response.publicapi.PublicAnnouncementCategoryResp;
import com.zhs.response.publicapi.PublicAnnouncementDetailResp;
import com.zhs.response.publicapi.PublicAnnouncementItemResp;

import java.util.List;

public interface IBasketPublicAnnouncementService {

    List<PublicAnnouncementCategoryResp> listCategories();

    IPage<PublicAnnouncementItemResp> pageAnnouncements(Integer currentPage, Integer pageSize,
                                                        String categoryCode, String keyword,
                                                        String sortField, String sortOrder);

    PublicAnnouncementDetailResp getDetail(Long announcementId);

    PublicAnnouncementAdjacentResp getPreviousNext(Long announcementId);

    List<PublicAnnouncementItemResp> listLatest(int limit);
}
