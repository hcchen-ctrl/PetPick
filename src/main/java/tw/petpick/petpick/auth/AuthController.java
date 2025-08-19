package tw.petpick.petpick.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

   @PostMapping("/login")
  public Map<String,Object> login(@RequestBody Map<String,String> in, HttpSession session){
    String account = in.get("account");
    String password = in.get("password");

    if ("admin".equals(account) && "123456".equals(password)) {
      session.setAttribute("uid", 1L);      // employees.employee_id = 1
      session.setAttribute("role", "ADMIN");
      session.setAttribute("name", "測試管理員");
    } else if ("user".equals(account) && "123456".equals(password)) {
      session.setAttribute("uid", 1L);      // userinfo.user_id = 1
      session.setAttribute("role", "USER");
      session.setAttribute("name", "測試會員");
    } else {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "帳號或密碼錯誤");
    }
    return Map.of("ok", true);
  }

  // ✅ /api/auth/logout
  @PostMapping("/logout")
  public Map<String,Object> logout(HttpSession session){
    session.invalidate();
    return Map.of("ok", true);
  }

  // ✅ /api/auth/status
  @GetMapping("/status")
  public Map<String,Object> status(HttpSession session){
    Object uid = session.getAttribute("uid");
    if (uid == null) return Map.of("loggedIn", false);
    return Map.of(
      "loggedIn", true,
      "uid", uid,
      "role", session.getAttribute("role"),
      "name", session.getAttribute("name")
    );
  }
}
