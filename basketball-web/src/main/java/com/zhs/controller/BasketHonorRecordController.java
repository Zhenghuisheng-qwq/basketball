package com.zhs.controller;



import com.zhs.service.IBasketHonorRecordService;
import com.zhs.model.BasketHonorRecord;
import com.zhs.dto.BasketHonorRecordDto;

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
@RequestMapping("/basket/honor/record")
@Api(value = "")
@Slf4j
public class BasketHonorRecordController {

    @Resource
    private IBasketHonorRecordService  iBasketHonorRecordService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketHonorRecord>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketHonorRecord> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketHonorRecord> lambda = new QueryWrapper<BasketHonorRecord>().lambda();
        //此处可以拼条件
        lambda.eq(BasketHonorRecord::getIsDelete,0);
        IPage<BasketHonorRecord> pages =  iBasketHonorRecordService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketHonorRecord> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketHonorRecord> wrapper = new QueryWrapper<BasketHonorRecord>()
                            .lambda().eq(BasketHonorRecord::getId,id).eq(BasketHonorRecord::getIsDelete,0);
        return R.ok(iBasketHonorRecordService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketHonorRecord> query = new QueryWrapper<BasketHonorRecord>().lambda().eq(BasketHonorRecord::getId, id).eq(BasketHonorRecord::getIsDelete, 0);
        BasketHonorRecord basketHonorRecord = iBasketHonorRecordService.getOne(query);
        if(ObjectUtils.isEmpty(basketHonorRecord)) throw new ServiceException("该数据不存在或者已经被删除");
        basketHonorRecord.setIsDelete(1);
        iBasketHonorRecordService.updateById(basketHonorRecord);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketHonorRecordDto basketHonorRecordDto){
        if (CollectionUtils.isEmpty(basketHonorRecordDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketHonorRecord> list = new ArrayList<>();
        basketHonorRecordDto.getIdList().stream().forEach(id ->{
        BasketHonorRecord basketHonorRecord = iBasketHonorRecordService.getById(id);
            if (ObjectUtils.isEmpty(basketHonorRecord)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketHonorRecord.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketHonorRecord.setIsDelete(1);
            list.add(basketHonorRecord);
        });
        iBasketHonorRecordService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketHonorRecordDto basketHonorRecordDto){
        return iBasketHonorRecordService.addOrUpdate(basketHonorRecordDto);
    }

}
