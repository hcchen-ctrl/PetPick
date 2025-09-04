package com.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.petpick.petpick.entity.PostReview;


public interface PostReviewRepository extends JpaRepository<PostReview, Long> {
    List<PostReview> findByPostIdOrderByCreatedAtDesc(Long postId);
}
