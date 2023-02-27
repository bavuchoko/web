package com.pjs.web.config.jwt;

import com.pjs.web.account.adapter.AccountAdapter;
import com.pjs.web.account.entity.Account;
import com.pjs.web.account.repository.AccountJapRepository;
import com.pjs.web.config.redis.RedisUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TokenManagerImpl implements TokenManager, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(TokenManagerImpl.class);

    private static final String AUTHORITIES_KEY = "auth";

    private final String secret;
    private final long accessTokenValidityTime;
    private final long refreshTokenValidityTime;

    private Key key;

    @Autowired
    CookieUtil cookieUtil;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    AccountJapRepository accountJapRepository;

    public TokenManagerImpl(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.token-validity-in-seconds}") long tokenValidityInSeconds) {
        this.secret = secret;
//        24시간
        this.accessTokenValidityTime = tokenValidityInSeconds;
//        1주일
        this.refreshTokenValidityTime = tokenValidityInSeconds * 7 ;
    }

    @Override
    public void afterPropertiesSet() {
        System.out.printf("secret");
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    @Override
    public String createToken(Authentication authentication, TokenType tokenType) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        String nickname = ((AccountAdapter)(authentication.getPrincipal())).getAccount().getNickname();
        long now = (new Date()).getTime();
        Date validity =  tokenType == TokenType.ACCESS_TOKEN ?  new Date(now + this.accessTokenValidityTime) : new Date(now + this.refreshTokenValidityTime);
        Map<String, Object> payloads = new HashMap<>();
        payloads.put("username", authentication.getName());
        payloads.put(AUTHORITIES_KEY, authorities);
        payloads.put("nickname", nickname ==null ? "익명" : nickname);

        return Jwts.builder()
                .setSubject(authentication.getName())
//                .claim(AUTHORITIES_KEY, authorities)
                .setClaims(payloads)
                .signWith(key, SignatureAlgorithm.HS512)
                .setIssuedAt(new Date())
                .setExpiration(validity)
                .compact();
    }

    @Override
    public String refreshAccessToken(HttpServletRequest request) {
        String refreshTokenInCookie = cookieUtil.getCookie(request, TokenType.REFRESH_TOKEN.getValue()).getValue();
        if (validateRefreshToken(request)) {


            String username = redisUtil.getData(refreshTokenInCookie);
            Account account = accountJapRepository.findByUsername(username)
                    .orElseThrow(()->new UsernameNotFoundException(username));

            String authorities = new AccountAdapter(account).getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            Map<String, Object> payloads = new HashMap<>();
            payloads.put("username", username);
            payloads.put(AUTHORITIES_KEY, authorities);
            payloads.put("nickname", account.getNickname() == null ? "익명" : account.getNickname());

            long now = (new Date()).getTime();
            return Jwts.builder()
                    .setSubject(account.getUsername())
//                    .claim(AUTHORITIES_KEY, authorities)
                    .setClaims(payloads)
                    .signWith(key, SignatureAlgorithm.HS512)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(now + this.accessTokenValidityTime))
                    .compact();
        }
        return null;
    }


    @Override
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        String username = claims.get("username", String.class);
        UserDetails aa=  new AccountAdapter(accountJapRepository.findByUsername(username)
                .orElseThrow(()->new UsernameNotFoundException(username)));

        return new UsernamePasswordAuthenticationToken(aa, token, authorities);
    }

    public String getUsername(String token) {
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("username").toString();
    }

    public String getNickname(String token) {
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("nickname").toString();
    }


    @Override
    public boolean validateToken(String token, HttpServletRequest request) {

        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            logger.info("잘못된 JWT 서명입니다.");
            throw e;
        } catch (ExpiredJwtException e) {
            logger.info("만료된 JWT 토큰입니다.");
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.info("지원되지 않는 JWT 토큰입니다.");
            throw e;
        } catch (IllegalArgumentException e) {
            logger.info("JWT 토큰이 잘못되었습니다.");
            throw e;
        }

    }

    @Override
    public boolean validateRefreshToken(HttpServletRequest request) {
        //쿠키에서 refreshToken을 꺼냄
        String refreshTokenInCookie = cookieUtil.getCookie(request, TokenType.REFRESH_TOKEN.getValue()).getValue();
        //토큰을 파싱함
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(refreshTokenInCookie)
                .getBody();

        //갱신토큰에 이상이 없으면 통과
        if (validateToken(refreshTokenInCookie, request)){
            return true;
        }
        return false;
    }

    @Override
    public void destroyTokens(HttpServletRequest request) {

    }

}
