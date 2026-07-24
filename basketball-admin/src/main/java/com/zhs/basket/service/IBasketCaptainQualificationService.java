package com.zhs.basket.service;

import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.vo.CaptainQualificationDetailVo;
import com.zhs.basket.vo.CaptainQualificationListVo;
import com.zhs.basket.vo.EligibleCaptainVo;
import java.util.List;

public interface IBasketCaptainQualificationService {
    List<EligibleCaptainVo> selectEligibleList();
    List<CaptainQualificationListVo> selectList(CaptainQualificationQuery query);
    CaptainQualificationDetailVo selectDetail(Long qualificationId);
    void grant(CaptainQualificationGrantReq req);
    void update(Long qualificationId, CaptainQualificationUpdateReq req);
    void revoke(Long qualificationId, CaptainQualificationRevokeReq req);
}
