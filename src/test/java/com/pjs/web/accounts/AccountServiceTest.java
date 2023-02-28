package com.example.monster.accounts;

import com.example.monster.accounts.entity.Account;
import com.example.monster.accounts.entity.AccountRole;
import com.example.monster.accounts.repository.AccountJapRepository;
import com.example.monster.accounts.service.AccountService;
import com.example.monster.accounts.service.CustomUserDetailsServiceImpl;
import com.example.monster.config.AppProperties;
import com.example.monster.common.BaseControllerTest;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


public class AccountServiceTest extends BaseControllerTest {

    @Autowired
    AccountService accountService;

    @Autowired
    CustomUserDetailsServiceImpl customUserDetailsService;

    @Autowired
    AccountJapRepository accountJapRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AppProperties appProperties;

    @BeforeEach
    public void delete() {
        accountJapRepository.deleteAll();
    }

    @Test
    public void findByUsername() {

        //Given
        Account account = Account.builder()
                .username(appProperties.getUserUsername())
                .password(appProperties.getUserPassword())
                .roles(Set.of(AccountRole.ADMIN, AccountRole.USER))
                .build();
        this.accountService.saveAccount(account);

        //When

        UserDetails jonsgu = customUserDetailsService.loadUserByUsername(appProperties.getUserUsername());
        //Then
        assertThat(passwordEncoder.matches(appProperties.getUserPassword(), jonsgu.getPassword())).isTrue();
    }

    @Test
    public void findByUsernameFail() {
        String username = "reandom@eamil.com";

        try {
            customUserDetailsService.loadUserByUsername(username);
            fail("test fail");
        } catch (UsernameNotFoundException e) {
            assertThat(e.getMessage()).containsSequence(username);
        }


    }

}