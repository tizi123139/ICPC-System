package com.scuec.icpc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private String allowedOrigins = "http://localhost:5173";

    public List<String> originList() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
