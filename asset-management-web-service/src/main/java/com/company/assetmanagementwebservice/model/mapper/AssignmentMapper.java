package com.company.assetmanagementwebservice.model.mapper;

import com.company.assetmanagementwebservice.entity.Assignment;
import com.company.assetmanagementwebservice.model.dto.AssignmentDTO;
import org.springframework.stereotype.Component;

@Component
public class AssignmentMapper {
    private final AssetMapper assetMapper = new AssetMapper();

    //map from Assignment to AssignmentDTO
    public AssignmentDTO fromEntity(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        dto.setAssetDTO(assetMapper.fromEntity(assignment.getAsset()));
        if (assignment.getUser() != null) {
            dto.setUserDTO(UserMapper.toUserDTONoAssignment(assignment.getUser()));
        }
        dto.setAssignedBy(assignment.getAssignedBy());
        dto.setAssignedDate(assignment.getAssignedDate());
        dto.setState(assignment.getState());
        dto.setNote(assignment.getNote());
        return dto;
    }

    //map from AssignmentDTO to Assignment
    public Assignment fromDTO(AssignmentDTO payload) {
        Assignment assignment = new Assignment();
        assignment.setId(payload.getId());
        assignment.setAssignedDate(payload.getAssignedDate());
        assignment.setState(payload.getState());
        assignment.setNote(payload.getNote());
        return assignment;
    }
    //map from Assignment to AssignmentDTO
    public AssignmentDTO fromEntityNoUser(Assignment assignment) {
        AssignmentDTO dto = new AssignmentDTO();
        dto.setId(assignment.getId());
        dto.setAssetDTO(assetMapper.fromEntity(assignment.getAsset()));
        dto.setAssignedBy(assignment.getAssignedBy());
        dto.setAssignedDate(assignment.getAssignedDate());
        dto.setState(assignment.getState());
        dto.setNote(assignment.getNote());
        return dto;
    }
    public Assignment merge(AssignmentDTO payload, Assignment entity){
        entity.setAssignedDate(payload.getAssignedDate());
        entity.setNote(payload.getNote());
        entity.setState(payload.getState());
        return entity;
    }

}
