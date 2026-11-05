package com.scuec.icpc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan("com.scuec.icpc.module.**.mapper")
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class IcpcSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(IcpcSystemApplication.class, args);
    }
}
