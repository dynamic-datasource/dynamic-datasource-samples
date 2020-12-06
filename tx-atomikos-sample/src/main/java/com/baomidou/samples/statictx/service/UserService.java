package com.baomidou.samples.statictx.service;


import com.baomidou.samples.statictx.entity.User;

import java.util.List;

public interface UserService {

    List<User> selectUsersFromMaster();

    List<User> selectUsersFromSlave();

    void addUser(User user);

    void deleteUserById(Long id);
}
