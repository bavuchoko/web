package com.pjs.web.iaro.entity.embeddedId;

import com.pjs.web.account.entity.Account;
import com.pjs.web.iaro.entity.IaroIssue;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Embeddable
public class IaroIssueMemberId implements Serializable {

    @ManyToOne
    @JoinColumn(name="account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name="issue_id")
    private IaroIssue issue;
}
