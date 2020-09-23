package com.baomidou.samples.jdbc.service;

import com.baomidou.samples.jdbc.entity.User;

import java.util.List;

public interface UserService {

    List<User> selectMasterUsers();

    List<User> selectSlaveUsers();

    void addUser(User user);

    void deleteUserById(Long id);
}
