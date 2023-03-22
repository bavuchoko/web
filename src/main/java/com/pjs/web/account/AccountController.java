package com.pjs.web.account;

import com.pjs.web.account.dto.AccountDto;
import com.pjs.web.account.entity.Account;
import com.pjs.web.account.entity.AccountRole;
import com.pjs.web.account.service.AccountService;
import com.pjs.web.common.annotation.CurrentUser;
import com.pjs.web.config.filter.TokenFilter;
import com.pjs.web.config.utils.CookieUtil;
import com.pjs.web.config.token.TokenManager;
import io.jsonwebtoken.Jwts;
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
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/api/user",  produces = MediaTypes.HAL_JSON_VALUE)
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Autowired
    TokenManager tokenManager;
    @Autowired
    CookieUtil cookieUtil;


    @GetMapping
    public ResponseEntity loadUserList(Pageable pageable, PagedResourcesAssembler<Account> assembler){

        Page<Account> page = accountService.loadUserList(pageable);

        var pageResources = assembler.toModel(page, entity -> EntityModel.of(entity).add(linkTo(AccountController.class).withSelfRel()));
        pageResources.add(Link.of("/docs/index/html").withRel("profile"));
        return ResponseEntity.ok().body(pageResources);
    }

    @PostMapping("/authentication")
    public ResponseEntity authenticate(
            @Valid @RequestBody AccountDto accountDto,
            Errors errors,
            HttpServletResponse response,
            HttpServletRequest request) {

        if(errors.hasErrors()){
            return badRequest(errors);
        }
        Map responseMap = new HashMap();
        try {
            String accessToken = accountService.authorize(accountDto,response, request);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(TokenFilter.AUTHORIZATION_HEADER, "Bearer " + accessToken);

            responseMap.put("status", HttpStatus.OK);
            responseMap.put("result", "success");
            responseMap.put("accessToken", accessToken);
            responseMap.put("username", tokenManager.getUsername(accessToken));
            responseMap.put("nickname", tokenManager.getNickname(accessToken));
            responseMap.put("joinDate", tokenManager.getJoinDate(accessToken));
            responseMap.put("message", "success to create account");

            return new ResponseEntity(responseMap, httpHeaders, HttpStatus.OK);
        }catch (BadCredentialsException e){
            responseMap.put("status", HttpStatus.BAD_REQUEST);
            responseMap.put("result", "failed");
            responseMap.put("accessToken", null);
            responseMap.put("username", null);
            responseMap.put("nickname", null);
            responseMap.put("joinDate", null);
            responseMap.put("message", e.getMessage());
            return new ResponseEntity<>("fail to login",HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/create")
    public ResponseEntity creatAccount(
            @Valid @RequestBody AccountDto accountDto,
            Errors errors,
            HttpServletResponse response,
            HttpServletRequest request) {
        if (errors.hasErrors()) {
            return badRequest(errors);
        }
        Map responseMap = new HashMap();
        try {
            accountDto.setRoles(Set.of(AccountRole.USER));
            accountDto.setJoinDate(LocalDateTime.now());
            Account account =accountDto.toEntity();

            accountService.saveAccount(account);

            String accessToken = accountService.authorize(accountDto, response, request);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(TokenFilter.AUTHORIZATION_HEADER, "Bearer " + accessToken);

            responseMap.put("status", HttpStatus.OK);
            responseMap.put("result", "success");
            responseMap.put("accessToken", accessToken);
            responseMap.put("username", tokenManager.getUsername(accessToken));
            responseMap.put("nickname", tokenManager.getNickname(accessToken));
            responseMap.put("joinDate", tokenManager.getJoinDate(accessToken));
            responseMap.put("message", "success to create account");

            return new ResponseEntity(responseMap, httpHeaders, HttpStatus.OK);
        }catch (IllegalArgumentException e){

            responseMap.put("status", HttpStatus.BAD_REQUEST);
            responseMap.put("result", "failed");
            responseMap.put("accessToken", null);
            responseMap.put("username", null);
            responseMap.put("nickname", null);
            responseMap.put("joinDate", null);
            responseMap.put("message", e.getMessage());

            return new ResponseEntity<>(responseMap ,HttpStatus.BAD_REQUEST);
        }
    }


    //Todo 중요!!
    /**
     * 쿠키에서 리프레쉬 토큰을 가져와서 토큰검증 후 리프레쉬토큰의 만료시간이 지나지 안았으면 새로운 토큰을 발급한다.
     * 실제 테스트 해보니 처음 정상적인 인가를 진행하면 쿠키에 refreshToken을 넣어주고 이후 요청에선 엑세스 토큰이 없어도 이 api를 요청하기만 하면
     * 새 토큰을 발급해버린다.
     * 결국 엑세스 토큰 자체가 없는 경우(예외)와 토큰이 만료된 경우(적절)의 로직을 분리해야 할 것 같다.
     * jwtAuthenticationEntryPoint 에서 401 처리를 할때 토큰이 없는경우를 구별해줘야 할 듯
     */

    @GetMapping("/reissue")
    public ResponseEntity reissue(HttpServletRequest request) {

        Map responseMap = new HashMap();
        try{
            String accessToken = accountService.reIssueToken(request);
            responseMap.put("status", HttpStatus.OK);
            responseMap.put("result", "success");
            responseMap.put("accessToken", accessToken);
            responseMap.put("username", tokenManager.getUsername(accessToken));
            responseMap.put("nickname", tokenManager.getNickname(accessToken));
            responseMap.put("joinDate", tokenManager.getJoinDate(accessToken));
            responseMap.put("message", "success to create account");
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(TokenFilter.AUTHORIZATION_HEADER, "Bearer " + accessToken);

            return new ResponseEntity(accessToken, httpHeaders, HttpStatus.OK);

        }catch (Exception e){
            responseMap.put("status", HttpStatus.BAD_REQUEST);
            responseMap.put("result", "failed");
            responseMap.put("accessToken", null);
            responseMap.put("username", null);
            responseMap.put("nickname", null);
            responseMap.put("joinDate", null);
            responseMap.put("message", e.getMessage());

            return new ResponseEntity(responseMap, HttpStatus.BAD_REQUEST);
        }
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
