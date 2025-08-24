package com.petpick.petpick.repository.mission;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.mission.Tag;
import org.springframework.data.jpa.repository.JpaRepository;



public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findByTagIdIn(List<Long> ids);
    List<Tag> findByNameIn(List<String> names);
}
