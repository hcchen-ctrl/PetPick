package tw.petpick.petpick.auth;

import java.util.Random;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;          // ★ 新增
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.model.Userinfo;
import tw.petpick.petpick.repository.UserinfoRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserinfoRepository repo;
    private final PasswordEncoder encoder;
    private final ObjectProvider<JavaMailSender> mailProvider;

    @Value("${petpick.dev.expose-verify-code:false}")
    private boolean devExpose;

    @Value("${petpick.mail.from:}")
    private String mailFrom;                        // ★ 若有設定顯示寄件者

    public void register(RegisterRequest r) {
        var opt = repo.findByAccountemail(r.accountemail());
        Userinfo u;
        if (opt.isPresent()) {
            u = opt.get();
            if (Boolean.TRUE.equals(u.getEmailVerified())) {
                throw new IllegalArgumentException("EMAIL_TAKEN");
            }
            u.setUsername(r.username());
            u.setPhonenumber(r.phonenumber());
        } else {
            u = new Userinfo();
            u.setUsername(r.username());
            u.setAccountemail(r.accountemail());
            u.setPhonenumber(r.phonenumber());
            u.setAuthority("ROLE_USER");
            u.setRole("USER");
            u.setEmailVerified(false);
        }

        u.setPassword(encoder.encode(r.password()));
        String code = genCode(6);
        u.setEmailVerificationCode(code);
        repo.save(u);

        // ★ 失敗不中斷註冊
        sendVerifyMail(r.accountemail(), code);

        if (devExpose) {
            System.out.println("[DEV] Verify code for " + r.accountemail() + " = " + code);
        }
    }

    public boolean verifyEmail(VerifyEmailRequest r) {
        var uOpt = repo.findByAccountemail(r.email());
        if (uOpt.isEmpty()) return false;
        var u = uOpt.get();
        if (u.getEmailVerificationCode() != null && u.getEmailVerificationCode().equals(r.code())) {
            u.setEmailVerified(true);
            u.setEmailVerificationCode(null);
            repo.save(u);
            return true;
        }
        return false;
    }

    public String devPeekCode(String email) {
        return repo.findByAccountemail(email)
                   .map(Userinfo::getEmailVerificationCode)
                   .orElse(null);
    }

    private void sendVerifyMail(String to, String code) {
        JavaMailSender mail = mailProvider.getIfAvailable();
        if (mail == null) {
            System.out.println("[MAIL] JavaMailSender not configured. Skip sending.");
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) msg.setFrom(mailFrom);  // ★ 可選
            msg.setTo(to);
            msg.setSubject("PetPick 驗證碼");
            msg.setText("您的驗證碼是：" + code + "。\n若非本人操作，請忽略本信。");
            mail.send(msg);
        } catch (MailException e) {
            // ★ 只記錄，不要丟出
            System.out.println("[MAIL] Send failed: " + e.getMessage());
        }
    }

    private String genCode(int len) {
        var r = new Random();
        var sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }
}
