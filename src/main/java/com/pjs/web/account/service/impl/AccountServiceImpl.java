package com.pjs.web.account.service.impl;


import com.pjs.web.account.adapter.AccountAdapter;
import com.pjs.web.account.dto.AccountDto;
import com.pjs.web.account.entity.Account;
import com.pjs.web.account.repository.AccountJapRepository;
import com.pjs.web.account.service.AccountService;
import com.pjs.web.config.redis.RedisUtil;
import com.pjs.web.config.jwt.CookieUtil;
import com.pjs.web.config.jwt.TokenManager;
import com.pjs.web.config.jwt.TokenType;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements UserDetailsService, AccountService {


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
            throw new IllegalStateException("이미 가입된 사용자입니다");
        });
        account.setPassword(this.passwordEncoder.encode(account.getPassword()));
        return this.accountJapRepository.save(account);
    }



    @Override
    public AccountDto authorize(String username, String pass, HttpServletResponse response) throws BadCredentialsException {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, pass);

        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);


        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = tokenManager.createToken(authentication, TokenType.ACCESS_TOKEN);

        AccountDto account = AccountDto.builder()
                .username(authentication.getName())
                .token(accessToken)
                .nickname(tokenManager.getNickname(accessToken)==null ? "익명" : tokenManager.getNickname(accessToken))
                .build();
        String refreshToken = tokenManager.createToken(authentication, TokenType.REFRESH_TOKEN);
        redisUtil.setData(refreshToken, authentication.getName());

        Cookie refreshTokenCookie = cookieUtil.createCookie("refreshToken", refreshToken);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setHttpOnly(true);
        // expires in 7 days
        refreshTokenCookie.setMaxAge((int)(accessTokenValidityTime/1000));
        response.addCookie(refreshTokenCookie);

        return account;
    }

    @Override
    public AccountDto refreshToken(HttpServletRequest request) {
        //쿠키에서 refresh토큰을 꺼내 검증함
        AccountDto account = null;
        if(tokenManager.validateRefreshToken(request)){
            String accessToken = tokenManager.refreshAccessToken(request);
            account =AccountDto.builder()
                    .token(accessToken)
                    .username(tokenManager.getUsername(accessToken))
                    .nickname(tokenManager.getNickname(accessToken) ==null ? "익명": tokenManager.getNickname(accessToken))
                    .build();
        }
        return account;
    }

    @Override
    public void logout(HttpServletRequest req) {
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
