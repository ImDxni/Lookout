package me.dani.lookout;

import org.springframework.boot.SpringApplication;

public class TestLookoutApplication {

	public static void main(String[] args) {
		SpringApplication.from(LookoutApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
