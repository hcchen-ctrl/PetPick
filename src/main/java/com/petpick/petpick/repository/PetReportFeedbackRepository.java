package tw.petpick.petpick.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tw.petpick.petpick.entity.PetReportFeedback;

import java.util.List;

public interface PetReportFeedbackRepository extends JpaRepository<PetReportFeedback, Long> {

    // 依某一筆領養的回報列表（給畫面右側列表用）
    List<PetReportFeedback> findByAdoptionIdOrderByReportDateDesc(Long adoptionId);

    // 🔧 改成 native SQL，避免 Hibernate 在 TEXT 欄位上自動套 upper()/lower() 造成 CLOB 型別錯誤
    //   MySQL 5.7 預設 UTF8 collation 已經大小寫不敏感，不用手動 lower()
    @Query(value = """
            SELECT *
            FROM petreport_feedbacks
            WHERE notes LIKE CONCAT('%', :kw, '%')
            ORDER BY report_date DESC
            """, nativeQuery = true)
    List<PetReportFeedback> searchByNotes(@Param("kw") String keyword);
}
