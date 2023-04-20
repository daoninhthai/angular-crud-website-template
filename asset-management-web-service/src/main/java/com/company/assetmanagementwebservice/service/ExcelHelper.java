package com.company.assetmanagementwebservice.service;

import com.company.assetmanagementwebservice.entity.Authority;
import com.company.assetmanagementwebservice.entity.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERs = { "First name", "Last name", "Date of birth","Gender","Joined Date","Authority","Location","Username","Status","Staff" };
    static String SHEET = "User";

    public static boolean hasExcelFormat(MultipartFile file) {

        if (!TYPE.equals(file.getContentType())) {
            return false;
        }

        return true;
    }

    public static List<User> excelToTutorials(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);

            Sheet sheet = workbook.getSheet(SHEET);
            Iterator<Row> rows = sheet.iterator();

            List<User> users = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                Iterator<Cell> cellsInRow = currentRow.iterator();

                User user = new User();

                int cellIdx = 0;
                while (cellsInRow.hasNext()) {
                    Cell currentCell = cellsInRow.next();

                    switch (cellIdx) {


                        case 0:
                            user.setFirstName(currentCell.getStringCellValue());
                            break;

                        case 1:
                            user.setLastName(currentCell.getStringCellValue());
                            break;

                        case 2:
                            user.setDob(currentCell.getLocalDateTimeCellValue().toLocalDate());
                            break;
                        case 3:
                            user.setGender(currentCell.getStringCellValue());
                            break;
                        case 4:
                            user.setJoinedDate(currentCell.getLocalDateTimeCellValue().toLocalDate());
                            break;
                        case 5:
                            Authority authority = new Authority();
                            authority.setAuthority(currentCell.getStringCellValue());
                            authority.setUser(user);
                            user.setAuthority(authority);
                            break;
                        case 6:
                            user.setLocation(currentCell.getStringCellValue());
                            break;
                        case 7:
                            user.setUsername(currentCell.getStringCellValue());
                            break;
                        case 8:
                            user.setStatus(currentCell.getStringCellValue());
                            break;
                        case 9:
                            user.setStaffCode(currentCell.getStringCellValue());
                            break;
                        default:
                            break;
                    }

                    cellIdx++;
                }

                users.add(user);
            }

            workbook.close();

            return users;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }
}
