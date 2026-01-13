package com.orderhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "jwt")
public record RsaKeyProperties(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
}