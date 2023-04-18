package com.company.assetmanagementwebservice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
@Component
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Integer id;


    private String username;


    private String firstName;


    private String lastName;


    private LocalDate dob;


    private String gender;


    private String staffCode;


    private LocalDate joinedDate;


    private String status;

    @JsonIgnore
    private String password;


    private String location;


    private String authority;


    private String defaultPassword;


    private String firstLogin;

    private List<AssignmentDTO> assignments;




}
