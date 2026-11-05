package com.scuec.icpc.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 密钥，长度至少 32 字节。 */
    private String secret;

    /** Token 有效期（秒），默认 2 小时。 */
    private long expirationSeconds = 7200;
}
