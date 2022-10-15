package com.baomidou.samples.shardingsphere.jdbc.v5.spring.mapper;

import com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity.OrderPojo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Author hl
 * @DateTime 2022/10/14 15:56
 */
@Mapper
public interface OrderPojoMapper {

//    @Select("select * from t_order")
    List<OrderPojo> orderPojoList();

//    @Insert("insert into t_order(order_id,name,user_id) values(#{orderId},#{name},#{useId})")
    int insertOrderPojo(OrderPojo orderPojo);
}
