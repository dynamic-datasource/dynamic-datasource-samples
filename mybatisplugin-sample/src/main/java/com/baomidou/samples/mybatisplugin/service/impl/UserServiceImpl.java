package com.baomidou.samples.mybatisplugin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.samples.mybatisplugin.entity.User;
import com.baomidou.samples.mybatisplugin.mapper.UserMapper;
import com.baomidou.samples.mybatisplugin.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public List<User> selectUsers() {
        return baseMapper.selectList(null);
    }

    @Override
    public void addUser(User user) {
        baseMapper.insert(user);
    }

    @Override
    public void deleteUserById(Long id) {
        baseMapper.deleteById(id);
    }
}
