package com.company.assetmanagementwebservice.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.company.assetmanagementwebservice.entity.Request;

public interface RequestRepository extends JpaRepository<Request, Integer> {

  Request findByAssignment_Id(Integer id);

  
  List<Request> findByAssignment_Asset_AssetCodeContainsOrAssignment_Asset_AssetNameContainsOrRequestByContains(String assetCode, String assetName, String username);
  
  List<Request> findRequestsByState(Integer state);

  List<Request> findRequestsByReturnedDate(LocalDate returnedDate);
}
