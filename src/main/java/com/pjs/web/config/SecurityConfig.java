package com.pjs.web.config;

import com.pjs.web.account.service.impl.AccountServiceImpl;
import com.pjs.web.config.filter.TokenAccessDeniedHandler;
import com.pjs.web.config.filter.TokenAuthenticationEntryPoint;
import com.pjs.web.config.token.TokenSecurityConfig;
import com.pjs.web.config.token.TokenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final TokenManager tokenManager;
    private final TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint;
    private final TokenAccessDeniedHandler tokenAccessDeniedHandler;


    public SecurityConfig(TokenManager tokenManager, TokenAuthenticationEntryPoint tokenAuthenticationEntryPoint, TokenAccessDeniedHandler tokenAccessDeniedHandler) {
        this.tokenManager = tokenManager;
        this.tokenAuthenticationEntryPoint = tokenAuthenticationEntryPoint;
        this.tokenAccessDeniedHandler = tokenAccessDeniedHandler;
    }



    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AccountServiceImpl accountServiceImpl;



    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }



    //인증서버 설정시 시큐리시 설정에 SecurityFilterChain 이 있으면 둘중 하나 선택하라고 에러남
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .httpBasic().disable()
                .csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(tokenAuthenticationEntryPoint)
                .accessDeniedHandler(tokenAccessDeniedHandler)

            .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            .and()
                .authorizeRequests()
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .mvcMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/menus/**").permitAll()
                .antMatchers("/api/**").permitAll()
                .antMatchers("/admin/*").hasRole("ADMIN")
                .anyRequest().authenticated()
            .and()
                .apply(new TokenSecurityConfig(tokenManager))

            .and().build();

    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
