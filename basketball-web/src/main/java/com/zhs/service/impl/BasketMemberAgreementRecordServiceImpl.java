package com.zhs.service.impl;


import com.zhs.model.BasketMemberAgreementRecord;
import com.zhs.dao.BasketMemberAgreementRecordDao;
import com.zhs.service.IBasketMemberAgreementRecordService;
import com.zhs.dto.BasketMemberAgreementRecordDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


/**
 *
 * @author 
 * @since 2026-07-24
 */
@Service
@Slf4j
public class BasketMemberAgreementRecordServiceImpl extends ServiceImpl< BasketMemberAgreementRecordDao, BasketMemberAgreementRecord> implements IBasketMemberAgreementRecordService {

    @Resource
    private BasketMemberAgreementRecordDao basketMemberAgreementRecordDao;

    @Override
    public R addOrUpdate(BasketMemberAgreementRecordDto basketMemberAgreementRecordDto) {
        if (BeanUtil.isEmpty(basketMemberAgreementRecordDto)) throw new ServiceException("数据不能为空");
        BasketMemberAgreementRecord basketMemberAgreementRecord = new BasketMemberAgreementRecord();
        BeanUtil.copyProperties(basketMemberAgreementRecordDto,basketMemberAgreementRecord);
        if (basketMemberAgreementRecordDto.getId() == null){
            basketMemberAgreementRecordDao.insert(basketMemberAgreementRecord);
            return R.ok("数据插入成功");
        }else {
            basketMemberAgreementRecordDao.updateById(basketMemberAgreementRecord);
            return R.ok("数据更新成功");
        }
    }
}