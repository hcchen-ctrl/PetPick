package com.petpick.petpick.repository;

import com.petpick.petpick.entity.AdoptPost;
import com.petpick.petpick.model.enums.PostStatus;
import com.petpick.petpick.model.enums.SourceType;
import org.springframework.data.jpa.domain.Specification;


public class AdoptPostSpecs {
  public static Specification<AdoptPost> statusEq(PostStatus s) {
  return (r, q, cb) -> s == null ? null : cb.equal(r.get("status"), s);
}
  public static Specification<AdoptPost> sourceType(SourceType t) {
    return (r, q, cb) -> t==null ? null : cb.equal(r.get("sourceType"), t);
  }
  public static Specification<AdoptPost> city(String city) {
    return (r, q, cb) -> (city==null||city.isBlank()) ? null : cb.equal(r.get("city"), city);
  }
  public static Specification<AdoptPost> sex(String sex) {
    return (r, q, cb) -> (sex==null||sex.isBlank()) ? null : cb.equal(r.get("sex"), sex);
  }
  public static Specification<AdoptPost> species(String sp) {
    return (r, q, cb) -> (sp==null||sp.isBlank()) ? null : cb.equal(r.get("species"), sp);
  }
  public static Specification<AdoptPost> age(String age) {
    return (r, q, cb) -> (age==null||age.isBlank()) ? null : cb.equal(r.get("age"), age);
  }
  public static Specification<AdoptPost> keyword(String kw) {
    if (kw==null || kw.isBlank()) return null;
    return (r, q, cb) -> {
      String like = "%"+kw.trim()+"%";
      return cb.or(
        cb.like(r.get("title"), like),
        cb.like(r.get("breed"), like),
        cb.like(r.get("color"), like),
        cb.like(r.get("description"), like),
        cb.like(r.get("district"), like),
        cb.like(r.get("city"), like)
      );
    };
  }

  public static Specification<AdoptPost> district(String district) {
  return (r, q, cb) -> (district == null || district.isBlank())
      ? null : cb.equal(r.get("district"), district);
}
}
