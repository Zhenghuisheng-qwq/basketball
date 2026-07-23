package com.zhs.service.impl;


import com.zhs.model.BasketFileAsset;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.service.IBasketFileAssetService;
import com.zhs.dto.BasketFileAssetDto;


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
public class BasketFileAssetServiceImpl extends ServiceImpl< BasketFileAssetDao, BasketFileAsset> implements IBasketFileAssetService {

    @Resource
    private BasketFileAssetDao basketFileAssetDao;

    @Override
    public R addOrUpdate(BasketFileAssetDto basketFileAssetDto) {
        if (BeanUtil.isEmpty(basketFileAssetDto)) throw new ServiceException("数据不能为空");
        BasketFileAsset basketFileAsset = new BasketFileAsset();
        BeanUtil.copyProperties(basketFileAssetDto,basketFileAsset);
        if (basketFileAssetDto.getId() == null){
            basketFileAssetDao.insert(basketFileAsset);
            return R.ok("数据插入成功");
        }else {
            basketFileAssetDao.updateById(basketFileAsset);
            return R.ok("数据更新成功");
        }
    }
}