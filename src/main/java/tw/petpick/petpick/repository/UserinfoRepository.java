package tw.petpick.petpick.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.petpick.petpick.model.Userinfo;

public interface UserinfoRepository extends JpaRepository<Userinfo, Long> {
    Optional<Userinfo> findByAccountemail(String accountemail);
    Optional<Userinfo> findByAccountemailOrUsername(String email, String username);
    boolean existsByAccountemail(String accountemail);   // ★新增
}
