package tw.petpick.petpick.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.model.Userinfo;
import tw.petpick.petpick.repository.UserinfoRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserinfoRepository userRepo;
    private final AuthService authService; // ★ 新增

    // ===== 既有：JSON 登入 =====
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> in,
                                     HttpServletRequest request) {
        String account  = in.getOrDefault("account","").trim();   // accountemail
        String password = in.getOrDefault("password","").trim();

        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(account, password)
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

        // ★★★ 新增：把自己的 uid 放進 session，給 /my-adoptions 用
        Userinfo u = userRepo.findByAccountemail(account).orElse(null);
        if (u != null) {
            var session = request.getSession(true);
            session.setAttribute("uid", u.getUserId());
            session.setAttribute("role", u.getRole());        // 可選
            session.setAttribute("name", u.getUsername());    // 可選
        }

    return Map.of("ok", true);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest req, HttpServletResponse res) {
        SecurityContextHolder.clearContext();
        var session = req.getSession(false);
        if (session != null) session.invalidate(); // ★ 真的把 session 清掉
        return Map.of("ok", true);
    }

    @GetMapping("/status")
    public Map<String, Object> status(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Map.of("loggedIn", false);
        }
        String username = auth.getName(); // 你的 UserDetailsService 應該回傳 accountemail
        Userinfo u = userRepo.findByAccountemail(username).orElse(null);
        if (u == null) return Map.of("loggedIn", false);

        String role = u.getRole();
        if (role != null && role.startsWith("ROLE_")) role = role.substring(5);

        return Map.of(
            "loggedIn", true,
            "uid", u.getUserId(),
            "role", role,
            "name", u.getUsername(),
            "phone", u.getPhonenumber(),
            "email", u.getAccountemail()
        );
    }

   // 查 Email 是否可用：/api/auth/check-email?e=xxx
    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(@RequestParam("e") String email) {
        boolean available = !userRepo.existsByAccountemail(email);
        return Map.of("available", available);
    }

    // 註冊：把 EMAIL_TAKEN 轉成 409，前端好顯示
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            authService.register(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            if ("EMAIL_TAKEN".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "此 Email 已被註冊"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestBody VerifyEmailRequest req) {
        return Map.of("ok", authService.verifyEmail(req));
    }

    // （開發用，正式請關）
    @Value("${petpick.dev.expose-verify-code:false}")
    private boolean devExpose;

    @GetMapping("/dev-code")
    public ResponseEntity<?> devCode(@RequestParam String email) {
        if (!devExpose) return ResponseEntity.status(404).body(Map.of("error","disabled"));
        String code = authService.devPeekCode(email);
        return (code==null) ? ResponseEntity.status(404).body(Map.of("error","not found"))
                            : ResponseEntity.ok(Map.of("code", code));
    }
}
