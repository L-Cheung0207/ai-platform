package com.example.platform.repository;

import com.example.platform.entity.SkillOperationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillOperationReportRepository extends JpaRepository<SkillOperationReport, Long> {

    Page<SkillOperationReport> findAllByOrderByGeneratedAtDesc(Pageable pageable);
}
