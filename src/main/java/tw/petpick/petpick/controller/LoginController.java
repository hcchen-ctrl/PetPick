// package tw.petpick.petpick.controller;

// import java.util.Optional;

// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import tw.petpick.petpick.dto.LoginRequest;
// import tw.petpick.petpick.model.Userinfo;
// import tw.petpick.petpick.repository.UserinfoRepository;

// @RestController
// @RequestMapping("/api")
// public class LoginController {

//     private final UserinfoRepository userinfoRepository;

//     public LoginController(UserinfoRepository userinfoRepository) {
//         this.userinfoRepository = userinfoRepository;
//     }

//     @PostMapping("/login")
//     public ResponseEntity<?> login(@RequestBody LoginRequest request) {
//         String account = request.getAccount();   // 可能是 username 或 email
//         String password = request.getPassword();

//         Optional<Userinfo> optUser = userinfoRepository.findByAccountemailOrUsername(account, account);

//         if (optUser.isEmpty()) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號不存在");
//         }

//         Userinfo user = optUser.get();
//         if (!password.equals(user.getPassword())) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("密碼錯誤");
//         }

//         // ✅ 登入成功：回傳使用者資料（可精簡）
//         return ResponseEntity.ok(user);
//     }
// }
