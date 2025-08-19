package tw.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.petpick.petpick.model.Userinfo;

public interface UserinfoRepository extends JpaRepository<Userinfo, Long> {
    Userinfo findByUsernameAndPassword(String username, String password);
}
