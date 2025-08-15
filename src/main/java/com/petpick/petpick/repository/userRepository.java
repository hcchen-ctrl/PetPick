package com.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.petpick.petpick.entity.userEntity;

import java.util.Optional;

public interface userRepository extends JpaRepository<userEntity,Long> {

    Optional<userEntity> findByAccountemail(String email);
   }
