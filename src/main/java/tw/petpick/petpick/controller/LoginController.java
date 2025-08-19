package tw.petpick.petpick.controller;

import tw.petpick.petpick.dto.LoginRequest;
import tw.petpick.petpick.model.Employee;
import tw.petpick.petpick.model.Userinfo;
import tw.petpick.petpick.repository.EmployeeRepository;
import tw.petpick.petpick.repository.UserinfoRepository;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserinfoRepository userinfoRepository;
    private final EmployeeRepository employeeRepository;

    public LoginController(UserinfoRepository userinfoRepository, EmployeeRepository employeeRepository) {
        this.userinfoRepository = userinfoRepository;
        this.employeeRepository = employeeRepository;
    }


   @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String role = request.getRole();

        if ("admin".equalsIgnoreCase(role)) {
            Optional<Employee> emp = employeeRepository.findByEmployeeNumberAndPassword(
                request.getEmployeeNumber(), request.getPassword()
            );
            return emp.map(e -> ResponseEntity.ok("管理員登入成功！"))
                    .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤"));
        }

        if ("user".equalsIgnoreCase(role)) {
            Userinfo user = userinfoRepository.findByUsernameAndPassword(
                request.getUsername(), request.getPassword()
            );
            return user != null
                ? ResponseEntity.ok(user)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("未知的角色");
    }
}
