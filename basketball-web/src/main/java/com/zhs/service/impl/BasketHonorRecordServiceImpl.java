package com.zhs.service.impl;


import com.zhs.model.BasketHonorRecord;
import com.zhs.dao.BasketHonorRecordDao;
import com.zhs.service.IBasketHonorRecordService;
import com.zhs.dto.BasketHonorRecordDto;


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
public class BasketHonorRecordServiceImpl extends ServiceImpl< BasketHonorRecordDao, BasketHonorRecord> implements IBasketHonorRecordService {

    @Resource
    private BasketHonorRecordDao basketHonorRecordDao;

    @Override
    public R addOrUpdate(BasketHonorRecordDto basketHonorRecordDto) {
        if (BeanUtil.isEmpty(basketHonorRecordDto)) throw new ServiceException("数据不能为空");
        BasketHonorRecord basketHonorRecord = new BasketHonorRecord();
        BeanUtil.copyProperties(basketHonorRecordDto,basketHonorRecord);
        if (basketHonorRecordDto.getId() == null){
            basketHonorRecordDao.insert(basketHonorRecord);
            return R.ok("数据插入成功");
        }else {
            basketHonorRecordDao.updateById(basketHonorRecord);
            return R.ok("数据更新成功");
        }
    }
}