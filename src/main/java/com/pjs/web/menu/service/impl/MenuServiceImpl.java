package com.pjs.web.menu.service.impl;

import com.pjs.web.menu.repository.MenuJpaRepository;
import com.pjs.web.menu.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    MenuJpaRepository menuJpaRepository;

    @Override
    public List getMenus() {
        return menuJpaRepository.findAllBy();
    }


}
