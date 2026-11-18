package com.zhike;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan({"com.zhike.system.mapper", "com.zhike.edu.mapper", "com.zhike.notice.mapper", "com.zhike.log.mapper"})
public class ZhikeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhikeApplication.class, args);
    }
}
