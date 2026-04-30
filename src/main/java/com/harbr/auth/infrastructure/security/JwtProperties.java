package com.harbr.auth.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "harbr.jwt")
public class JwtProperties {

    private String secretKey;
    private long accessTokenExpiration = 900000;
    private long refreshTokenExpiration = 604800000;
}