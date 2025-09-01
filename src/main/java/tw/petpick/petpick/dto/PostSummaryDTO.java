package tw.petpick.petpick.dto;

import lombok.Data;
import tw.petpick.petpick.model.AdoptPost;

@Data
public class PostSummaryDTO {
    private Long id;
    private String title;
    private String image1, image2, image3;
    private String species;
    private String breed;
    private String sex;      // ← 確認有
    private String age;
    private String bodyType;
    private String city;
    private String district;

    public static PostSummaryDTO from(AdoptPost p){
        PostSummaryDTO d = new PostSummaryDTO();
        d.id = p.getId();
        d.title = p.getTitle();
        d.image1 = p.getImage1();
        d.image2 = p.getImage2();
        d.image3 = p.getImage3();
        d.species = p.getSpecies();
        d.breed = p.getBreed();
        d.sex = p.getSex();          // ← 這行一定要有
        d.age = p.getAge();
        d.bodyType = p.getBodyType();
        d.city = p.getCity();
        d.district = p.getDistrict();
        return d;
    }
}
