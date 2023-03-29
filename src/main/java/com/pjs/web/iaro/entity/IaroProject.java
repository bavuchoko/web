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
public class IaroProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    @Column(name = "project_id")
    private Long id;

    private String subject;
    private String description;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name="account_id")
    @JsonSerialize(using = AccountSerializer.class)
    private Account opener;


    @OneToMany(mappedBy = "project")
    private List<IaroIssue> issues;



    private LocalDateTime registerDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime deadLine;


}
