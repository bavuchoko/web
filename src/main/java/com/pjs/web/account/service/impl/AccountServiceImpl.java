package com.pjs.web.account.service.impl;


import com.pjs.web.account.dto.AccountAdapter;
import com.pjs.web.account.dto.AccountDto;
import com.pjs.web.account.entity.Account;
import com.pjs.web.account.repository.AccountJapRepository;
import com.pjs.web.account.service.AccountService;
import com.pjs.web.common.WebCommon;
import com.pjs.web.config.utils.RedisUtil;
import com.pjs.web.config.utils.CookieUtil;
import com.pjs.web.config.token.TokenManager;
import com.pjs.web.config.token.TokenType;
import jdk.jfr.Frequency;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {


    @Autowired
    AccountJapRepository accountJapRepository;
    @Autowired
    CookieUtil cookieUtil;

    @Value("${spring.jwt.token-validity-in-seconds}")
    private long accessTokenValidityTime;

    private final TokenManager tokenManager;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final RedisUtil redisUtil;

    private final PasswordEncoder passwordEncoder;



    @Override
    public Account saveAccount(Account account) {

        accountJapRepository.findByUsername(account.getUsername()).ifPresent(e->{
            throw new IllegalArgumentException("Duplicated username");
        });
        account.setPassword(this.passwordEncoder.encode(account.getPassword()));
        return this.accountJapRepository.save(account);
    }



    @Override
    public String authorize(AccountDto account, HttpServletResponse response, HttpServletRequest request) throws BadCredentialsException {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(account.getUsername(), account.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = tokenManager.createToken(authentication, TokenType.ACCESS_TOKEN);
        String refreshToken = tokenManager.createToken(authentication, TokenType.REFRESH_TOKEN);
        redisUtil.setData(refreshToken, WebCommon.getClientIp(request));

        Cookie refreshTokenCookie = cookieUtil.createCookie(TokenType.REFRESH_TOKEN.getValue(), refreshToken);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setHttpOnly(true);
        // expires in 7 days
        refreshTokenCookie.setMaxAge((int)(accessTokenValidityTime/1000));
        response.addCookie(refreshTokenCookie);

        return accessToken;
    }

    @Override
    public String reIssueToken(HttpServletRequest request) {
        //쿠키에서 refreshToken을 꺼냄
        String refreshTokenInCookie = cookieUtil.getCookie(request, TokenType.REFRESH_TOKEN.getValue()).getValue();
        String accessToken = null;
        if(StringUtils.hasText(refreshTokenInCookie) && tokenManager.validateToken(refreshTokenInCookie)){
            //refresh토큰을 검증함
            Authentication authentication = tokenManager.refreshAccessToken(request);
            //검증을 통과하면 리턴하는 인증객체로 새로운 엑세스 토큰 발급
            accessToken = tokenManager.createToken(authentication, TokenType.ACCESS_TOKEN);

            /**
             * todo
             * 갱신토큰의 갱신에 관한 로직 필요
             * 매번 엑세스 토큰이 갱신될때마다 갱신토큰을 갱신할 것인지, 갱신토큰의 유효시간이 얼마 이하 일때만 갱신할 것인지. 갱신하지 않고 갱신토큰 만료시 새로 로그인을 요구할지.
             */
        }else{
            throw new IllegalArgumentException("No valid refreshToken");
        }
        return accessToken;
    }

    @Override
    public void logout(HttpServletRequest req) {

        /**
         * todo
         * 이미 발급된 엑세스 토큰은 어떻게 처리할 것인가.
         */
        if(null != cookieUtil.getCookie(req, TokenType.REFRESH_TOKEN.getValue())){
        String refreshTokenInCookie = cookieUtil.getCookie(req, TokenType.REFRESH_TOKEN.getValue()).getValue();
        redisUtil.deleteData(refreshTokenInCookie);
        }
    }

    @Override
    public Page<Account> loadUserList(Pageable pagable){
        return this.accountJapRepository.findAll(pagable);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountJapRepository.findByUsername(username)
                .orElseThrow(()->new UsernameNotFoundException(username));
        return new AccountAdapter(account);
    }
}
