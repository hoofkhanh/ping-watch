package com.hokhanh.ping_watch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class PingWatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(PingWatchApplication.class, args);
	}

}
