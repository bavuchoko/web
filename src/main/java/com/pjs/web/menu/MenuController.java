package com.pjs.web.menu;

import com.pjs.web.menu.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/menus")
public class MenuController {

    @Autowired
    MenuService menuService;

    @GetMapping
    public ResponseEntity getMenus() {
        List menus =menuService.getMenus();
        return ResponseEntity.ok(menus);
    }

}
