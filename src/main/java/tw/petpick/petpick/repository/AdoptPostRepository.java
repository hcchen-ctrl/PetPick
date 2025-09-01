package tw.petpick.petpick.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import tw.petpick.petpick.model.AdoptPost;
import tw.petpick.petpick.model.enums.PostStatus;

public interface AdoptPostRepository
    extends JpaRepository<AdoptPost, Long>, 
    JpaSpecificationExecutor<AdoptPost> {

    // ① 讓「我的進度頁」撈到當前登入使用者的刊登（按建立時間新到舊）
    List<AdoptPost> findByPostedByUserIdOrderByCreatedAtDesc(Long postedByUserId);

    // 新增：依狀態過濾（審核中/已通過…）
  List<AdoptPost> findByPostedByUserIdAndStatusOrderByCreatedAtDesc(Long postedByUserId, PostStatus status);
}
