// UserRepository.java
package com.petpick.petpick.repository;

import com.petpick.petpick.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByAccountemail(String accountemail);

    Optional<UserEntity> findByUsername(String username);
}