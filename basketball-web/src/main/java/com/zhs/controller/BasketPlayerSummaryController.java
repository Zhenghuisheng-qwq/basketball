package com.zhs.controller;



import com.zhs.service.IBasketPlayerSummaryService;
import com.zhs.model.BasketPlayerSummary;
import com.zhs.dto.BasketPlayerSummaryDto;

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
 * @since 2026-07-18
 */
@RestController
@RequestMapping("/basket/player/summary")
@Api(value = "")
@Slf4j
public class BasketPlayerSummaryController {

    @Resource
    private IBasketPlayerSummaryService  iBasketPlayerSummaryService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayerSummary>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayerSummary> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerSummary> lambda = new QueryWrapper<BasketPlayerSummary>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayerSummary::getIsDelete,0);
        IPage<BasketPlayerSummary> pages =  iBasketPlayerSummaryService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayerSummary> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayerSummary> wrapper = new QueryWrapper<BasketPlayerSummary>()
                            .lambda().eq(BasketPlayerSummary::getId,id).eq(BasketPlayerSummary::getIsDelete,0);
        return R.ok(iBasketPlayerSummaryService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayerSummary> query = new QueryWrapper<BasketPlayerSummary>().lambda().eq(BasketPlayerSummary::getId, id).eq(BasketPlayerSummary::getIsDelete, 0);
        BasketPlayerSummary basketPlayerSummary = iBasketPlayerSummaryService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayerSummary)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayerSummary.setIsDelete(1);
        iBasketPlayerSummaryService.updateById(basketPlayerSummary);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerSummaryDto basketPlayerSummaryDto){
        if (CollectionUtils.isEmpty(basketPlayerSummaryDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayerSummary> list = new ArrayList<>();
        basketPlayerSummaryDto.getIdList().stream().forEach(id ->{
        BasketPlayerSummary basketPlayerSummary = iBasketPlayerSummaryService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayerSummary)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayerSummary.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayerSummary.setIsDelete(1);
            list.add(basketPlayerSummary);
        });
        iBasketPlayerSummaryService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerSummaryDto basketPlayerSummaryDto){
        return iBasketPlayerSummaryService.addOrUpdate(basketPlayerSummaryDto);
    }

}
