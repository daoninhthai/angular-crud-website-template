package com.company.assetmanagementwebservice.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.company.assetmanagementwebservice.entity.User;
import com.company.assetmanagementwebservice.model.dto.UserDTO;
import com.company.assetmanagementwebservice.model.request.ChangePasswordRequest;
import com.company.assetmanagementwebservice.model.request.CreateUserRequest;
import com.company.assetmanagementwebservice.model.request.UpdateUserRequest;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface UserService {
  List<UserDTO> getAllUser();

  UserDTO findByUserName(String username);

  User findUserByUsername(String username);

  UserDTO getUserById(int id);

  UserDTO updateUser(UpdateUserRequest request, int id);

  UserDTO disableUser(UpdateUserRequest request, int id);

  UserDTO createUser(CreateUserRequest request);

  List<UserDTO> searchByNameOrStaffCode(String keyword);

  List<UserDTO> getUsers(String type, String keyword);

  UserDTO changePassword(ChangePasswordRequest request, String username);

  void importToDb(List<MultipartFile> multipartfiles);

}
