package com.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.petpick.petpick.entity.userEntity;

public interface userRepository extends JpaRepository<userEntity,Long> {

    boolean existsByAccountemail(String accountemail);
    userEntity findByAccountemail(String accountemail);
   }
