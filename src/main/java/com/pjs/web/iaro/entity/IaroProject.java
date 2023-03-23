package com.pjs.web.iaro.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pjs.web.account.dto.AccountSerializer;
import com.pjs.web.account.entity.Account;
import com.pjs.web.common.status.IaroProgress;
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
public class IaroProject {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO )
    @Column(name = "project_id")
    private Long id;

    private String subject;
    private String description;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name="account_id")
    @JsonSerialize(using = AccountSerializer.class)
    private Account opener;


    @OneToMany(fetch =FetchType.LAZY)
    @JoinColumn(name="workset_id")
    private List<IaroWorkSet> workSet;


    @OneToMany(fetch =FetchType.LAZY)
    @JoinColumn(name="task_id")
    private List<IaroTask> tasks;


    private LocalDateTime openDate;
    private LocalDateTime endDate;
    private LocalDateTime deadLine;


}
