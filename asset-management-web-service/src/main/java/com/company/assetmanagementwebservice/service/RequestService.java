package com.company.assetmanagementwebservice.service;

import java.time.LocalDate;
import java.util.List;
import com.company.assetmanagementwebservice.model.dto.RequestDTO;


public interface RequestService {
  List<RequestDTO> getAllRequest();

  RequestDTO create(RequestDTO requestDTO);

  List<RequestDTO> filterRequests(Integer state, LocalDate returnedDate, String keyword);

  void deleteById(Integer id);

  RequestDTO complete(Integer id);
}
