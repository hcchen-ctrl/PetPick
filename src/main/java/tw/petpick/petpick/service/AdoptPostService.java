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
                                   PostStatus status,     // 只能 pending / approved / rejected
                                   Long reviewerUserId,
                                   String reason) {

        if (status != PostStatus.pending &&
            status != PostStatus.approved &&
            status != PostStatus.rejected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad status");
        }

        AdoptPost post = postRepo.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));

        if (post.getStatus() != status) {
            post.setStatus(status);
            postRepo.save(post);
        }

        if (status == PostStatus.approved || status == PostStatus.rejected) {
            PostReview r = new PostReview();
            r.setPostId(postId);
            r.setAction(status == PostStatus.approved ? "approve" : "reject");
            r.setReason(reason);
            r.setReviewerUserId(reviewerUserId);   // ✅ 改這裡
            reviewRepo.save(r);
        }
    }
}
