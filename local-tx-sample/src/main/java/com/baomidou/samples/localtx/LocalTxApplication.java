package com.baomidou.samples.localtx;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@MapperScan("com.baomidou.samples.localtx.mapper")
public class LocalTxApplication {

    public static void main(String[] args) {
        SpringApplication.run(LocalTxApplication.class, args);
        log.info("open http://localhost:8080/doc.html");
    }
}