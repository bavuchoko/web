package com.pjs.web.account.service;

import com.pjs.web.account.dto.AccountDto;
import com.pjs.web.account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AccountService extends UserDetailsService {
    Account saveAccount(Account account);

    AccountDto authorize(String username, String pass, HttpServletResponse response);

    AccountDto refreshToken(HttpServletRequest request);

    void logout(HttpServletRequest req);

    Page<Account> loadUserList(Pageable pagable);
}
