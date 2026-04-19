package com.resumeai.resume_section_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ResumeSectionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeSectionServiceApplication.class, args);
	}

}
