package com.zhs.basket.service;

import com.zhs.basket.query.MemberProfileChangeQuery;
import com.zhs.basket.req.MemberProfileChangeApproveReq;
import com.zhs.basket.req.MemberProfileChangeRejectReq;
import com.zhs.basket.vo.MemberProfileChangeDetailVo;
import com.zhs.basket.vo.MemberProfileChangeListVo;

import java.util.List;

public interface IBasketMemberProfileChangeService {

    List<MemberProfileChangeListVo> selectList(MemberProfileChangeQuery query);

    MemberProfileChangeDetailVo selectDetail(Long id);

    void approve(Long id, MemberProfileChangeApproveReq req);

    void reject(Long id, MemberProfileChangeRejectReq req);
}
