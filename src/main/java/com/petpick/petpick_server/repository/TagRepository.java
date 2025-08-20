package com.petpick.petpick_server.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick_server.entity.Tag;


public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findByTagIdIn(List<Long> ids);
    List<Tag> findByNameIn(List<String> names);
}
