package com.diego.odontoflowbackend;

import org.springframework.boot.SpringApplication;

public class TestOdontoflowBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(OdontoflowBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
