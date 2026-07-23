package com.zhs.controller;



import com.zhs.service.IBasketAnnouncementService;
import com.zhs.model.BasketAnnouncement;
import com.zhs.dto.BasketAnnouncementDto;

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
@RequestMapping("/basket/announcement")
@Api(value = "")
@Slf4j
public class BasketAnnouncementController {

    @Resource
    private IBasketAnnouncementService  iBasketAnnouncementService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketAnnouncement>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketAnnouncement> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketAnnouncement> lambda = new QueryWrapper<BasketAnnouncement>().lambda();
        //此处可以拼条件
        lambda.eq(BasketAnnouncement::getIsDelete,0);
        IPage<BasketAnnouncement> pages =  iBasketAnnouncementService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketAnnouncement> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketAnnouncement> wrapper = new QueryWrapper<BasketAnnouncement>()
                            .lambda().eq(BasketAnnouncement::getId,id).eq(BasketAnnouncement::getIsDelete,0);
        return R.ok(iBasketAnnouncementService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketAnnouncement> query = new QueryWrapper<BasketAnnouncement>().lambda().eq(BasketAnnouncement::getId, id).eq(BasketAnnouncement::getIsDelete, 0);
        BasketAnnouncement basketAnnouncement = iBasketAnnouncementService.getOne(query);
        if(ObjectUtils.isEmpty(basketAnnouncement)) throw new ServiceException("该数据不存在或者已经被删除");
        basketAnnouncement.setIsDelete(1);
        iBasketAnnouncementService.updateById(basketAnnouncement);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketAnnouncementDto basketAnnouncementDto){
        if (CollectionUtils.isEmpty(basketAnnouncementDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketAnnouncement> list = new ArrayList<>();
        basketAnnouncementDto.getIdList().stream().forEach(id ->{
        BasketAnnouncement basketAnnouncement = iBasketAnnouncementService.getById(id);
            if (ObjectUtils.isEmpty(basketAnnouncement)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketAnnouncement.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketAnnouncement.setIsDelete(1);
            list.add(basketAnnouncement);
        });
        iBasketAnnouncementService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketAnnouncementDto basketAnnouncementDto){
        return iBasketAnnouncementService.addOrUpdate(basketAnnouncementDto);
    }

}
