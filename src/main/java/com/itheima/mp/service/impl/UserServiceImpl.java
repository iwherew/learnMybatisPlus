package com.itheima.mp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.itheima.mp.domain.dto.PageDTO;
import com.itheima.mp.domain.po.Address;
import com.itheima.mp.domain.po.User;
import com.itheima.mp.domain.query.UserQuery;
import com.itheima.mp.domain.vo.AddressVO;
import com.itheima.mp.domain.vo.UserVO;
import com.itheima.mp.enums.UserStatus;
import com.itheima.mp.mapper.UserMapper;
import com.itheima.mp.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long id, Integer money) {
        // 1.查询用户
        User user = getById(id);

        // 2.校验用户状态
        if(user == null || user.getStatus() == UserStatus.FROZEN){
            throw new RuntimeException("用户状态异常");
        }

        // 3.校验余额是否充足
        if(user.getBalance() < money){
            throw new RuntimeException("用户余额不足");
        }

        // 4.扣减余额
//        baseMapper.deductBalance(id, money);
        int remainBalance = user.getBalance() - money;
        lambdaUpdate()
                .set(User::getBalance, remainBalance)
                .set(remainBalance == 0, User::getStatus, UserStatus.FROZEN)
                .eq(User::getId, id)
                .eq(User::getBalance, user.getBalance())
                .update();
    }

    @Override
    public List<User> queryUsers(String name, Integer status, Integer minBalance, Integer maxBalance) {
        List<User> users = lambdaQuery()
                .like(name != null, User::getUsername, name)
                .eq(status != null, User::getStatus, status)
                .ge(minBalance != null, User::getBalance, minBalance)
                .le(maxBalance != null, User::getBalance, maxBalance)
                .list();
//                .one()
//                .list()
//                .page
//                .count()
//                .exists()
        return users;
    }

    @Override
    public UserVO queryUserAndAddressById(Long id) {
        // 1.查询用户
        User user = getById(id);
        if(user == null || user.getStatus() == UserStatus.FROZEN){
            throw new RuntimeException("用户状态异常");
        }
        // 2.查询地址
        List<Address> addressList = Db.lambdaQuery(Address.class)
                .eq(Address::getUserId, id)
                .list();

        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        if(CollUtil.isNotEmpty(addressList)){
            userVO.setAddresses(BeanUtil.copyToList(addressList, AddressVO.class));
        }
        return userVO;
    }

    @Override
    public List<UserVO> queryUserAndAddressByIds(List<Long> ids) {
        // 1.查询用户
        List<User> users = listByIds(ids);
        if(CollUtil.isEmpty(users)){
            return Collections.emptyList();
        }
        // 2.查询地址
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        List<Address> addressList = Db.lambdaQuery(Address.class).in(Address::getUserId, userIds).list();
        // 转换地址VO
        List<AddressVO> addressVOList = BeanUtil.copyToList(addressList, AddressVO.class);
        // 用户地址集合分组处理
        Map<Long, List<AddressVO>> addressMap = new HashMap<>();
        if(CollUtil.isNotEmpty(addressList)){
            addressMap = addressVOList.stream().collect(Collectors.groupingBy(AddressVO::getUserId));
        }

        // 3.转换vo返回
        List<UserVO> list = new ArrayList<>(users.size());
        for (User user : users) {
            UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
            vo.setAddresses(addressMap.get(user.getId()));
            list.add(vo);
        }
        return list;
    }

    @Override
    public PageDTO<UserVO> queryUsersPage(UserQuery query) {
        String name = query.getName();
        Integer status = query.getStatus();
        Page<User> page = query.toMpPageDefaultSortByUpdateTime();
        Page<User> p = lambdaQuery()
                .like(name != null, User::getUsername, name)
                .eq(status != null, User::getStatus, status)
                .page(page);
//        return PageDTO.of(p, UserVO.class);
        return PageDTO.of(p,user -> {
            UserVO vo = BeanUtil.copyProperties(user, UserVO.class);
            // 处理特殊逻辑
            vo.setUsername(user.getUsername().substring(0, vo.getUsername().length()-2)+ "**");
            return vo;
        });
    }
}
