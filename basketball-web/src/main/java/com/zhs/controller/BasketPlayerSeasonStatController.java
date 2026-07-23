package com.zhs.controller;



import com.zhs.service.IBasketPlayerSeasonStatService;
import com.zhs.model.BasketPlayerSeasonStat;
import com.zhs.dto.BasketPlayerSeasonStatDto;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.zhs.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author 
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/basket/player/season/stat")
@Api(value = "")
@Slf4j
public class BasketPlayerSeasonStatController {

    @Resource
    private IBasketPlayerSeasonStatService  iBasketPlayerSeasonStatService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayerSeasonStat>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayerSeasonStat> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerSeasonStat> lambda = new QueryWrapper<BasketPlayerSeasonStat>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayerSeasonStat::getIsDelete,0);
        IPage<BasketPlayerSeasonStat> pages =  iBasketPlayerSeasonStatService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayerSeasonStat> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayerSeasonStat> wrapper = new QueryWrapper<BasketPlayerSeasonStat>()
                            .lambda().eq(BasketPlayerSeasonStat::getId,id).eq(BasketPlayerSeasonStat::getIsDelete,0);
        return R.ok(iBasketPlayerSeasonStatService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayerSeasonStat> query = new QueryWrapper<BasketPlayerSeasonStat>().lambda().eq(BasketPlayerSeasonStat::getId, id).eq(BasketPlayerSeasonStat::getIsDelete, 0);
        BasketPlayerSeasonStat basketPlayerSeasonStat = iBasketPlayerSeasonStatService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayerSeasonStat)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayerSeasonStat.setIsDelete(1);
        iBasketPlayerSeasonStatService.updateById(basketPlayerSeasonStat);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerSeasonStatDto basketPlayerSeasonStatDto){
        if (CollectionUtils.isEmpty(basketPlayerSeasonStatDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayerSeasonStat> list = new ArrayList<>();
        basketPlayerSeasonStatDto.getIdList().stream().forEach(id ->{
        BasketPlayerSeasonStat basketPlayerSeasonStat = iBasketPlayerSeasonStatService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayerSeasonStat)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayerSeasonStat.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayerSeasonStat.setIsDelete(1);
            list.add(basketPlayerSeasonStat);
        });
        iBasketPlayerSeasonStatService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerSeasonStatDto basketPlayerSeasonStatDto){
        return iBasketPlayerSeasonStatService.addOrUpdate(basketPlayerSeasonStatDto);
    }

}
