package com.baomidou.samples.statictx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.baomidou.samples.mybatis.mapper")
public class StaticTxApplication {

    public static void main(String[] args) {
        SpringApplication.run(StaticTxApplication.class, args);
    }
}