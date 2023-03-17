package com.pjs.web.account.service;

import com.pjs.web.account.dto.AccountDto;
import com.pjs.web.account.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface AccountService extends UserDetailsService {
    Account saveAccount(Account account);

    String authorize(AccountDto accountDto, HttpServletResponse response, HttpServletRequest request);

    String reIssueToken(HttpServletRequest request);

    void logout(HttpServletRequest req);

    Page<Account> loadUserList(Pageable pagable);
}
