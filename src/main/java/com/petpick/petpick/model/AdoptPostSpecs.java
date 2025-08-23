package com.petpick.petpick.model;

import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;
import org.springframework.data.jpa.domain.Specification;


public class AdoptPostSpecs {

    public static Specification<AdoptPost> statusEq(PostStatus status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<AdoptPost> sourceType(SourceType type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("sourceType"), type);
        };
    }

    public static Specification<AdoptPost> city(String city) {
        return (root, query, cb) -> {
            if (city == null || city.isBlank()) return null;
            return cb.equal(root.get("city"), city);
        };
    }

    public static Specification<AdoptPost> district(String district) {
        return (root, query, cb) -> {
            if (district == null || district.isBlank()) return null;
            return cb.equal(root.get("district"), district);
        };
    }

    public static Specification<AdoptPost> sex(String sex) {
        return (root, query, cb) -> {
            if (sex == null || sex.isBlank()) return null;
            return cb.equal(root.get("sex"), sex);
        };
    }

    public static Specification<AdoptPost> species(String species) {
        return (root, query, cb) -> {
            if (species == null || species.isBlank()) return null;
            return cb.equal(root.get("species"), species);
        };
    }

    public static Specification<AdoptPost> age(String age) {
        return (root, query, cb) -> {
            if (age == null || age.isBlank()) return null;
            return cb.equal(root.get("age"), age);
        };
    }

    public static Specification<AdoptPost> keyword(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String like = "%" + q.trim() + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("description"), like),
                    cb.like(root.get("breed"), like),
                    cb.like(root.get("color"), like)
            );
        };
    }
}

