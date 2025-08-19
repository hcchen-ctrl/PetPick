package tw.petpick.petpick.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import tw.petpick.petpick.model.AdoptPost;
import tw.petpick.petpick.model.PostReview;
import tw.petpick.petpick.model.enums.PostStatus;
import tw.petpick.petpick.repository.AdoptPostRepository;
import tw.petpick.petpick.repository.PostReviewRepository;

@Service
public class AdoptPostService {

    private final AdoptPostRepository postRepo;
    private final PostReviewRepository reviewRepo;

    public AdoptPostService(AdoptPostRepository postRepo, PostReviewRepository reviewRepo) {
        this.postRepo = postRepo;
        this.reviewRepo = reviewRepo;
    }

    @Transactional
    public void updateStatusAndLog(Long postId,
                                PostStatus status,           // 直接用 enum
                                Long reviewerEmployeeId,
                                String reason) {

    // 只允許這三種狀態
    if (status != PostStatus.pending &&
        status != PostStatus.approved &&
        status != PostStatus.rejected) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad status");
    }

    AdoptPost post = postRepo.findById(postId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));

    if (post.getStatus() != status) {
        post.setStatus(status);          // ✅ 型別相同
        postRepo.save(post);
    }

    if (status == PostStatus.approved || status == PostStatus.rejected) {
        PostReview r = new PostReview();
        r.setPostId(postId);
        r.setAction(status == PostStatus.approved ? "approve" : "reject");
        r.setReason(reason);
        r.setReviewerEmployeeId(reviewerEmployeeId == null ? null : reviewerEmployeeId.intValue());
        reviewRepo.save(r);
    }
}
}
