package me.dani.lookout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LookoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(LookoutApplication.class, args);
	}

}
