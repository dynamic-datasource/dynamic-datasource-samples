package com.baomidou.samples.mybatisplus2;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@MapperScan("com.baomidou.samples.mybatisplus2.mapper")
public class MybatisPlus2Application {

    public static void main(String[] args) {
        SpringApplication.run(MybatisPlus2Application.class, args);
        log.info("open http://localhost:8080/doc.html");
    }
}