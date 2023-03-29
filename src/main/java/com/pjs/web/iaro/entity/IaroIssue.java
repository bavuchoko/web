package com.pjs.web.iaro.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@AllArgsConstructor
@NoArgsConstructor
public class IaroIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long id;

    private String subject;
    private String description;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account opener;

    @OneToMany(mappedBy = "id.issue")
    private List<IaroIssueMember> members;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference
    private IaroProject project;

    @OneToMany(mappedBy = "issue")
    private List<IaroTask> tasks;



    private LocalDateTime registerDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime deadLine;
}
