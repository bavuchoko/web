package com.pjs.web.accounts;

import com.pjs.web.account.entity.Account;
import com.pjs.web.account.entity.AccountRole;
import com.pjs.web.account.repository.AccountJapRepository;
import com.pjs.web.account.service.AccountService;
import com.pjs.web.account.service.AccountService;

import com.pjs.web.account.service.impl.AccountServiceImpl;
import com.pjs.web.config.AppProperties;
import common.BaseControllerTest;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


public class AccountServiceTest extends BaseControllerTest {

    @Autowired
    AccountService accountService;

    @Autowired
    AccountService accountServiceImpl;

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

        UserDetails jonsgu = accountServiceImpl.loadUserByUsername(appProperties.getUserUsername());
        //Then
        assertThat(passwordEncoder.matches(appProperties.getUserPassword(), jonsgu.getPassword())).isTrue();
    }

    @Test
    public void findByUsernameFail() {
        String username = "reandom@eamil.com";

        try {
            accountServiceImpl.loadUserByUsername(username);
            fail("test fail");
        } catch (UsernameNotFoundException e) {
            assertThat(e.getMessage()).containsSequence(username);
        }


    }

}