package com.livebroadcast.broadcast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.livebroadcast.broadcast", "com.livebroadcast.common"})
@ConfigurationPropertiesScan("com.livebroadcast.broadcast")
@EnableScheduling
public class BroadcastServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BroadcastServiceApplication.class, args);
    }
}
