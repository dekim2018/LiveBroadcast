package com.livebroadcast.broadcast.app.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class PublishTokenGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
