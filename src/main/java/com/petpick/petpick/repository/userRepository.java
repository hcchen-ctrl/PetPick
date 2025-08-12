package com.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.petpick.petpick.entity.userEntity;

public interface userRepository extends JpaRepository<userEntity,Long> {
    boolean existsByUsername(String username);
userEntity findByUsername(String username);

   }
