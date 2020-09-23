package com.baomidou.samples.spel.mapper;

import com.baomidou.samples.spel.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {

    @Select("SELECT * FROM user")
    List<User> selectUsers();
}
