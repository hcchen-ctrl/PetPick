package com.petpick.petpick.DTO;

import com.petpick.petpick.entity.AdoptPost;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PostPageDTO {
    private List<PostSummaryDTO> content;
    private int number;
    private int totalPages;
    private long totalElements;

    public static PostPageDTO from(Page<AdoptPost> page) {
        PostPageDTO dto = new PostPageDTO();
        dto.setContent(page.getContent().stream().map(PostSummaryDTO::from).toList());
        dto.setNumber(page.getNumber());
        dto.setTotalPages(page.getTotalPages());
        dto.setTotalElements(page.getTotalElements());
        return dto;
    }
}

