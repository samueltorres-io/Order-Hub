package com.orderhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.orderhub.config.RsaKeyProperties;

@SpringBootApplication
@EnableConfigurationProperties(RsaKeyProperties.class)
public class OrderhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderhubApplication.class, args);
	}

}
