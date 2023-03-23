package com.pjs.web.iaro.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(BucketPK.class)
public class IaroBucket {

    @Id
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="project_id", columnDefinition = "int(11)", nullable = false)
    private IaroProject iaroProject;

    @Id
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="workset_id", columnDefinition = "int(11)", nullable = false)
    private IaroWorkSet iaroWorkSet;

    @Id
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name="task_id", columnDefinition = "int(11)", nullable = false)
    private IaroTask iaroTask;
}
