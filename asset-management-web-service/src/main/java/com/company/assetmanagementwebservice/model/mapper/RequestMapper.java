package com.company.assetmanagementwebservice.model.mapper;

import com.company.assetmanagementwebservice.entity.Assignment;
import com.company.assetmanagementwebservice.entity.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.company.assetmanagementwebservice.model.dto.RequestDTO;

@Component
public class RequestMapper {
  @Autowired
  private AssignmentMapper assignmentMapper;

  public RequestDTO fromEntity(Request entity) {
    RequestDTO dto = new RequestDTO();
    dto.setId(entity.getId());
    dto.setState(entity.getState());
    dto.setReturnedDate(entity.getReturnedDate());
    dto.setAcceptedBy(entity.getAcceptedBy());
    dto.setRequestBy(entity.getRequestBy());
    Assignment assignment = entity.getAssignment();
    dto.setAssignmentDTO(assignmentMapper.fromEntity(assignment));
    return dto;
  }

}
