package com.petpick.petpick.repository;

import java.util.List;

import com.petpick.petpick.entity.PostReview;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PostReviewRepository extends JpaRepository<PostReview, Long> {
    List<PostReview> findByPostIdOrderByCreatedAtDesc(Long postId);
}
