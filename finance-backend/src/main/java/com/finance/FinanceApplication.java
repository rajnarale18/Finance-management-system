package com.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FinanceApplication - Entry point of the Spring Boot application.
 *
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * This annotation tells Spring Boot to auto-configure everything and scan all components.
 */
@SpringBootApplication
public class FinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
        System.out.println("✅ Finance Backend Started Successfully on http://localhost:8080");
    }
}
