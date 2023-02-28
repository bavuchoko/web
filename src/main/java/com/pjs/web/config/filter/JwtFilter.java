package com.pjs.web.config.filter;

import com.pjs.web.config.jwt.TokenManager;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JwtFilter  extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private TokenManager tokenManager;

    public JwtFilter(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }


    /**
     * jwt 토큰의 인증정보를 SecurityContext에 저장하는 역할 수행
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        String jwt = resolveToken(request); // Request에서 토큰을 받음
        String requestURI = request.getRequestURI();
        try {
            if (StringUtils.hasText(jwt) && tokenManager.validateToken(jwt, request)) {
                Authentication authentication = tokenManager.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication); // resolveToke을 통해 토큰을 받아와서 유효성 검증을 하고 정상 토큰이면 SecurityContext에 저장
                logger.debug("Security Context에 '{}' 인증 정보를 저장했습니다, uri: {}", authentication.getName(), requestURI);
            } else {
                logger.debug("유효한 JWT 토큰이 없습니다, uri: {}", requestURI);
            }
        }catch (ExpiredJwtException e){
            request.setAttribute("error", e);
        }
        filterChain.doFilter(request, response); // 다음 Filter를 실행하기 위한 코드. 마지막 필터라면 필터 실행 후 리소스를 반환한다.
    }

    /**
     * Request Header에서 토큰 정보를 꺼내오기 위한 resolveToken 메서드
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


}
