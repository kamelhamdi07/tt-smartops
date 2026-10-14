package tn.tt.smartops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Planning planning, Bootstrap bootstrap, Demo demo) {

    public record Jwt(String secret, long expirationMinutes) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Planning(String workStart, String workEnd) {
        public LocalTime start() { return LocalTime.parse(workStart); }
        public LocalTime end() { return LocalTime.parse(workEnd); }
    }

    public record Bootstrap(String adminEmail, String adminPassword) {}

    public record Demo(boolean enabled, String password) {}
}
