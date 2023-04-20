package com.company.assetmanagementwebservice.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.company.assetmanagementwebservice.exception.BadRequestException;
import com.company.assetmanagementwebservice.exception.BusinessException;
import com.company.assetmanagementwebservice.exception.InternalServerException;
import com.company.assetmanagementwebservice.exception.NotFoundException;
import com.company.assetmanagementwebservice.repository.AssignmentRepository;
import com.company.assetmanagementwebservice.repository.UserRepository;
import com.company.assetmanagementwebservice.service.UserService;
import com.company.assetmanagementwebservice.entity.Assignment;
import com.company.assetmanagementwebservice.exception.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.company.assetmanagementwebservice.entity.Authority;
import com.company.assetmanagementwebservice.entity.User;
import com.company.assetmanagementwebservice.model.dto.UserDTO;
import com.company.assetmanagementwebservice.model.mapper.UserMapper;
import com.company.assetmanagementwebservice.model.request.ChangePasswordRequest;
import com.company.assetmanagementwebservice.model.request.CreateUserRequest;
import com.company.assetmanagementwebservice.model.request.UpdateUserRequest;
import org.springframework.web.multipart.MultipartFile;


@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, AssignmentRepository assignmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDTO> getAllUser() {

        List<User> users = userRepository.findByStatus("enabled");
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            result.add(UserMapper.toUserDTO(user));
        }
        return result;
    }

    @Override
    public UserDTO findByUserName(String username) {
        User user = userRepository.findByUsername(username);
        return UserMapper.toUserDTO(user);

    }


    @Override
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public UserDTO getUserById(int id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new NotFoundException("No user found");
        }
        return UserMapper.toUserDTO(user.get());
    }


    @Override
    public UserDTO updateUser(UpdateUserRequest request, int id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new NotFoundException("No user found");
        }
        User updateUser = UserMapper.mergeUpdate(request, user.get());
        userRepository.save(updateUser);
        return UserMapper.toUserDTO(updateUser);
    }

    @Override
    public UserDTO disableUser(UpdateUserRequest request, int id) {
        Optional<User> user = userRepository.findById(id);
        List<Assignment> assignment = assignmentRepository.findByUser_UsernameAndStateNot(request.getUsername(), -1);
        if (assignment.size() > 0) {
            throw new BusinessException("This user got some assignment");
        }
        User changeUserStatus = UserMapper.mergeDisable(request, user.get());
        try {
            userRepository.save(changeUserStatus);
        } catch (Exception ex) {
            throw new BadRequestException("Can't change user status");
        }
        return UserMapper.toUserDTO(changeUserStatus);
    }
    
    @Override
    public UserDTO createUser(CreateUserRequest request) {

        User user = userRepository.findByUsername(request.getUsername());
        long count = userRepository.count() + 1;
        String staffCode = "SD" + String.format("%04d", count);
        user = UserMapper.toUser(request);
        StringBuilder username = new StringBuilder(user.getFirstName().toLowerCase());
        String lastName = user.getLastName().toLowerCase();
        String[] tmp = lastName.split("\\s+");
        for (String s : tmp) {
            username.append(s.charAt(0));
        }
        String finalUsername = username.toString();
        Integer countUsername = userRepository.countByDuplicateFullName(username.toString());
        if (countUsername > 0) {
            finalUsername = username.toString() + countUsername.toString();
            user.setUsername(finalUsername);
        } else {
            user.setUsername(finalUsername);
        }
        DateTimeFormatter formatters = DateTimeFormatter.ofPattern("ddMMyyyy");
        String dob = user.getDob().format(formatters);
        user.setStaffCode(staffCode);


        user.setStatus("enabled");
        user.setPassword(passwordEncoder.encode(finalUsername + "@" + dob));
        user.setDefaultPassword(finalUsername + "@" + dob);
        user.setFirstLogin("true");
        //set location based on current logged in user (admin)

        
        Authority authority = new Authority();
        authority.setAuthority(request.getAuthority());
        authority.setUser(user);
        
        user.setAuthority(authority);
        userRepository.saveAndFlush(user);
        return UserMapper.toUserDTO(user);
    }


    @Override
    public List<UserDTO> searchByNameOrStaffCode(String keyword) {
    	String fullName = "%" + keyword + "%";
    	String staffCode = keyword;
        List<User> users = userRepository.findUserByFullNameOrStaffCode(fullName, staffCode);
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            result.add(UserMapper.toUserDTO(user));
        }
        return result;

    }

    @Override
    public UserDTO changePassword(ChangePasswordRequest request, String username) {

        User updateUser = UserMapper.toUser(request, username);


    try {
      updateUser.setFirstLogin("false");
      updateUser.setPassword(passwordEncoder.encode(request.getPassword()));
      userRepository.updatePassword(updateUser.getPassword(),updateUser.getFirstLogin(), username);
    } catch (Exception ex) {
      throw new InternalServerException("Can't update password");
    }

        return UserMapper.toUserDTO(updateUser);


    }

    //method used for filtering (type: ADMIN or STAFF), searching (by fullName or staffCode) and get all user
    @Override
    public List<UserDTO> getUsers(String type, String keyword) {

        List<User> users = new ArrayList<>();
        String fullName = "%" + keyword + "%";
        String staffCode = keyword;
        if (type == null && keyword == null) {
            users = userRepository.findByStatus("enabled");
        } else if (type != null && keyword == null) {
            users = userRepository.findByAuthority_authorityAndStatus(type, "enabled");
        } else if (type == null && keyword != null) {
            users = userRepository.findUserByFullNameOrStaffCode(fullName, staffCode);
        } else if (type != null && keyword != null) {
            users = userRepository.findUserByFullNameOrStaffCode(fullName, staffCode);
            users = users.stream().filter(user -> user.getAuthority().getAuthority().equals(type.toUpperCase())).collect(Collectors.toList());
        }
        return users.stream()

        		.map(UserMapper::toUserDTO)
        		.collect(Collectors.toList());
    }

    @Override
    public void importToDb(List<MultipartFile> multipartfiles) {
        if (!multipartfiles.isEmpty()) {
            List<User> transactions = new ArrayList<>();
            multipartfiles.forEach(multipartfile -> {
                try {
                    XSSFWorkbook workBook = new XSSFWorkbook(multipartfile.getInputStream());

                    XSSFSheet sheet = workBook.getSheetAt(0);
                    // looping through each row
                    for (int rowIndex = 0; rowIndex < getNumberOfNonEmptyCells(sheet, 0) - 1; rowIndex++) {
                        // current row
                        XSSFRow row = sheet.getRow(rowIndex);
                        // skip the first row because it is a header row
                        if (rowIndex == 0) {
                            continue;
                        }
                        String firstName = String.valueOf(row.getCell(0));
                        String lastName = String.valueOf(row.getCell(1));
                        LocalDate dob = LocalDate.parse(row.getCell(2).toString());
                        String gender = String.valueOf(row.getCell(3));

                        LocalDate joinedDate = LocalDate.parse(row.getCell(4).toString());
                        Authority authority = new Authority();
                        authority.setAuthority(String.valueOf(row.getCell(5)));

                        String location = String.valueOf(row.getCell(6));

                        User transaction = User.builder().firstName(firstName).lastName(lastName)
                                .dob(dob).gender(gender).joinedDate(joinedDate)
                                .authority(authority).location(location).build();
                        transactions.add(transaction);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            if (!transactions.isEmpty()) {
                // save to database
                userRepository.saveAll(transactions);
            }
        }
    }

    private Object getValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case ERROR:
                return cell.getErrorCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            case _NONE:
                return null;
            default:
                break;
        }
        return null;
    }

    public static int getNumberOfNonEmptyCells(XSSFSheet sheet, int columnIndex) {
        int numOfNonEmptyCells = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                XSSFCell cell = row.getCell(columnIndex);
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    numOfNonEmptyCells++;
                }
            }
        }
        return numOfNonEmptyCells;
    }


}
