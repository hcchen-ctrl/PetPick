package tw.petpick.petpick.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;         // 給 user 登入用
    private String employeeNumber;   // 給 admin 登入用
    private String password;
    private String role;       // "user" 或 "admin"

}
