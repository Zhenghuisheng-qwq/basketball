package com.zhs.controller;



import com.zhs.service.IBasketCaptainRecordService;
import com.zhs.model.BasketCaptainRecord;
import com.zhs.dto.BasketCaptainRecordDto;

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
@RequestMapping("/basket/captain/record")
@Api(value = "")
@Slf4j
public class BasketCaptainRecordController {

    @Resource
    private IBasketCaptainRecordService  iBasketCaptainRecordService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketCaptainRecord>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketCaptainRecord> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketCaptainRecord> lambda = new QueryWrapper<BasketCaptainRecord>().lambda();
        //此处可以拼条件
        lambda.eq(BasketCaptainRecord::getIsDelete,0);
        IPage<BasketCaptainRecord> pages =  iBasketCaptainRecordService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketCaptainRecord> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketCaptainRecord> wrapper = new QueryWrapper<BasketCaptainRecord>()
                            .lambda().eq(BasketCaptainRecord::getId,id).eq(BasketCaptainRecord::getIsDelete,0);
        return R.ok(iBasketCaptainRecordService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketCaptainRecord> query = new QueryWrapper<BasketCaptainRecord>().lambda().eq(BasketCaptainRecord::getId, id).eq(BasketCaptainRecord::getIsDelete, 0);
        BasketCaptainRecord basketCaptainRecord = iBasketCaptainRecordService.getOne(query);
        if(ObjectUtils.isEmpty(basketCaptainRecord)) throw new ServiceException("该数据不存在或者已经被删除");
        basketCaptainRecord.setIsDelete(1);
        iBasketCaptainRecordService.updateById(basketCaptainRecord);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketCaptainRecordDto basketCaptainRecordDto){
        if (CollectionUtils.isEmpty(basketCaptainRecordDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketCaptainRecord> list = new ArrayList<>();
        basketCaptainRecordDto.getIdList().stream().forEach(id ->{
        BasketCaptainRecord basketCaptainRecord = iBasketCaptainRecordService.getById(id);
            if (ObjectUtils.isEmpty(basketCaptainRecord)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketCaptainRecord.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketCaptainRecord.setIsDelete(1);
            list.add(basketCaptainRecord);
        });
        iBasketCaptainRecordService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketCaptainRecordDto basketCaptainRecordDto){
        return iBasketCaptainRecordService.addOrUpdate(basketCaptainRecordDto);
    }

}
