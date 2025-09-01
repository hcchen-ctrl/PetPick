package tw.petpick.petpick.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.petpick.petpick.model.Userinfo;
import tw.petpick.petpick.repository.UserinfoRepository;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserinfoRepository repo;

    @PatchMapping("/{id}/promote")
    @PreAuthorize("hasRole('ADMIN')")
    public void promote(@PathVariable Long id) {
        Userinfo u = repo.findById(id).orElseThrow();
        u.setAuthority("ROLE_ADMIN");
        u.setRole("ADMIN");
        repo.save(u);
    }
}
