package com.zhs.service.impl;


import com.zhs.model.BasketVerifyCodeRecord;
import com.zhs.dao.BasketVerifyCodeRecordDao;
import com.zhs.service.IBasketVerifyCodeRecordService;
import com.zhs.dto.BasketVerifyCodeRecordDto;


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
public class BasketVerifyCodeRecordServiceImpl extends ServiceImpl< BasketVerifyCodeRecordDao, BasketVerifyCodeRecord> implements IBasketVerifyCodeRecordService {

    @Resource
    private BasketVerifyCodeRecordDao basketVerifyCodeRecordDao;

    @Override
    public R addOrUpdate(BasketVerifyCodeRecordDto basketVerifyCodeRecordDto) {
        if (BeanUtil.isEmpty(basketVerifyCodeRecordDto)) throw new ServiceException("数据不能为空");
        BasketVerifyCodeRecord basketVerifyCodeRecord = new BasketVerifyCodeRecord();
        BeanUtil.copyProperties(basketVerifyCodeRecordDto,basketVerifyCodeRecord);
        if (basketVerifyCodeRecordDto.getId() == null){
            basketVerifyCodeRecordDao.insert(basketVerifyCodeRecord);
            return R.ok("数据插入成功");
        }else {
            basketVerifyCodeRecordDao.updateById(basketVerifyCodeRecord);
            return R.ok("数据更新成功");
        }
    }
}