package com.zhs.service.impl;


import com.zhs.model.BasketDisciplineRecord;
import com.zhs.dao.BasketDisciplineRecordDao;
import com.zhs.service.IBasketDisciplineRecordService;
import com.zhs.dto.BasketDisciplineRecordDto;


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
public class BasketDisciplineRecordServiceImpl extends ServiceImpl< BasketDisciplineRecordDao, BasketDisciplineRecord> implements IBasketDisciplineRecordService {

    @Resource
    private BasketDisciplineRecordDao basketDisciplineRecordDao;

    @Override
    public R addOrUpdate(BasketDisciplineRecordDto basketDisciplineRecordDto) {
        if (BeanUtil.isEmpty(basketDisciplineRecordDto)) throw new ServiceException("数据不能为空");
        BasketDisciplineRecord basketDisciplineRecord = new BasketDisciplineRecord();
        BeanUtil.copyProperties(basketDisciplineRecordDto,basketDisciplineRecord);
        if (basketDisciplineRecordDto.getId() == null){
            basketDisciplineRecordDao.insert(basketDisciplineRecord);
            return R.ok("数据插入成功");
        }else {
            basketDisciplineRecordDao.updateById(basketDisciplineRecord);
            return R.ok("数据更新成功");
        }
    }
}