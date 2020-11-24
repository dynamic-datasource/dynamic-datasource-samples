package com.baomidou.samples.mybatisplugin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.samples.mybatisplugin.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    List<User> selectUsers();

    void addUser(User user);

    void deleteUserById(Long id);
}
