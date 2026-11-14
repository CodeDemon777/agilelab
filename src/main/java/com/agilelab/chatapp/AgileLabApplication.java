package com.agilelab.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class AgileLabApplication {

    public static void main(String[] args) {
        try {
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
        } catch (Exception ignored) {
        }
        SpringApplication.run(AgileLabApplication.class, args);
    }
}


