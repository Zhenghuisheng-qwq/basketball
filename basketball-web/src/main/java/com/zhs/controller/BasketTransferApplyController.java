package com.zhs.controller;



import com.zhs.service.IBasketTransferApplyService;
import com.zhs.model.BasketTransferApply;
import com.zhs.dto.BasketTransferApplyDto;

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
@RequestMapping("/basket/transfer/apply")
@Api(value = "")
@Slf4j
public class BasketTransferApplyController {

    @Resource
    private IBasketTransferApplyService  iBasketTransferApplyService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTransferApply>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTransferApply> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTransferApply> lambda = new QueryWrapper<BasketTransferApply>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTransferApply::getIsDelete,0);
        IPage<BasketTransferApply> pages =  iBasketTransferApplyService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTransferApply> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTransferApply> wrapper = new QueryWrapper<BasketTransferApply>()
                            .lambda().eq(BasketTransferApply::getId,id).eq(BasketTransferApply::getIsDelete,0);
        return R.ok(iBasketTransferApplyService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTransferApply> query = new QueryWrapper<BasketTransferApply>().lambda().eq(BasketTransferApply::getId, id).eq(BasketTransferApply::getIsDelete, 0);
        BasketTransferApply basketTransferApply = iBasketTransferApplyService.getOne(query);
        if(ObjectUtils.isEmpty(basketTransferApply)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTransferApply.setIsDelete(1);
        iBasketTransferApplyService.updateById(basketTransferApply);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTransferApplyDto basketTransferApplyDto){
        if (CollectionUtils.isEmpty(basketTransferApplyDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTransferApply> list = new ArrayList<>();
        basketTransferApplyDto.getIdList().stream().forEach(id ->{
        BasketTransferApply basketTransferApply = iBasketTransferApplyService.getById(id);
            if (ObjectUtils.isEmpty(basketTransferApply)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTransferApply.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTransferApply.setIsDelete(1);
            list.add(basketTransferApply);
        });
        iBasketTransferApplyService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTransferApplyDto basketTransferApplyDto){
        return iBasketTransferApplyService.addOrUpdate(basketTransferApplyDto);
    }

}
