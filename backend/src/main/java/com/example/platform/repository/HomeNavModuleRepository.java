package com.example.platform.repository;

import com.example.platform.entity.HomeNavModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HomeNavModuleRepository extends JpaRepository<HomeNavModule, Long> {

    List<HomeNavModule> findAllByOrderBySortOrderAscIdAsc();

    List<HomeNavModule> findByVisibleTrueOrderBySortOrderAscIdAsc();

    Optional<HomeNavModule> findByCode(HomeNavModule.Code code);
}
