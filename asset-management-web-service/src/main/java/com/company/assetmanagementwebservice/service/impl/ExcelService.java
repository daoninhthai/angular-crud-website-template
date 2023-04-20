package com.company.assetmanagementwebservice.service.impl;

import com.company.assetmanagementwebservice.entity.User;
import com.company.assetmanagementwebservice.repository.UserRepository;
import com.company.assetmanagementwebservice.service.ExcelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ExcelService {
    @Autowired
    UserRepository repository;

    public void save(MultipartFile file) {
        try {
            List<User> users = ExcelHelper.excelToTutorials(file.getInputStream());
            repository.saveAll(users);
        } catch (IOException e) {
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        }
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }
}
