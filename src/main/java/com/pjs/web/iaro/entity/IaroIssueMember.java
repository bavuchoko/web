package com.pjs.web.iaro.entity;


import com.pjs.web.iaro.entity.embeddedId.IaroIssueMemberId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IaroIssueMember {

    @EmbeddedId
    private IaroIssueMemberId id;

}
