-- phpMyAdmin SQL Dump
-- version 5.1.2
-- https://www.phpmyadmin.net/
--
-- 主機： localhost:3306
-- 產生時間： 2025-08-21 08:01:12
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
-- 資料庫: `pet_adoption`
--

-- --------------------------------------------------------

--
-- 資料表結構 `abuse_reports`
--

CREATE TABLE `abuse_reports` (
  `id` bigint(20) NOT NULL,
  `post_id` int(10) UNSIGNED NOT NULL,
  `reporter_user_id` int(10) UNSIGNED NOT NULL,
  `reason_code` varchar(50) DEFAULT NULL,
  `note` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 資料表結構 `adopt_applications`
--

CREATE TABLE `adopt_applications` (
  `id` bigint(20) NOT NULL,
  `post_id` int(10) UNSIGNED NOT NULL,
  `applicant_user_id` int(10) UNSIGNED NOT NULL,
  `message` text,
  `status` enum('pending','approved','rejected','cancelled') NOT NULL DEFAULT 'pending',
  `reviewed_by_employee_id` int(10) UNSIGNED DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `approved_at` datetime DEFAULT NULL,
  `rejected_at` datetime DEFAULT NULL,
  `reject_reason` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- 傾印資料表的資料 `adopt_applications`
--

INSERT INTO `adopt_applications` (`id`, `post_id`, `applicant_user_id`, `message`, `status`, `reviewed_by_employee_id`, `created_at`, `updated_at`, `approved_at`, `rejected_at`, `reject_reason`) VALUES
(1, 16, 1, '想領養看看', 'approved', 1, '2025-08-19 14:35:05', '2025-08-19 14:37:27', '2025-08-19 14:37:27', NULL, NULL),
(2, 17, 1, '想玩龜頭', 'approved', 1, '2025-08-19 15:38:30', '2025-08-19 15:59:40', '2025-08-19 15:59:40', NULL, NULL),
(3, 12, 1, '想玩他的蛋', 'approved', 1, '2025-08-19 16:00:58', '2025-08-19 16:03:01', '2025-08-19 16:03:01', NULL, NULL),
(4, 19, 1, '柯文哲的狗', 'approved', 1, '2025-08-19 16:06:00', '2025-08-19 16:39:57', '2025-08-19 16:39:57', NULL, NULL),
(5, 21, 1, '喜歡乖巧不叫的狗 想領養看看', 'approved', 1, '2025-08-20 14:34:44', '2025-08-20 14:36:49', '2025-08-20 14:36:49', NULL, NULL);

-- --------------------------------------------------------

--
-- 資料表結構 `adopt_feedbacks`
--

CREATE TABLE `adopt_feedbacks` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `adopter_user_id` bigint(20) NOT NULL,
  `feedback_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note` text,
  `image_url` varchar(255) DEFAULT NULL,
  `verified_by_employee_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 資料表結構 `adopt_posts`
--

CREATE TABLE `adopt_posts` (
  `id` int(10) UNSIGNED NOT NULL,
  `title` varchar(120) NOT NULL,
  `species` varchar(30) NOT NULL,
  `breed` varchar(60) DEFAULT NULL,
  `sex` enum('male','female','unknown') DEFAULT 'unknown',
  `age` varchar(60) DEFAULT NULL,
  `neutered` enum('yes','no','unknown') NOT NULL DEFAULT 'unknown',
  `body_type` varchar(30) DEFAULT NULL,
  `color` varchar(60) DEFAULT NULL,
  `city` varchar(60) DEFAULT NULL,
  `district` varchar(60) DEFAULT NULL,
  `description` text,
  `image1` varchar(255) DEFAULT NULL,
  `image2` varchar(255) DEFAULT NULL,
  `image3` varchar(255) DEFAULT NULL,
  `source_type` enum('user','platform') NOT NULL,
  `status` enum('pending','approved','on_hold','closed','cancelled','rejected') NOT NULL DEFAULT 'pending',
  `posted_by_user_id` int(10) UNSIGNED DEFAULT NULL,
  `posted_by_employee_id` int(10) UNSIGNED DEFAULT NULL,
  `contact_name` varchar(80) DEFAULT NULL,
  `contact_phone` varchar(40) DEFAULT NULL,
  `contact_method` enum('call_sms','line_only') NOT NULL DEFAULT 'call_sms',
  `contact_line` varchar(80) DEFAULT NULL,
  `adopter_age_limit` enum('any','age20plus','age25plus') NOT NULL DEFAULT 'any',
  `require_home_visit` tinyint(1) NOT NULL DEFAULT '0',
  `require_contract` tinyint(1) NOT NULL DEFAULT '0',
  `require_followup` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- 傾印資料表的資料 `adopt_posts`
--

INSERT INTO `adopt_posts` (`id`, `title`, `species`, `breed`, `sex`, `age`, `neutered`, `body_type`, `color`, `city`, `district`, `description`, `image1`, `image2`, `image3`, `source_type`, `status`, `posted_by_user_id`, `posted_by_employee_id`, `contact_name`, `contact_phone`, `contact_method`, `contact_line`, `adopter_age_limit`, `require_home_visit`, `require_contract`, `require_followup`, `created_at`, `updated_at`) VALUES
(2, '測試民眾送養', '狗', '米克斯', 'male', '成年', 'unknown', '中型', '黑白', '台中市', '北屯區', '這是一隻非常乖巧的狗狗', 'https://example.com/img1.jpg', 'https://example.com/img2.jpg', NULL, 'user', 'closed', 1, NULL, '小王', '0912-345-678', 'call_sms', NULL, 'any', 0, 0, 0, '2025-08-11 15:27:03', '2025-08-18 10:20:14'),
(3, '管理員測試刊登', '狗', '黃金獵犬', 'male', '幼年', 'unknown', '大型', '金色', '台中市', '西區', '這是一隻可愛的黃金獵犬，等待領養', 'https://example.com/img1.jpg', 'https://example.com/img2.jpg', NULL, 'platform', 'closed', NULL, 1, '小李', '0912-345-678', 'call_sms', NULL, 'any', 0, 0, 0, '2025-08-11 15:56:18', '2025-08-18 11:48:56'),
(5, '小扁', '蛇', '青竹絲', 'male', '幼年', 'unknown', '小型', '綠色', '台中市', '西區', '被車輾過', 'https://example.com/img1.jpg', 'https://example.com/img2.jpg', 'https://example.com/img3.jpg', 'platform', 'closed', NULL, 1, '阿罵', '0969269269', 'call_sms', '', 'any', 0, 0, 0, '2025-08-12 16:25:14', '2025-08-18 11:48:41'),
(6, '佩琪', '豬', '豬', 'female', '幼年', 'unknown', '大型', '膚色', '台中市', '北屯區', '佩琪母豬給人騎', 'https://example.com/img1.jpg', 'https://example.com/img2.jpg', 'https://example.com/img3.jpg', 'user', 'closed', 1, NULL, '陳佩琪', '0987487487', 'call_sms', '269269', 'any', 0, 0, 0, '2025-08-12 16:29:04', '2025-08-18 10:20:19'),
(7, '小菊', '貓', '虎斑貓', 'female', '幼年', 'unknown', '中型', '橘色', '台中市', '西屯區', '只會吃', 'https://example.com/img1.jpg', 'https://example.com/img2.jpg', 'https://example.com/img3.jpg', 'user', 'closed', 1, NULL, '菊花', '0900123456', 'call_sms', '123456', 'any', 0, 0, 0, '2025-08-13 09:44:29', '2025-08-18 10:20:22'),
(8, '小白', '狗', '馬爾濟斯', 'male', '幼年', 'no', '小型', '白色', '台中市', '中區', '蠟筆小新的狗', 'https://example.com/img1.jpg', 'https://example.com/img2.jpg', 'https://example.com/img3.jpg', 'user', 'closed', 1, NULL, '小王', '0976875287', 'call_sms', '123456', 'age20plus', 0, 1, 1, '2025-08-13 14:20:43', '2025-08-18 10:20:25'),
(9, '多拉A夢', '貓', '狸貓', 'male', '成年', 'no', '中型', '藍色', '台中市', '南屯區', '大雄的貓', '/uploads/4f77d33d-8b64-4b00-9364-c153b6b9460c.png', '/uploads/d22b3d41-ad7a-40d1-b598-8005a485cd53.jpg', '/uploads/d7763149-12b9-4cc5-bb0c-28ba2fc10605.jpg', 'user', 'approved', 1, NULL, '大雄', '0987487487', 'call_sms', '487487', 'any', 0, 1, 1, '2025-08-13 14:40:19', '2025-08-13 14:41:12'),
(10, '小土', '狗', '混種', 'male', '幼年', 'no', '小型', '咖啡混黑色', '台中市', '南區', '專業吃土', '/uploads/16c4113c-db8b-459e-bcca-1f1d0c4ae6f2.jpg', '/uploads/b1ce41d2-39de-4815-aeb2-fb8f7a9d7bed.jpg', '/uploads/89861457-b739-4cea-8ff0-55cfb58577a1.jpg', 'user', 'closed', 1, NULL, '土哥', '0989489489', 'call_sms', '489489', 'age20plus', 0, 0, 0, '2025-08-13 16:04:49', '2025-08-18 09:30:06'),
(11, '貪啃奇', '狗', '法國鬥牛犬', 'male', '幼年', 'no', '小型', '白色', '台中市', '北區', '很可愛 但沒人愛', '/uploads/01e7e65e-5402-40e5-87cb-1249f2e5ca99.jpg', '/uploads/b3bd55fb-81f4-4fa9-8149-30ce02b33440.jpg', '/uploads/06e66789-e284-41eb-a038-a6c29c2ecf1c.jpg', 'user', 'approved', 1, NULL, '小白', '0927878877', 'call_sms', '789456', 'age20plus', 0, 1, 1, '2025-08-14 10:11:09', '2025-08-14 13:33:56'),
(12, '保力達B', '鼠', '三線鼠', 'unknown', '成年', 'unknown', '小型', '白色', '台東縣', '綠島鄉', '快來認養', '/uploads/44f678e4-8df0-4e38-b864-5c80f83ae71d.jpg', '/uploads/fe4edc9c-71c1-4982-87e9-02bc3323160b.jpg', '/uploads/d6f97ae6-fb5b-4cfc-a6f0-1f502e4cb786.jpg', 'platform', 'closed', NULL, 1, '番仔', '0948267987', 'call_sms', 'da546548', 'age20plus', 0, 0, 0, '2025-08-14 14:22:10', '2025-08-19 16:03:01'),
(13, '小龜', '龜', '象龜', 'unknown', '成年', 'unknown', '小型', '綠色', '新北市', '汐止區', '已養4年多，因個人因素，想給陸龜們更好的飼養環境，故送養', '/uploads/60013746-fa92-4b17-a9e1-252b62c9dc89.jpg', '/uploads/66f8d044-1cd8-42a2-838b-a267a4a4fd6c.jpg', '/uploads/eaa682f8-ea02-4bb8-8c67-b2d3323dd65a.jpg', 'user', 'cancelled', 1, NULL, '龜狗', '0978536987', 'call_sms', '', 'any', 0, 0, 0, '2025-08-15 15:10:43', '2025-08-15 18:25:56'),
(14, '舔狗', '狗', '混種', 'male', '幼年', 'no', '小型', '咖啡混黑色', '台中市', '豐原區', '舔狗舔到最後一無所有', '/uploads/8328ef7a-6485-4ceb-9979-aa8e3b20fa4f.jpg', '/uploads/1f95fdc1-fb84-402e-92f2-e7a2c4846160.jpg', '/uploads/5cab5167-14ec-4931-9b90-422cd7bc7638.jpg', 'platform', 'rejected', NULL, 1, '阿花', '0978278278', 'call_sms', '', 'any', 0, 0, 0, '2025-08-18 09:35:23', '2025-08-18 11:18:21'),
(15, '麻吉', '狗', '馬爾泰迪', 'male', '成年', 'yes', '小型', '白色混淡咖啡色', '新北市', '淡水區', '瑪爾泰迪目前1歲多，出生日期：112/5/10，快滿2歲，我們領養大概4個月，打過各種疫苗，聰明，身體健康，最近剛結紮，有訓練定點大小便，不過偶爾會尿錯地方，可愛活潑非常黏人，親人，不親狗，跑很快跳很高，是一隻很有個性的狗，有時候會低吼，特別是外出回家要幫它擦手腳，他就會低吼，希望是養狗有經驗的人家收養，因為最近家人生病，我要照顧家人，沒空陪伴它，希望能幫它找到好主人！', '/uploads/a4a0dd66-0cd5-496c-83db-0a9209465dba.jpg', '/uploads/442f1951-344a-432e-8d3b-a90a96093c7f.jpg', '/uploads/c740975b-317f-41ff-bb73-a1bb501dd680.jpg', 'user', 'approved', 1, NULL, '吳玫穎', '0922201647', 'call_sms', 'mei268', 'age20plus', 1, 1, 0, '2025-08-18 11:17:46', '2025-08-19 01:39:32'),
(16, '咪咪', '貓', '混種', 'female', '成年', 'no', '小型', '咖啡條紋穿白襪子', '新北市', '八里區', '撿到疑似被遺棄的貓貓\n很黏人，叫了會來很聽話\n因個人無法收編，選擇送養。', '/uploads/418ecbf6-f44e-4789-8bd1-dc7a08ca2b56.jpg', '/uploads/912121c0-dde4-4691-9509-9acc8403b4a2.jpg', '/uploads/60e212db-374b-4427-9559-3778a2443a6e.jpg', 'platform', 'closed', NULL, 1, '雨痕', '0979347989', 'call_sms', 'k5200301', 'age20plus', 1, 0, 1, '2025-08-18 11:43:28', '2025-08-19 16:00:07'),
(17, '小龜', '龜', '象龜', 'unknown', '成年', 'unknown', '小型', '綠色', '新北市', '五股區', '龜狗的小龜', '/uploads/732547f8-4463-408e-8dfa-46f68a1c19da.jpg', '/uploads/e52379f0-2000-4d50-a7fe-ed823965b3da.jpg', '/uploads/b3a85bc1-3804-4f98-8c97-0613d659dc45.jpg', 'platform', 'closed', NULL, 1, '龜狗', '0987789879', 'call_sms', '', 'any', 0, 0, 0, '2025-08-18 16:19:20', '2025-08-19 15:59:39'),
(18, 'Nike', '狗', '比熊', 'male', '成年', 'no', '中型', '白色', '桃園市', '大溪區', '非常黏人、須有時間陪伴。', '/uploads/165f0032-ca7f-4cb2-b0a1-f5e9e1ff71bb.jpg', '/uploads/ad9e2b47-0643-45c2-9ea7-96acaeb470b8.jpg', '/uploads/ee843159-b3b4-4188-8a0e-4660f09c9e5c.jpg', 'user', 'approved', 1, NULL, '萱萱', '0915729321', 'call_sms', 'Lisa729', 'age20plus', 1, 0, 1, '2025-08-19 00:39:57', '2025-08-19 01:39:29'),
(19, '畜生', '狗', '公狗', 'male', '老年', 'no', '大型', '膚色', '台南市', '中西區', '我是總統', '/uploads/f2aca0d5-0914-42dd-9d07-c1ea0c519d55.jpg', '', '', 'platform', 'closed', NULL, 1, '柯文哲', '0987487487', 'call_sms', '78778', 'any', 0, 0, 0, '2025-08-19 16:04:57', '2025-08-19 16:39:57'),
(20, '米糕', '鼠', '黃金鼠', 'female', '成年', 'no', '小型', '白色', '台中市', '西區', '較膽小，易受驚嚇，受驚嚇時偶爾會偷咬人（輕咬）。', '/uploads/eed5882e-3475-4dcd-924c-4bbd6c36099e.jpg', '/uploads/1dea7ce0-6f87-479e-b3ba-129315112e42.jpg', '', 'user', 'approved', 1, NULL, 'J', '0971891890', 'call_sms', '', 'any', 1, 0, 1, '2025-08-20 14:28:50', '2025-08-20 14:29:20'),
(21, '小黑', '狗', '混種', 'female', '老年', 'yes', '中型', '黑色', '新北市', '三峽區', '米克斯，姓名小黑，年記約9歲，不常叫，已結紮，約9歲，中型犬', '/uploads/50352c8c-dd67-4a70-89a2-153a08b704e3.jpg', '/uploads/9ba6aed7-d644-4ab1-85db-b1c1be1585f6.jpg', '/uploads/7df43af0-c58e-4cf8-bf78-3bbf7b6e4aad.jpg', 'platform', 'closed', NULL, 1, '曹小姐', '0902252910', 'call_sms', 'caolishih', 'any', 0, 0, 0, '2025-08-20 14:31:50', '2025-08-20 14:36:49');

-- --------------------------------------------------------

--
-- 資料表結構 `contact_view_logs`
--

CREATE TABLE `contact_view_logs` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `viewer_user_id` bigint(20) NOT NULL,
  `viewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- 資料表結構 `employees`
--

CREATE TABLE `employees` (
  `employee_id` int(10) UNSIGNED NOT NULL,
  `employee_number` varchar(100) NOT NULL,
  `name` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `isresign` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- 傾印資料表的資料 `employees`
--

INSERT INTO `employees` (`employee_id`, `employee_number`, `name`, `password`, `isresign`) VALUES
(1, 'A001', '測試管理員', '123456', 'N');

-- --------------------------------------------------------

--
-- 資料表結構 `post_reviews`
--

CREATE TABLE `post_reviews` (
  `id` bigint(20) NOT NULL,
  `post_id` int(10) UNSIGNED NOT NULL,
  `action` enum('approve','reject') NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `reviewer_employee_id` int(10) UNSIGNED NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- 傾印資料表的資料 `post_reviews`
--

INSERT INTO `post_reviews` (`id`, `post_id`, `action`, `reason`, `reviewer_employee_id`, `created_at`) VALUES
(1, 14, 'reject', '', 1, '2025-08-18 11:18:21'),
(2, 15, 'approve', '', 1, '2025-08-18 11:19:24'),
(3, 18, 'approve', '', 1, '2025-08-19 00:40:20'),
(4, 20, 'approve', '', 1, '2025-08-20 14:29:20');

-- --------------------------------------------------------

--
-- 資料表結構 `userinfo`
--

CREATE TABLE `userinfo` (
  `user_id` int(10) UNSIGNED NOT NULL,
  `phonenumber` varchar(50) DEFAULT NULL,
  `icon` blob COMMENT '頭像',
  `username` varchar(100) NOT NULL,
  `password` varchar(100) NOT NULL,
  `gender` varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '性别(M:男生;F:女生)',
  `accountemail` varchar(100) NOT NULL COMMENT '帳號',
  `city` varchar(100) DEFAULT NULL COMMENT '居住城市',
  `district` varchar(100) DEFAULT NULL COMMENT '居住區域',
  `experience` varchar(100) DEFAULT NULL COMMENT '飼養經驗(N:無;Y:有)',
  `daily` varchar(1000) DEFAULT NULL COMMENT '飼主生活作息',
  `activities` varchar(1000) DEFAULT NULL COMMENT '飼主日常活動',
  `pet` varchar(1000) DEFAULT NULL COMMENT '飼養偏好',
  `pet_activities` varchar(1000) DEFAULT NULL COMMENT '寵物個性',
  `isaccount` varchar(100) DEFAULT NULL COMMENT '帳號是否刪除',
  `isblacklist` varchar(100) DEFAULT NULL COMMENT '是否為黑名單'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- 傾印資料表的資料 `userinfo`
--

INSERT INTO `userinfo` (`user_id`, `phonenumber`, `icon`, `username`, `password`, `gender`, `accountemail`, `city`, `district`, `experience`, `daily`, `activities`, `pet`, `pet_activities`, `isaccount`, `isblacklist`) VALUES
(1, NULL, NULL, '測試會員', '123456', NULL, 'user@test.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `abuse_reports`
--
ALTER TABLE `abuse_reports`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_abuse_once` (`post_id`,`reporter_user_id`),
  ADD KEY `idx_abuse_post` (`post_id`),
  ADD KEY `fk_abuse_user` (`reporter_user_id`);

--
-- 資料表索引 `adopt_applications`
--
ALTER TABLE `adopt_applications`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_applicant_post` (`post_id`,`applicant_user_id`),
  ADD KEY `idx_applicant` (`applicant_user_id`),
  ADD KEY `idx_app_status` (`status`),
  ADD KEY `idx_app_post` (`post_id`),
  ADD KEY `fk_app_employee` (`reviewed_by_employee_id`);

--
-- 資料表索引 `adopt_feedbacks`
--
ALTER TABLE `adopt_feedbacks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_fb_post` (`post_id`),
  ADD KEY `idx_fb_adopter` (`adopter_user_id`),
  ADD KEY `idx_fb_date` (`feedback_date`);

--
-- 資料表索引 `adopt_posts`
--
ALTER TABLE `adopt_posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_posts_status` (`status`),
  ADD KEY `idx_posts_source` (`source_type`,`status`),
  ADD KEY `idx_posts_city` (`city`,`district`),
  ADD KEY `idx_posts_owner_user` (`posted_by_user_id`),
  ADD KEY `idx_posts_owner_emp` (`posted_by_employee_id`),
  ADD KEY `idx_posts_user` (`posted_by_user_id`),
  ADD KEY `idx_posts_employee` (`posted_by_employee_id`);

--
-- 資料表索引 `contact_view_logs`
--
ALTER TABLE `contact_view_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_view_post_user` (`post_id`,`viewer_user_id`),
  ADD KEY `idx_view_time` (`viewed_at`);

--
-- 資料表索引 `employees`
--
ALTER TABLE `employees`
  ADD PRIMARY KEY (`employee_id`);

--
-- 資料表索引 `post_reviews`
--
ALTER TABLE `post_reviews`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_review_post` (`post_id`),
  ADD KEY `fk_review_emp` (`reviewer_employee_id`);

--
-- 資料表索引 `userinfo`
--
ALTER TABLE `userinfo`
  ADD PRIMARY KEY (`user_id`);

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `abuse_reports`
--
ALTER TABLE `abuse_reports`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `adopt_applications`
--
ALTER TABLE `adopt_applications`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `adopt_feedbacks`
--
ALTER TABLE `adopt_feedbacks`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `adopt_posts`
--
ALTER TABLE `adopt_posts`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=22;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `contact_view_logs`
--
ALTER TABLE `contact_view_logs`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `employees`
--
ALTER TABLE `employees`
  MODIFY `employee_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `post_reviews`
--
ALTER TABLE `post_reviews`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `abuse_reports`
--
ALTER TABLE `abuse_reports`
  ADD CONSTRAINT `fk_abuse_post` FOREIGN KEY (`post_id`) REFERENCES `adopt_posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_abuse_user` FOREIGN KEY (`reporter_user_id`) REFERENCES `userinfo` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- 資料表的限制式 `adopt_applications`
--
ALTER TABLE `adopt_applications`
  ADD CONSTRAINT `fk_app_employee` FOREIGN KEY (`reviewed_by_employee_id`) REFERENCES `employees` (`employee_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_app_post` FOREIGN KEY (`post_id`) REFERENCES `adopt_posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_app_user` FOREIGN KEY (`applicant_user_id`) REFERENCES `userinfo` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- 資料表的限制式 `adopt_posts`
--
ALTER TABLE `adopt_posts`
  ADD CONSTRAINT `fk_adopt_posts_employee` FOREIGN KEY (`posted_by_employee_id`) REFERENCES `employees` (`employee_id`),
  ADD CONSTRAINT `fk_adopt_posts_user` FOREIGN KEY (`posted_by_user_id`) REFERENCES `userinfo` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- 資料表的限制式 `post_reviews`
--
ALTER TABLE `post_reviews`
  ADD CONSTRAINT `fk_review_emp` FOREIGN KEY (`reviewer_employee_id`) REFERENCES `employees` (`employee_id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_review_post` FOREIGN KEY (`post_id`) REFERENCES `adopt_posts` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
