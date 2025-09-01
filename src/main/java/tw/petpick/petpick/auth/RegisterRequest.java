package tw.petpick.petpick.auth;

public record RegisterRequest(
    String username,        // 姓名/暱稱 -> userinfo.username
    String accountemail,    // 登入帳號   -> userinfo.accountemail (UNIQUE)
    String phonenumber,     // 手機       -> userinfo.phonenumber
    String password         // 明文 -> 會被加密成 BCrypt 存到 userinfo.password
) {}