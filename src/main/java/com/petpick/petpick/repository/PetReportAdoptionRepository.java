package tw.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import tw.petpick.petpick.dto.AdoptionView;
import tw.petpick.petpick.entity.PetReportAdoption;

/**
 * 回報追蹤名單 Repository
 * 這三個查詢會一次把前端需要的欄位帶齊：
 * id, ownerName, petName, adoptionDate, imageUrl, requiredReports, reportCount
 *
 * 注意：
 * 1) 這裡使用「原生 SQL」（nativeQuery = true），直接查你的資料表：
 * - 名單表：petreport_adoptions a
 * - 回報表：petreport_feedbacks f（只拿來 COUNT）
 * - 圖片：a.image_url（你已決定新增在自己的表內）
 *
 * 2) 回傳型別使用投影介面 AdoptionView（位於 tw.petpick.petpick.dto）。
 */
public interface PetReportAdoptionRepository extends JpaRepository<PetReportAdoption, Long> {

  /** 還須回報：狀態 active，且回報次數 < 需求次數 */
  @Query(value = """
      SELECT
        a.id                         AS id,
        a.owner_name                 AS ownerName,
        a.pet_name                   AS petName,
        a.adoption_date              AS adoptionDate,
        a.image_url                  AS imageUrl,
        a.required_reports           AS requiredReports,
        COUNT(f.id)                  AS reportCount
      FROM petreport_adoptions a
      LEFT JOIN petreport_feedbacks f ON f.adoption_id = a.id
      WHERE a.status = 'active'
      GROUP BY a.id, a.owner_name, a.pet_name, a.adoption_date, a.image_url, a.required_reports
      HAVING COUNT(f.id) < a.required_reports
      ORDER BY a.adoption_date DESC
      """, nativeQuery = true)
  List<AdoptionView> listNeedAdoptions();

  /** 已完成/無須回報：回報次數 >= 需求次數，或狀態不是 active */
  @Query(value = """
      SELECT
        a.id                         AS id,
        a.owner_name                 AS ownerName,
        a.pet_name                   AS petName,
        a.adoption_date              AS adoptionDate,
        a.image_url                  AS imageUrl,
        a.required_reports           AS requiredReports,
        COUNT(f.id)                  AS reportCount
      FROM petreport_adoptions a
      LEFT JOIN petreport_feedbacks f ON f.adoption_id = a.id
      GROUP BY a.id, a.owner_name, a.pet_name, a.adoption_date, a.image_url, a.required_reports, a.status
      HAVING COUNT(f.id) >= a.required_reports OR a.status <> 'active'
      ORDER BY a.adoption_date DESC
      """, nativeQuery = true)
  List<AdoptionView> listDoneAdoptions();

  /** 單筆明細：adopt-report 左側需要帶 imageUrl/petName/ownerName/adoptionDate */
  @Query(value = """
      SELECT
        a.id                         AS id,
        a.owner_name                 AS ownerName,
        a.pet_name                   AS petName,
        a.adoption_date              AS adoptionDate,
        a.image_url                  AS imageUrl,
        a.required_reports           AS requiredReports,
        COUNT(f.id)                  AS reportCount
      FROM petreport_adoptions a
      LEFT JOIN petreport_feedbacks f ON f.adoption_id = a.id
      WHERE a.id = :id
      GROUP BY a.id, a.owner_name, a.pet_name, a.adoption_date, a.image_url, a.required_reports
      """, nativeQuery = true)
  Optional<AdoptionView> findViewById(@Param("id") Long id);

  // 
  @Query(value = """
      SELECT
        pra.id,
        pra.owner_name      AS ownerName,
        pra.pet_name        AS petName,
        pra.adoption_date   AS adoptionDate,
        pra.status          AS adoptionStatus,
        pra.post_id_ext     AS postIdExt,
        pra.adopter_user_id_ext AS adopterUserIdExt,
        pra.image_url       AS imageUrl
      FROM petreport_adoptions pra
      WHERE pra.adopter_user_id_ext = :uid
        AND pra.status = 'active'
      ORDER BY pra.adoption_date DESC
      """, nativeQuery = true)
  List<AdoptionView> findMyActiveAdoptions(@Param("uid") Long uid);

}
