package com.baomidou.samples.shardingsphere.jdbc.v5.spring.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author hl
 * @DateTime 2022/10/14 15:53
 */
@Data
@Accessors(chain = true)
public class OrderPojo {

    private Long orderId;
    private String name;
    private Long userId;

}
