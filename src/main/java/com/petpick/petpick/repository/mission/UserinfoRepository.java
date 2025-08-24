package com.petpick.petpick.repository.mission;

import com.petpick.petpick.entity.mission.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserinfoRepository extends JpaRepository<UserInfo, Long> {
    
}
