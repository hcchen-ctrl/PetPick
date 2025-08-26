-- phpMyAdmin SQL Dump
-- version 5.1.2
-- https://www.phpmyadmin.net/
--
-- 主機： localhost:3306
-- 產生時間： 2025-08-25 08:36:52
-- 伺服器版本： 5.7.24
-- PHP 版本： 8.3.1

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 資料庫: `pet_report`
--

-- --------------------------------------------------------

--
-- 資料表結構 `petreport_adoptions`
--

CREATE TABLE `petreport_adoptions` (
  `id` bigint(20) NOT NULL,
  `owner_name` varchar(100) NOT NULL,
  `pet_name` varchar(100) NOT NULL,
  `adoption_date` date NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `required_reports` int(11) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `post_id_ext` bigint(20) DEFAULT NULL,
  `adopter_user_id_ext` bigint(20) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- 資料表結構 `petreport_feedbacks`
--

CREATE TABLE `petreport_feedbacks` (
  `id` bigint(20) NOT NULL,
  `adoption_id` bigint(20) NOT NULL,
  `report_date` date NOT NULL,
  `report_month` char(7) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `notes` text,
  `status` varchar(20) NOT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `verified_by_employee_id_ext` bigint(20) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `petreport_adoptions`
--
ALTER TABLE `petreport_adoptions`
  ADD PRIMARY KEY (`id`);

--
-- 資料表索引 `petreport_feedbacks`
--
ALTER TABLE `petreport_feedbacks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_prf_adoption_id` (`adoption_id`),
  ADD KEY `idx_prf_report_date` (`report_date`);

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `petreport_adoptions`
--
ALTER TABLE `petreport_adoptions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `petreport_feedbacks`
--
ALTER TABLE `petreport_feedbacks`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `petreport_feedbacks`
--
ALTER TABLE `petreport_feedbacks`
  ADD CONSTRAINT `fk_prf_adoption` FOREIGN KEY (`adoption_id`) REFERENCES `petreport_adoptions` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
