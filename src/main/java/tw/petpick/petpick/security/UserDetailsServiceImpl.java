package tw.petpick.petpick.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import tw.petpick.petpick.model.Userinfo;
import tw.petpick.petpick.repository.UserinfoRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserinfoRepository repo;
    public UserDetailsServiceImpl(UserinfoRepository repo) { this.repo = repo; }

    @Override
    public UserDetails loadUserByUsername(String accountemail) throws UsernameNotFoundException {
        Userinfo u = repo.findByAccountemail(accountemail)
                .orElseThrow(() -> new UsernameNotFoundException("帳號不存在"));

        String role = (u.getRole()==null ? "USER" : u.getRole().toUpperCase());
        if (!role.startsWith("ROLE_")) role = "ROLE_" + role;

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getAccountemail())
                .password(u.getPassword())   // 開發期可 {noop}123456；上線請用 bcrypt
                .authorities(role)
                .build();
    }
}
