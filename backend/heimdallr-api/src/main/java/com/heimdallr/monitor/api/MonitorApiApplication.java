package com.heimdallr.monitor.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.heimdallr.monitor")
@EnableScheduling
public class MonitorApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitorApiApplication.class, args);
    }
}
