package com.gauri.cosmosdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args)
    {
        System.setProperty("server.servlet.context-path", "/cosmosdemo");
        SpringApplication.run(Main.class, args);
    }
}
