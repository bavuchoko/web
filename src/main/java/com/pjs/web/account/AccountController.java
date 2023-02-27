package com.pjs.web.account;

import com.pjs.web.account.dto.AccountDto;
import com.pjs.web.account.entity.Account;
import com.pjs.web.account.entity.AccountRole;
import com.pjs.web.account.service.AccountService;
import com.pjs.web.common.annotation.CurrentUser;
import com.pjs.web.config.filter.JwtFilter;
import com.pjs.web.config.jwt.CookieUtil;
import com.pjs.web.config.jwt.TokenManager;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/api/user",  produces = MediaTypes.HAL_JSON_VALUE)
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private final TokenManager tokenManager;


    @Autowired
    CookieUtil cookieUtil;


    @GetMapping
    public ResponseEntity loadUserList(Pageable pageable, PagedResourcesAssembler<Account> assembler){

        Page<Account> page = accountService.loadUserList(pageable);
        var pageResources = assembler.toModel(page, entity -> EntityModel.of(entity).add(linkTo(AccountController.class).withSelfRel()));
        pageResources.add(Link.of("/docs/index/html").withRel("profile"));
        return ResponseEntity.ok().body(pageResources);
    }

    @PostMapping("/authenticate")
    public ResponseEntity authenticate(@Valid @RequestBody AccountDto accountDto, Errors errors, HttpServletResponse response) {

        if(errors.hasErrors()){
            return badRequest(errors);
        }
        try {
            AccountDto authirize = accountService.authorize(accountDto.getUsername(), accountDto.getPassword(), response);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + authirize.getToken());
            return new ResponseEntity(authirize, httpHeaders, HttpStatus.OK);
        }catch (BadCredentialsException e){
            return new ResponseEntity<>("fail to login",HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/join")
    public ResponseEntity creatAccount(@Valid @RequestBody AccountDto accountDto,Errors errors, HttpServletResponse response) {
        if (errors.hasErrors()) {
            return badRequest(errors);
        }
        try {
            accountDto.setRoles(Set.of(AccountRole.USER));
            Account account =accountDto.toEntity();
            accountService.saveAccount(account);
            AccountDto authirize = accountService.authorize(accountDto.getUsername(), accountDto.getPassword(), response);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + authirize.getToken());
            return new ResponseEntity(authirize, httpHeaders, HttpStatus.OK);
        }catch (IllegalStateException e){
            return new ResponseEntity<>("some fields are unexceptable",HttpStatus.BAD_REQUEST);
        }
    }


    //Todo 중요!!
    /**
     * 쿠키에서 리프레쉬 토큰을 가져와서 토큰검증 후 리프레쉬토큰의 만료시간이 지나지 안았으면 새로운 토큰을 발급한다.
     * 실제 테스트 해보니 postman에서 기존에 토큰 요청을 한 경우 요청 헤더에 갱신토큰이 셋 되버려서 엑세스 토큰 자체가 없는 경우에도 이 api를  요청하면
     * 새 토큰을 발급해버리는 경우가 생긴다.
     * 결국 엑세스 토큰 자체가 없는 경우(예외)와 토큰이 만료된 경우(적절)의 로직을 분리해야 할 것 같다.
     * jwtAuthenticationEntryPoint 에서 401 처리를 할때 토큰이 없는경우를 구별해줘야 할 듯
     */

    @GetMapping("/refreshtoken")
    public ResponseEntity refreshToken(HttpServletRequest request) {
        AccountDto authirize = accountService.refreshToken(request);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + authirize.getToken());
        return new ResponseEntity(authirize, httpHeaders, HttpStatus.OK);
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest req){
        accountService.logout(req);
    }

    @GetMapping("/validation")
    public String valdationTimeCheck(@CurrentUser Account account) {
        System.out.println(account.getNickname());
        return  "aa";
    }



    @GetMapping("/permitted")
    public String permitted(){
        return "everybody permiited";
    }


    @GetMapping("/admintest")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public String admintest(){
        return "only admin permiited";
    }

    @GetMapping("/usertest")
    @PreAuthorize("hasAnyRole('USER')")
    public String usertest(){
        return "only user permiited";
    }

    private ResponseEntity<EntityModel<Errors>> badRequest(Errors errors) {
        return ResponseEntity.badRequest().body(EntityModel.of(errors).add(linkTo(AccountController.class).slash("/join").withRel("redirect")));
    }


}
