package com.zhs.service.impl;


import com.zhs.model.BasketCaptainRecord;
import com.zhs.dao.BasketCaptainRecordDao;
import com.zhs.service.IBasketCaptainRecordService;
import com.zhs.dto.BasketCaptainRecordDto;


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
 * @since 2026-07-18
 */
@Service
@Slf4j
public class BasketCaptainRecordServiceImpl extends ServiceImpl< BasketCaptainRecordDao, BasketCaptainRecord> implements IBasketCaptainRecordService {

    @Resource
    private BasketCaptainRecordDao basketCaptainRecordDao;

    @Override
    public R addOrUpdate(BasketCaptainRecordDto basketCaptainRecordDto) {
        if (BeanUtil.isEmpty(basketCaptainRecordDto)) throw new ServiceException("数据不能为空");
        BasketCaptainRecord basketCaptainRecord = new BasketCaptainRecord();
        BeanUtil.copyProperties(basketCaptainRecordDto,basketCaptainRecord);
        if (basketCaptainRecordDto.getId() == null){
            basketCaptainRecordDao.insert(basketCaptainRecord);
            return R.ok("数据插入成功");
        }else {
            basketCaptainRecordDao.updateById(basketCaptainRecord);
            return R.ok("数据更新成功");
        }
    }
}