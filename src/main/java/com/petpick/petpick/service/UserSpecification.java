package com.petpick.petpick.service;

import com.petpick.petpick.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

// UserSpecification.java
public class UserSpecification {

    public static Specification<UserEntity> buildSearchSpec(String q, String isaccount, String isblacklist, String role) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 關鍵字搜尋 (會員ID、用戶名、Email、電話)
            if (StringUtils.hasText(q)) {
                String likePattern = "%" + q.toLowerCase() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.toString(root.get("userid")), "%" + q + "%"),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("accountemail")), likePattern),
                        criteriaBuilder.like(root.get("phonenumber"), "%" + q + "%")
                );
                predicates.add(keywordPredicate);
            }

            // 帳戶狀態篩選
            if (StringUtils.hasText(isaccount)) {
                predicates.add(criteriaBuilder.equal(root.get("isaccount"), isaccount));
            }

            // 黑名單狀態篩選
            if (StringUtils.hasText(isblacklist)) {
                predicates.add(criteriaBuilder.equal(root.get("isblacklist"), isblacklist));
            }

            // 角色篩選
            if (StringUtils.hasText(role)) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
