package com.zhs.service.impl;


import com.zhs.model.BasketStatChangeRecord;
import com.zhs.dao.BasketStatChangeRecordDao;
import com.zhs.service.IBasketStatChangeRecordService;
import com.zhs.dto.BasketStatChangeRecordDto;


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
public class BasketStatChangeRecordServiceImpl extends ServiceImpl< BasketStatChangeRecordDao, BasketStatChangeRecord> implements IBasketStatChangeRecordService {

    @Resource
    private BasketStatChangeRecordDao basketStatChangeRecordDao;

    @Override
    public R addOrUpdate(BasketStatChangeRecordDto basketStatChangeRecordDto) {
        if (BeanUtil.isEmpty(basketStatChangeRecordDto)) throw new ServiceException("数据不能为空");
        BasketStatChangeRecord basketStatChangeRecord = new BasketStatChangeRecord();
        BeanUtil.copyProperties(basketStatChangeRecordDto,basketStatChangeRecord);
        if (basketStatChangeRecordDto.getId() == null){
            basketStatChangeRecordDao.insert(basketStatChangeRecord);
            return R.ok("数据插入成功");
        }else {
            basketStatChangeRecordDao.updateById(basketStatChangeRecord);
            return R.ok("数据更新成功");
        }
    }
}