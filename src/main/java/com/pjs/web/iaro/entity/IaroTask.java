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
@NoArgsConstructor
@AllArgsConstructor
public class IaroTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    private String subject;
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account taskOpener;


    @OneToMany(mappedBy = "id.task")
    private List<IaroTaskMember> members;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "project_id")
    @JsonBackReference
    private IaroProject project;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    @JsonBackReference
    private IaroIssue issue;


    private int orders;

    private LocalDateTime registerDate;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime deadLine;
}
