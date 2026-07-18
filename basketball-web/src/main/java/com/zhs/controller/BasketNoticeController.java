package com.zhs.controller;



import com.zhs.service.IBasketNoticeService;
import com.zhs.model.BasketNotice;
import com.zhs.dto.BasketNoticeDto;

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
@RequestMapping("/basket/notice")
@Api(value = "")
@Slf4j
public class BasketNoticeController {

    @Resource
    private IBasketNoticeService  iBasketNoticeService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketNotice>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketNotice> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketNotice> lambda = new QueryWrapper<BasketNotice>().lambda();
        //此处可以拼条件
        lambda.eq(BasketNotice::getIsDelete,0);
        IPage<BasketNotice> pages =  iBasketNoticeService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketNotice> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketNotice> wrapper = new QueryWrapper<BasketNotice>()
                            .lambda().eq(BasketNotice::getId,id).eq(BasketNotice::getIsDelete,0);
        return R.ok(iBasketNoticeService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketNotice> query = new QueryWrapper<BasketNotice>().lambda().eq(BasketNotice::getId, id).eq(BasketNotice::getIsDelete, 0);
        BasketNotice basketNotice = iBasketNoticeService.getOne(query);
        if(ObjectUtils.isEmpty(basketNotice)) throw new ServiceException("该数据不存在或者已经被删除");
        basketNotice.setIsDelete(1);
        iBasketNoticeService.updateById(basketNotice);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketNoticeDto basketNoticeDto){
        if (CollectionUtils.isEmpty(basketNoticeDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketNotice> list = new ArrayList<>();
        basketNoticeDto.getIdList().stream().forEach(id ->{
        BasketNotice basketNotice = iBasketNoticeService.getById(id);
            if (ObjectUtils.isEmpty(basketNotice)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketNotice.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketNotice.setIsDelete(1);
            list.add(basketNotice);
        });
        iBasketNoticeService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketNoticeDto basketNoticeDto){
        return iBasketNoticeService.addOrUpdate(basketNoticeDto);
    }

}
