package com.zhs.controller;



import com.zhs.service.IBasketRenameApplyService;
import com.zhs.model.BasketRenameApply;
import com.zhs.dto.BasketRenameApplyDto;

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
@RequestMapping("/basket/rename/apply")
@Api(value = "")
@Slf4j
public class BasketRenameApplyController {

    @Resource
    private IBasketRenameApplyService  iBasketRenameApplyService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketRenameApply>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketRenameApply> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketRenameApply> lambda = new QueryWrapper<BasketRenameApply>().lambda();
        //此处可以拼条件
        lambda.eq(BasketRenameApply::getIsDelete,0);
        IPage<BasketRenameApply> pages =  iBasketRenameApplyService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketRenameApply> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketRenameApply> wrapper = new QueryWrapper<BasketRenameApply>()
                            .lambda().eq(BasketRenameApply::getId,id).eq(BasketRenameApply::getIsDelete,0);
        return R.ok(iBasketRenameApplyService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketRenameApply> query = new QueryWrapper<BasketRenameApply>().lambda().eq(BasketRenameApply::getId, id).eq(BasketRenameApply::getIsDelete, 0);
        BasketRenameApply basketRenameApply = iBasketRenameApplyService.getOne(query);
        if(ObjectUtils.isEmpty(basketRenameApply)) throw new ServiceException("该数据不存在或者已经被删除");
        basketRenameApply.setIsDelete(1);
        iBasketRenameApplyService.updateById(basketRenameApply);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketRenameApplyDto basketRenameApplyDto){
        if (CollectionUtils.isEmpty(basketRenameApplyDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketRenameApply> list = new ArrayList<>();
        basketRenameApplyDto.getIdList().stream().forEach(id ->{
        BasketRenameApply basketRenameApply = iBasketRenameApplyService.getById(id);
            if (ObjectUtils.isEmpty(basketRenameApply)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketRenameApply.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketRenameApply.setIsDelete(1);
            list.add(basketRenameApply);
        });
        iBasketRenameApplyService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketRenameApplyDto basketRenameApplyDto){
        return iBasketRenameApplyService.addOrUpdate(basketRenameApplyDto);
    }

}
