package com.example.handson;

import org.springframework.boot.SpringApplication;

public class TestHandsonApplication {

	public static void main(String[] args) {
		SpringApplication.from(HandsonApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
