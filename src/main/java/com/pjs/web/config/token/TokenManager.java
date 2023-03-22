package com.pjs.web.config.token;

import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;

public interface TokenManager {

    String createToken(Authentication authentication, TokenType tokenType);
    Authentication refreshAccessToken(HttpServletRequest request);
    boolean validateToken(String token);

    void destroyTokens(HttpServletRequest request);
    Authentication getAuthentication(String token);

    String getUsername(String accessToken);
    String getNickname(String accessToken);
    String getJoinDate(String accessToken);
}
