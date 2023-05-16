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
        String joinDate = ((AccountAdapter)(authentication.getPrincipal())).getAccount().getJoinDate().toString();
        long now = (new Date()).getTime();
        Date validity =  tokenType == TokenType.ACCESS_TOKEN ?  new Date(now + this.accessTokenValidityTime) : new Date(now + this.refreshTokenValidityTime);
        Map<String, Object> payloads = new HashMap<>();
        payloads.put("username", authentication.getName());
        payloads.put(AUTHORITIES_KEY, authorities);
        payloads.put("joinDate", joinDate);
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
    public Authentication getAuthenticationFromRefreshToken(HttpServletRequest request) {
        //쿠키에서 갱신토큰 꺼냄
        String refreshTokenInCookie = cookieUtil.getCookie(request, TokenType.REFRESH_TOKEN.getValue()).getValue();
        //클라이언트 ip
        if (validateToken(refreshTokenInCookie)) {
            String clientIP = WebCommon.getClientIp(request);
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
        //문자열의 토큰으로부터 Claim 객체를 생성.
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        //Claim 으로부터 권한을 Collection으로 가져온다.
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
        //Claim 으로부터 username을 꺼낸다.
        String username = claims.get("username", String.class);
        //Username으로 DB 에서 User를 조회하고 그 user로 인증객체 생성을 위해 넣어줄 UserDetail 객체를 생성한다.
        UserDetails userDetails=  new AccountAdapter(accountJapRepository.findByUsername(username)
                .orElseThrow(()->new UsernameNotFoundException(username)));

        //이 UsernamePasswordAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) 생성자는 ..
        //     ( AccountServiceImpl 의 authorize 에서 사용한 username과 password를 받고  isAuthenticated = false 를 반환하는  UsernamePasswordAuthenticationToken 생성자와 달리 )
        // 신뢰할 수 있는 인증토큰생성에서만 사용해야 한다. 그런데 이미 refresh 토큰 검증을 끝냈는데 굳이 DB 에서 다시 유저를 조회해 와야하나? JPA 영속성엔티티에 남아있어서 DB에 직접 조회하지는 않는가? 의문이 있음.
        //          두 생성자의 차이는 인증 전의 인증객체인지 인증 완료 후의 객체인지에 있다.(super.setAuthenticated(false / true )  차이)
        //          인증 전의 객체는 AccountServiceImpl 에서 UsernamePasswordAuthenticationToken 이후 authenticationManagerBuilder 의 authenticate 에서 자격증명을 시도하고 성공하면 완전히 채워진 인증객체를 반환한다.
        return new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
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
    public String getJoinDate(String token) {
        Claims claims = Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("joinDate").toString();
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
