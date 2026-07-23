package com.zhs.service.impl;


import com.zhs.model.BasketPlayerCertApplication;
import com.zhs.dao.BasketPlayerCertApplicationDao;
import com.zhs.service.IBasketPlayerCertApplicationService;
import com.zhs.dto.BasketPlayerCertApplicationDto;


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
public class BasketPlayerCertApplicationServiceImpl extends ServiceImpl< BasketPlayerCertApplicationDao, BasketPlayerCertApplication> implements IBasketPlayerCertApplicationService {

    @Resource
    private BasketPlayerCertApplicationDao basketPlayerCertApplicationDao;

    @Override
    public R addOrUpdate(BasketPlayerCertApplicationDto basketPlayerCertApplicationDto) {
        if (BeanUtil.isEmpty(basketPlayerCertApplicationDto)) throw new ServiceException("数据不能为空");
        BasketPlayerCertApplication basketPlayerCertApplication = new BasketPlayerCertApplication();
        BeanUtil.copyProperties(basketPlayerCertApplicationDto,basketPlayerCertApplication);
        if (basketPlayerCertApplicationDto.getId() == null){
            basketPlayerCertApplicationDao.insert(basketPlayerCertApplication);
            return R.ok("数据插入成功");
        }else {
            basketPlayerCertApplicationDao.updateById(basketPlayerCertApplication);
            return R.ok("数据更新成功");
        }
    }
}