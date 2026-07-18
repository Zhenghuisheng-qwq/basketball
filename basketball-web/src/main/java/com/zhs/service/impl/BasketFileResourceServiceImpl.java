package com.zhs.service.impl;


import com.zhs.model.BasketFileResource;
import com.zhs.dao.BasketFileResourceDao;
import com.zhs.service.IBasketFileResourceService;
import com.zhs.dto.BasketFileResourceDto;


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
public class BasketFileResourceServiceImpl extends ServiceImpl< BasketFileResourceDao, BasketFileResource> implements IBasketFileResourceService {

    @Resource
    private BasketFileResourceDao basketFileResourceDao;

    @Override
    public R addOrUpdate(BasketFileResourceDto basketFileResourceDto) {
        if (BeanUtil.isEmpty(basketFileResourceDto)) throw new ServiceException("数据不能为空");
        BasketFileResource basketFileResource = new BasketFileResource();
        BeanUtil.copyProperties(basketFileResourceDto,basketFileResource);
        if (basketFileResourceDto.getId() == null){
            basketFileResourceDao.insert(basketFileResource);
            return R.ok("数据插入成功");
        }else {
            basketFileResourceDao.updateById(basketFileResource);
            return R.ok("数据更新成功");
        }
    }
}