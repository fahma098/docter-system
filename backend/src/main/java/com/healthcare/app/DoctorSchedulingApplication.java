package com.healthcare.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main Spring Boot Application Entry Point
 * Doctor Availability & Healthcare Scheduling Platform
 */
@SpringBootApplication
@EnableTransactionManagement
public class DoctorSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DoctorSchedulingApplication.class, args);
    }
}
