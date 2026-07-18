package com.zhs.controller;



import com.zhs.service.IBasketJoinApplyService;
import com.zhs.model.BasketJoinApply;
import com.zhs.dto.BasketJoinApplyDto;

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
@RequestMapping("/basket/join/apply")
@Api(value = "")
@Slf4j
public class BasketJoinApplyController {

    @Resource
    private IBasketJoinApplyService  iBasketJoinApplyService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketJoinApply>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketJoinApply> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketJoinApply> lambda = new QueryWrapper<BasketJoinApply>().lambda();
        //此处可以拼条件
        lambda.eq(BasketJoinApply::getIsDelete,0);
        IPage<BasketJoinApply> pages =  iBasketJoinApplyService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketJoinApply> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketJoinApply> wrapper = new QueryWrapper<BasketJoinApply>()
                            .lambda().eq(BasketJoinApply::getId,id).eq(BasketJoinApply::getIsDelete,0);
        return R.ok(iBasketJoinApplyService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketJoinApply> query = new QueryWrapper<BasketJoinApply>().lambda().eq(BasketJoinApply::getId, id).eq(BasketJoinApply::getIsDelete, 0);
        BasketJoinApply basketJoinApply = iBasketJoinApplyService.getOne(query);
        if(ObjectUtils.isEmpty(basketJoinApply)) throw new ServiceException("该数据不存在或者已经被删除");
        basketJoinApply.setIsDelete(1);
        iBasketJoinApplyService.updateById(basketJoinApply);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketJoinApplyDto basketJoinApplyDto){
        if (CollectionUtils.isEmpty(basketJoinApplyDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketJoinApply> list = new ArrayList<>();
        basketJoinApplyDto.getIdList().stream().forEach(id ->{
        BasketJoinApply basketJoinApply = iBasketJoinApplyService.getById(id);
            if (ObjectUtils.isEmpty(basketJoinApply)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketJoinApply.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketJoinApply.setIsDelete(1);
            list.add(basketJoinApply);
        });
        iBasketJoinApplyService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketJoinApplyDto basketJoinApplyDto){
        return iBasketJoinApplyService.addOrUpdate(basketJoinApplyDto);
    }

}
