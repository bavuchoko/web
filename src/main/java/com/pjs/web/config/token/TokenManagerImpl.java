package com.pjs.web.config.token;

import com.pjs.web.account.dto.AccountAdapter;
import com.pjs.web.account.entity.Account;
import com.pjs.web.account.repository.AccountJapRepository;
import com.pjs.web.common.WebCommon;
import com.pjs.web.config.utils.CookieUtil;
import com.pjs.web.config.utils.RedisUtil;
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
import org.springframework.util.StringUtils;

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
        payloads.put("nickname", nickname == null ? "익명" : nickname);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .setClaims(payloads)
                .signWith(key, SignatureAlgorithm.HS512)
                .setIssuedAt(new Date())
                .setExpiration(validity)
                .compact();
    }

    @Override
    public Authentication refreshAccessToken(HttpServletRequest request) {
        //쿠키에서 갱신토큰 꺼냄
        String refreshTokenInCookie = cookieUtil.getCookie(request, TokenType.REFRESH_TOKEN.getValue()).getValue();
        //클라이언트 ip
        String clientIP = WebCommon.getClientIp(request);
        if (validateToken(refreshTokenInCookie)) {
            String storedIP = redisUtil.getData(refreshTokenInCookie);
            //갱신토큰을 Key로 redis에서 조회한 Ip 와 갱신 요청한 클라이언트 Ip 가 같으면 인증객체를 새로 생성
            if(clientIP.equals(storedIP)){
                return getAuthentication(refreshTokenInCookie);
            }
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
    public boolean validateToken(String token) {

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
    public void destroyTokens(HttpServletRequest request) {

    }

}
