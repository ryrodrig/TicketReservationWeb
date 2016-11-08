package com.walmart.ticketing.conf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ComponentScan("com.walmart.ticketing")
@ImportResource({"classpath*:applicationContext.xml"})
@PropertySource("classpath:application.properties")
public class Bootstrapper {
	
	public static void main(String[] args) {
		SpringApplication.run(Bootstrapper.class, args);
	}

}
