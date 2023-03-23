package com.pjs.web.iaro.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pjs.web.account.dto.AccountSerializer;
import com.pjs.web.account.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaroTask {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "task_id")
    private Long id;

    private String subject;
    private String description;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account opner;

    @OneToMany(fetch =FetchType.LAZY)
    @JoinColumn(name="account_id")
    @JsonSerialize(using = AccountSerializer.class)
    private List<Account> assignedUser;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private IaroProject belongToProject;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "workset_id")
    private IaroWorkSet belongToWorkSet;

    private int order;

    private LocalDateTime openDate;
    private LocalDateTime deadLine;
    private LocalDateTime endDate;
}