package tw.petpick.petpick.dto;

import lombok.Data;
import tw.petpick.petpick.model.AdoptPost;

@Data
public class PostSummaryDTO {
    private Long id;
    private String title;
    private String image1;
    private String image2;
    private String image3;
    private String species;
    private String breed;

    public static PostSummaryDTO from(AdoptPost p){
        PostSummaryDTO d = new PostSummaryDTO();
        d.id = p.getId();
        d.title = p.getTitle();
        d.image1 = p.getImage1();
        d.image2 = p.getImage2();
        d.image3 = p.getImage3();
        d.species = p.getSpecies();
        d.breed = p.getBreed();
        return d;
    }
}
