package com.baomidou.samples.ds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class AddRemoveDsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddRemoveDsApplication.class, args);
        log.info("open http://localhost:8080/doc.html");
    }
}