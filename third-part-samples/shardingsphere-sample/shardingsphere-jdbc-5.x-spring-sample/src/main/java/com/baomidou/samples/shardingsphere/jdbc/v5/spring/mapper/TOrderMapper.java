package com.baomidou.samples.shardingsphere.jdbc.v5.spring.mapper;

import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.TOrder;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@SuppressWarnings({"SqlDialectInspection", "SqlNoDataSourceInspection"})
public interface TOrderMapper {
    @Select("select * from t_order")
    List<TOrder> findAll();

    @Insert("insert into t_order (order_id,`name`,user_id) values (#{orderId},#{name},#{userId})")
    int addAll(@Param("orderId") Long orderId, @Param("name") String name, @Param("userId") Long userId);

    @Insert("insert into t_order (`name`,user_id) values (#{name},#{userId})")
    int addByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);

    @Delete("delete from t_order where user_id = #{userId}")
    int deleteById(Long userId);

    @Delete("delete from t_order")
    int deleteAll();
}
