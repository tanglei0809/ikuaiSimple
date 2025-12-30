package com.ikuai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ApplicationApp {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationApp.class, args);
        System.out.println("***********启动成功***********");
        System.out.println("****************使用ip:8056/index.html进行访问管理");
        log.info("**********使用ip:8056/index.html进行访问管理");
    }

}