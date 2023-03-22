package com.pjs.web.menu.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuDto {

    private int id;

    private String name;

    private String uri;
}
