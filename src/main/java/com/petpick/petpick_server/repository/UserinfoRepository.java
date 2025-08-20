package com.petpick.petpick_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.petpick.petpick_server.entity.UserInfo;

@Repository
public interface UserinfoRepository extends JpaRepository<UserInfo, Long> {
    
}
