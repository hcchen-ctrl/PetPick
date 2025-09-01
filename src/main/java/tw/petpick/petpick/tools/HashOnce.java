package tw.petpick.petpick.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashOnce {
    public static void main(String[] args) {
        var encoder = new BCryptPasswordEncoder();

        // ① 這裡改成你想要的原始密碼（例如 "123456" 或 "Admin123"）
        String raw = (args.length > 0) ? args[0] : "123456";

        // ② 產生雜湊並印出
        String hash = encoder.encode(raw);
        System.out.println("RAW   : " + raw);
        System.out.println("BCrypt: " + hash);
    }
}
