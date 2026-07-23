package com.zhs.service.impl;


import com.zhs.model.BasketPlayerCertMaterial;
import com.zhs.dao.BasketPlayerCertMaterialDao;
import com.zhs.service.IBasketPlayerCertMaterialService;
import com.zhs.dto.BasketPlayerCertMaterialDto;


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
public class BasketPlayerCertMaterialServiceImpl extends ServiceImpl< BasketPlayerCertMaterialDao, BasketPlayerCertMaterial> implements IBasketPlayerCertMaterialService {

    @Resource
    private BasketPlayerCertMaterialDao basketPlayerCertMaterialDao;

    @Override
    public R addOrUpdate(BasketPlayerCertMaterialDto basketPlayerCertMaterialDto) {
        if (BeanUtil.isEmpty(basketPlayerCertMaterialDto)) throw new ServiceException("数据不能为空");
        BasketPlayerCertMaterial basketPlayerCertMaterial = new BasketPlayerCertMaterial();
        BeanUtil.copyProperties(basketPlayerCertMaterialDto,basketPlayerCertMaterial);
        if (basketPlayerCertMaterialDto.getId() == null){
            basketPlayerCertMaterialDao.insert(basketPlayerCertMaterial);
            return R.ok("数据插入成功");
        }else {
            basketPlayerCertMaterialDao.updateById(basketPlayerCertMaterial);
            return R.ok("数据更新成功");
        }
    }
}