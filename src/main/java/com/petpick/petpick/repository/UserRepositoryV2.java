package com.petpick.petpick.repository;

import com.petpick.petpick.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepositoryV2 extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
    // 依 Email 查詢
    UserEntity findByAccountemail(String accountemail);
}
