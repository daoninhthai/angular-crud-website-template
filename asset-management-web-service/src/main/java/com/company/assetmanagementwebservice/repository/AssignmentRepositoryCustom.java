package com.company.assetmanagementwebservice.repository;

import com.company.assetmanagementwebservice.entity.Assignment;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepositoryCustom {
    List<Assignment> get(String keyword, Integer state, LocalDate date);
}
