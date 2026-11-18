package com.zhike.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zhike.jwt")
public class JwtProperties {

    /** Base64 编码的 HMAC 密钥，长度需 >= 256 bit */
    private String secret;
    private long ttlHours = 8;
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
