package com.interview.interview_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FiverrShortlinksApplication {

	public static void main(String[] args) {
		SpringApplication.run(FiverrShortlinksApplication.class, args);
	}

}
