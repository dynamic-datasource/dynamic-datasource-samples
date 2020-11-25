package com.baomidou.samples.multitenant;


import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = DruidDataSourceAutoConfigure.class)
public class ImwardCloudApplication {

  public static void main(String[] args) {
    SpringApplication.run(ImwardCloudApplication.class, args);
  }
}