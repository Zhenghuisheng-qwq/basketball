package com.zhs.controller;



import com.zhs.service.IBasketPlayerRankService;
import com.zhs.model.BasketPlayerRank;
import com.zhs.dto.BasketPlayerRankDto;

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
@RequestMapping("/basket/player/rank")
@Api(value = "")
@Slf4j
public class BasketPlayerRankController {

    @Resource
    private IBasketPlayerRankService  iBasketPlayerRankService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketPlayerRank>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketPlayerRank> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketPlayerRank> lambda = new QueryWrapper<BasketPlayerRank>().lambda();
        //此处可以拼条件
        lambda.eq(BasketPlayerRank::getIsDelete,0);
        IPage<BasketPlayerRank> pages =  iBasketPlayerRankService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketPlayerRank> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketPlayerRank> wrapper = new QueryWrapper<BasketPlayerRank>()
                            .lambda().eq(BasketPlayerRank::getId,id).eq(BasketPlayerRank::getIsDelete,0);
        return R.ok(iBasketPlayerRankService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketPlayerRank> query = new QueryWrapper<BasketPlayerRank>().lambda().eq(BasketPlayerRank::getId, id).eq(BasketPlayerRank::getIsDelete, 0);
        BasketPlayerRank basketPlayerRank = iBasketPlayerRankService.getOne(query);
        if(ObjectUtils.isEmpty(basketPlayerRank)) throw new ServiceException("该数据不存在或者已经被删除");
        basketPlayerRank.setIsDelete(1);
        iBasketPlayerRankService.updateById(basketPlayerRank);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketPlayerRankDto basketPlayerRankDto){
        if (CollectionUtils.isEmpty(basketPlayerRankDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketPlayerRank> list = new ArrayList<>();
        basketPlayerRankDto.getIdList().stream().forEach(id ->{
        BasketPlayerRank basketPlayerRank = iBasketPlayerRankService.getById(id);
            if (ObjectUtils.isEmpty(basketPlayerRank)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketPlayerRank.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketPlayerRank.setIsDelete(1);
            list.add(basketPlayerRank);
        });
        iBasketPlayerRankService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketPlayerRankDto basketPlayerRankDto){
        return iBasketPlayerRankService.addOrUpdate(basketPlayerRankDto);
    }

}
