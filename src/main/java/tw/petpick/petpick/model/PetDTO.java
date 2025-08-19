package tw.petpick.petpick.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PetDTO {
    public int animal_id;
    public String animal_subid;
    public int animal_area_pkid;
    public int animal_shelter_pkid;
    public String animal_place;
    public String animal_kind;
    public String animal_Variety;
    public String animal_sex;
    public String animal_bodytype;
    public String animal_colour;
    public String animal_age;
    public String animal_sterilization;
    public String animal_bacterin;
    public String animal_foundplace;
    public String animal_title;
    public String animal_status;
    public String animal_remark;
    public String shelter_address;
    public String shelter_name;
    public String shelter_tel;
    public String album_file;
    public String album_base64;
}
