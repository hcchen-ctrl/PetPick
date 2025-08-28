package tw.petpick.petpick.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class PetReportSyncJob {

    private final JdbcTemplate jdbc;

    public PetReportSyncJob(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 每 10 分鐘同步一次（自行調整 cron） */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void sync() {
        // 1) 新增名單（核准的申請 → petreport_adoptions），帶入 image_url
        //    - 避免重複：post_id_ext + adopter_user_id_ext 做 NOT EXISTS
        //    - 圖片來源：adopt_posts.image1 → image2 → image3
        String insertSql = """
            INSERT INTO petreport_adoptions
                (owner_name, pet_name, adoption_date, required_reports, status,
                 adopter_user_id_ext, post_id_ext, image_url, created_at, updated_at)
            SELECT
                u.username AS owner_name,
                p.title    AS pet_name,
                DATE(COALESCE(a.approved_at, p.updated_at, p.created_at)) AS adoption_date,
                12 AS required_reports,
                'active' AS status,
                a.applicant_user_id AS adopter_user_id_ext,
                p.id AS post_id_ext,
                COALESCE(p.image1, p.image2, p.image3) AS image_url,
                NOW(), NOW()
            FROM adopt_applications a
            JOIN adopt_posts p ON p.id = a.post_id
            JOIN userinfo u    ON u.user_id = a.applicant_user_id
            WHERE a.status = 'approved'
              AND NOT EXISTS (
                  SELECT 1
                  FROM petreport_adoptions x
                  WHERE x.post_id_ext = p.id
                    AND x.adopter_user_id_ext = a.applicant_user_id
              );
        """;
        jdbc.update(insertSql);

        // 2) 回填：已存在名單但 image_url 為 NULL 的，補上
        String backfillSql = """
            UPDATE petreport_adoptions a
            JOIN adopt_posts p ON p.id = a.post_id_ext
            SET a.image_url = COALESCE(p.image1, p.image2, p.image3),
                a.updated_at = NOW()
            WHERE a.image_url IS NULL OR a.image_url = '';
        """;
        jdbc.update(backfillSql);

        // 3) 同步變動（可選）：若 adopt_posts 的圖片已更新，這邊也跟著更新
        //    只在不相等時才更新，避免不必要寫入
        String refreshIfChangedSql = """
            UPDATE petreport_adoptions a
            JOIN adopt_posts p ON p.id = a.post_id_ext
            SET a.image_url = COALESCE(p.image1, p.image2, p.image3),
                a.updated_at = NOW()
            WHERE COALESCE(a.image_url, '') <> COALESCE(COALESCE(p.image1, p.image2, p.image3), '');
        """;
        jdbc.update(refreshIfChangedSql);
    }
}
