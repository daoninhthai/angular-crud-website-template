package com.company.assetmanagementwebservice.repository;

import com.company.assetmanagementwebservice.entity.Authority;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface AuthorityRepository  extends JpaRepository<Authority, Integer> {
    @Query(value = "SELECT user_id from authorities ", nativeQuery = true)
    Authority findByUserId(int user_id);

}
