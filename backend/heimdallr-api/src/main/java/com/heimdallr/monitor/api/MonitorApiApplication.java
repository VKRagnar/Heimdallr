package com.heimdallr.monitor.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.heimdallr.monitor")
public class MonitorApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitorApiApplication.class, args);
    }
}
