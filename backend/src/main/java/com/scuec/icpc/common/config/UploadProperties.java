package com.scuec.icpc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String directory = "./uploads";
    private String urlPrefix = "/uploads";
    private long maxSize = 10 * 1024 * 1024L;
}
