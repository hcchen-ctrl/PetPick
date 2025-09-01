package tw.petpick.petpick.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String account;   // 可以輸入 username 或 email
    private String password;
}
