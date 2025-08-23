package com.petpick.petpick.repository;

import com.petpick.petpick.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity findByAccountemail(String accountemail);

}
