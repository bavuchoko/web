package com.pjs.web.iaro.entity;

import lombok.Data;

import java.io.Serializable;


@Data
public class BucketPK  implements Serializable {
    private int projectKey;

    private int worksetKey;

    private int taskKey;
}
