package tn.tt.smartops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartOpsApplication.class, args);
    }
}
