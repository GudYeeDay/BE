package org.example.gudyeeday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@SpringBootApplication
public class GudYeeDayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GudYeeDayApplication.class, args);
    }

}
