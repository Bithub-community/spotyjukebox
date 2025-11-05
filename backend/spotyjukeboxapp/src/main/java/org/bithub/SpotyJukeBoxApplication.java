package org.bithub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpotyJukeBoxApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpotyJukeBoxApplication.class, args);
    }
}