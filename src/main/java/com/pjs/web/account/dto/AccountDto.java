package com.pjs.web.account.dto;

import com.pjs.web.account.entity.Account;
import com.pjs.web.account.entity.AccountRole;
import lombok.*;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private static ModelMapper modelMapper = new ModelMapper();

    private Integer id;

    @NotBlank(message = "아이디는 필수값입니다.")
    @Email(message = "이메일 형식에 맞지 않습니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수값입니다.")
    private String password;

    private String nickname;
    private Set<AccountRole> roles;

    private String token;

    private LocalDateTime joinDate;

    public Account toEntity() {
        modelMapper.getConfiguration()
                .setFieldAccessLevel(Configuration.AccessLevel.PRIVATE)
                .setFieldMatchingEnabled(true);
        return modelMapper.map(this, Account.class);
    }
}

