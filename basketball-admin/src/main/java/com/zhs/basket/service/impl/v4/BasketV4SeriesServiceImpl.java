package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.basket.service.v4.IBasketV4SeriesService;
import com.zhs.basket.vo.v4.BasketSeriesVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketV4SeriesServiceImpl implements IBasketV4SeriesService {

    @Autowired
    private BasketSeriesMapper seriesMapper;

    @Override
    public List<BasketSeriesVo> selectList(Integer enabled) {
        return seriesMapper.selectList(enabled).stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public List<BasketSeriesVo> selectHonorIconList() {
        return seriesMapper.selectHonorIconList().stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public BasketSeriesVo selectById(Long id) {
        return toVo(requireSeries(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(Long id, Integer enabled) {
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new ServiceException("enabled 只能为 0 或 1");
        }
        requireSeries(id);
        if (seriesMapper.updateEnabled(id, enabled) != 1) {
            throw new ServiceException("更新 Series 启停状态失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHonorIconUrl(Long id, String honorIconUrl) {
        requireSeries(id);
        String url = honorIconUrl == null ? "" : honorIconUrl.trim();
        if (!url.isEmpty() && !url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("/")) {
            throw new ServiceException("荣誉头像地址不合法");
        }
        if (url.length() > 512) {
            throw new ServiceException("荣誉头像地址过长");
        }
        if (seriesMapper.updateHonorIconUrl(id, url.isEmpty() ? null : url) != 1) {
            throw new ServiceException("更新荣誉头像失败");
        }
    }

    private BasketSeries requireSeries(Long id) {
        if (id == null) {
            throw new ServiceException("Series ID 不能为空");
        }
        BasketSeries series = seriesMapper.selectById(id);
        if (series == null) {
            throw new ServiceException("Series 不存在");
        }
        return series;
    }

    private BasketSeriesVo toVo(BasketSeries entity) {
        BasketSeriesVo vo = new BasketSeriesVo();
        BeanUtils.copyBeanProp(vo, entity);
        return vo;
    }
}
