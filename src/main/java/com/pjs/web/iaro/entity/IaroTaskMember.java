package com.pjs.web.iaro.entity;


import com.pjs.web.iaro.entity.embeddedId.IaroTaskMemberId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaroTaskMember {


    @EmbeddedId
    private IaroTaskMemberId id;
}
