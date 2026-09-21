package com.bankingsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankingSimulationApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSimulationApplication.class, args);
        System.out.println("Online Banking Simulation System is running!");
    }

}
