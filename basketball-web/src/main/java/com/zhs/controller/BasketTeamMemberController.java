package com.zhs.controller;



import com.zhs.service.IBasketTeamMemberService;
import com.zhs.model.BasketTeamMember;
import com.zhs.dto.BasketTeamMemberDto;

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
@RequestMapping("/basket/team/member")
@Api(value = "")
@Slf4j
public class BasketTeamMemberController {

    @Resource
    private IBasketTeamMemberService  iBasketTeamMemberService;

    @ApiOperation(value = "分页查询")
    @GetMapping("/getByPage")
    public R<IPage<BasketTeamMember>> getListByPage(
        @RequestParam(value = "currentPage", required = false, defaultValue = "1")Integer currentPage,
        @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize){
        //构建分页
        Page<BasketTeamMember> page = new Page<>(currentPage, pageSize);
        LambdaQueryWrapper<BasketTeamMember> lambda = new QueryWrapper<BasketTeamMember>().lambda();
        //此处可以拼条件
        lambda.eq(BasketTeamMember::getIsDelete,0);
        IPage<BasketTeamMember> pages =  iBasketTeamMemberService.page(page, lambda);
        return R.ok(pages);
    }


    @ApiOperation(value = "通过id查询")
    @GetMapping("/getById")
    public R<BasketTeamMember> getById(@RequestParam("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<BasketTeamMember> wrapper = new QueryWrapper<BasketTeamMember>()
                            .lambda().eq(BasketTeamMember::getId,id).eq(BasketTeamMember::getIsDelete,0);
        return R.ok(iBasketTeamMemberService.getOne(wrapper));
    }

    @ApiOperation(value = "通过id删除数据")
    @GetMapping("/delete/{id}")
    @Transactional
    public R deleteById(@PathVariable("id") String id){
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        //逻辑删除
        LambdaQueryWrapper<BasketTeamMember> query = new QueryWrapper<BasketTeamMember>().lambda().eq(BasketTeamMember::getId, id).eq(BasketTeamMember::getIsDelete, 0);
        BasketTeamMember basketTeamMember = iBasketTeamMemberService.getOne(query);
        if(ObjectUtils.isEmpty(basketTeamMember)) throw new ServiceException("该数据不存在或者已经被删除");
        basketTeamMember.setIsDelete(1);
        iBasketTeamMemberService.updateById(basketTeamMember);
        return R.ok("数据删除成功");
    }


    @ApiOperation(value = "批量删除数据")
    @PostMapping("/deleteByIds")
    @Transactional
    public R deleteByIds(@RequestBody BasketTeamMemberDto basketTeamMemberDto){
        if (CollectionUtils.isEmpty(basketTeamMemberDto.getIdList())) throw new ServiceException("要删除的id不能为空!");
        //逻辑删除
        List<BasketTeamMember> list = new ArrayList<>();
        basketTeamMemberDto.getIdList().stream().forEach(id ->{
        BasketTeamMember basketTeamMember = iBasketTeamMemberService.getById(id);
            if (ObjectUtils.isEmpty(basketTeamMember)) throw new ServiceException("id为" + id + "的数据不存在");
            if (1 == basketTeamMember.getIsDelete()) throw new ServiceException("id为" + id + "的数据已经被删除");
            basketTeamMember.setIsDelete(1);
            list.add(basketTeamMember);
        });
        iBasketTeamMemberService.updateBatchById(list);
        return R.ok("数据删除成功");
    }

    @ApiOperation(value = "新增或者更新数据")
    @PostMapping("/addOrUpdate")
    @Transactional
    public R addOrUpdate(@RequestBody BasketTeamMemberDto basketTeamMemberDto){
        return iBasketTeamMemberService.addOrUpdate(basketTeamMemberDto);
    }

}
