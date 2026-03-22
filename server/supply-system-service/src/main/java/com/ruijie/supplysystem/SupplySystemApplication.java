package com.ruijie.supplysystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ruijie.supplysystem.mapper")
public class SupplySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplySystemApplication.class, args);
    }
}
