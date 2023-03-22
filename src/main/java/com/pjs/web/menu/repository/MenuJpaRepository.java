package com.pjs.web.menu.repository;

import com.pjs.web.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuJpaRepository extends JpaRepository<Menu, Integer> {
    List findAllBy();


}
