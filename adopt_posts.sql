-- phpMyAdmin SQL Dump
-- version 5.1.2
-- https://www.phpmyadmin.net/
--
-- 主機： localhost:3306
-- 產生時間： 2025-08-26 06:39:41
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
-- 資料庫: `petpick`
--

-- --------------------------------------------------------

--
-- 資料表結構 `adopt_posts`
--

CREATE TABLE `adopt_posts` (
  `id` bigint(20) NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `species` varchar(255) DEFAULT NULL,
  `breed` varchar(255) DEFAULT NULL,
  `sex` varchar(255) DEFAULT NULL,
  `age` varchar(255) DEFAULT NULL,
  `neutered` enum('yes','no','unknown') NOT NULL DEFAULT 'unknown',
  `body_type` varchar(255) DEFAULT NULL,
  `color` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `description` text,
  `image1` varchar(255) DEFAULT NULL,
  `image2` varchar(255) DEFAULT NULL,
  `image3` varchar(255) DEFAULT NULL,
  `source_type` enum('user','platform') NOT NULL,
  `status` enum('pending','approved','on_hold','closed','cancelled','rejected') NOT NULL DEFAULT 'pending',
  `posted_by_user_id` bigint(20) DEFAULT NULL,
  `posted_by_employee_id` bigint(20) DEFAULT NULL,
  `contact_name` varchar(255) DEFAULT NULL,
  `contact_phone` varchar(255) DEFAULT NULL,
  `contact_method` enum('call_sms','line_only') NOT NULL DEFAULT 'call_sms',
  `contact_line` varchar(255) DEFAULT NULL,
  `adopter_age_limit` enum('any','age20plus','age25plus') NOT NULL DEFAULT 'any',
  `require_home_visit` tinyint(1) NOT NULL DEFAULT '0',
  `require_contract` tinyint(1) NOT NULL DEFAULT '0',
  `require_followup` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
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
(17, '小龜', '龜', '象龜', 'unknown', '成年', 'unknown', '小型', '綠色', '新北市', '五股區', '龜狗的小龜', '/uploads/732547f8-4463-408e-8dfa-46f68a1c19da.jpg', '/uploads/e52379f0-2000-4d50-a7fe-ed823965b3da.jpg', '/uploads/b3a85bc1-3804-4f98-8c97-0613d659dc45.jpg', 'platform', 'approved', NULL, 1, '龜狗', '0987789879', 'call_sms', '', 'any', 0, 0, 0, '2025-08-18 16:19:20', '2025-08-24 10:54:45'),
(18, 'Nike', '狗', '比熊', 'male', '成年', 'no', '中型', '白色', '桃園市', '大溪區', '非常黏人、須有時間陪伴。', '/uploads/165f0032-ca7f-4cb2-b0a1-f5e9e1ff71bb.jpg', '/uploads/ad9e2b47-0643-45c2-9ea7-96acaeb470b8.jpg', '/uploads/ee843159-b3b4-4188-8a0e-4660f09c9e5c.jpg', 'user', 'approved', 1, NULL, '萱萱', '0915729321', 'call_sms', 'Lisa729', 'age20plus', 1, 0, 1, '2025-08-19 00:39:57', '2025-08-19 01:39:29'),
(19, '畜生', '狗', '公狗', 'male', '老年', 'no', '大型', '膚色', '台南市', '中西區', '我是總統', '/uploads/f2aca0d5-0914-42dd-9d07-c1ea0c519d55.jpg', '', '', 'platform', 'closed', NULL, 1, '柯文哲', '0987487487', 'call_sms', '78778', 'any', 0, 0, 0, '2025-08-19 16:04:57', '2025-08-19 16:39:57'),
(20, '米糕', '鼠', '黃金鼠', 'female', '成年', 'no', '小型', '白色', '台中市', '西區', '較膽小，易受驚嚇，受驚嚇時偶爾會偷咬人（輕咬）。', '/uploads/eed5882e-3475-4dcd-924c-4bbd6c36099e.jpg', '/uploads/1dea7ce0-6f87-479e-b3ba-129315112e42.jpg', '', 'user', 'approved', 1, NULL, 'J', '0971891890', 'call_sms', '', 'any', 1, 0, 1, '2025-08-20 14:28:50', '2025-08-20 14:29:20'),
(21, '小黑', '狗', '混種', 'female', '老年', 'yes', '中型', '黑色', '新北市', '三峽區', '米克斯，姓名小黑，年記約9歲，不常叫，已結紮，約9歲，中型犬', '/uploads/50352c8c-dd67-4a70-89a2-153a08b704e3.jpg', '/uploads/9ba6aed7-d644-4ab1-85db-b1c1be1585f6.jpg', '/uploads/7df43af0-c58e-4cf8-bf78-3bbf7b6e4aad.jpg', 'platform', 'closed', NULL, 1, '曹小姐', '0902252910', 'call_sms', 'caolishih', 'any', 0, 0, 0, '2025-08-20 14:31:50', '2025-08-20 14:36:49'),
(22, '喵喵', '貓', '喵喵', 'male', '幼年', 'unknown', '中型', '', '苗栗縣', '通霄鎮', '喵喵', '', '', '', 'user', 'pending', 15, NULL, '喵喵', '0123456789', 'call_sms', '', 'any', 0, 0, 0, '2025-08-23 16:09:16', '2025-08-23 16:09:16'),
(23, 'au ll', '狗', '小花喵', 'female', '成年', 'no', '中型', '', '台中市', '南屯區', '2\n319489', '', '', '', 'user', 'pending', 15, NULL, '123456798', '1234567892', 'call_sms', '', 'any', 0, 0, 0, '2025-08-23 16:14:07', '2025-08-23 16:14:07'),
(24, '安安', '狗', '啦阿', 'female', '幼年', 'no', '小型', '', '桃園市', '平鎮區', '烏拉壓哈', '', '', '', 'platform', 'closed', 13, NULL, '巫薩其', '0123456789', 'call_sms', '', 'any', 0, 0, 0, '2025-08-23 16:19:11', '2025-08-24 23:08:55'),
(25, '123', '鳥', '123', 'male', '幼年', 'no', '小型', '', '新北市', '汐止區', '12349', '', '', '', 'user', 'approved', 13, NULL, '小巴喵', '1234567899', 'call_sms', '', 'any', 0, 0, 0, '2025-08-23 16:28:13', '2025-08-24 14:57:46'),
(26, '烏薩奇', '兔', '烏薩奇', 'female', '幼年', 'yes', '中型', '', '苗栗縣', '通霄鎮', '烏薩奇94烏薩奇', '', '', '', 'platform', 'approved', NULL, 15, '烏薩奇', '0123456789', 'call_sms', '', 'any', 0, 0, 0, '2025-08-25 11:28:48', '2025-08-25 11:28:48'),
(27, '小桃', '貓', '小桃', 'male', '幼年', 'yes', '中型', '', '彰化縣', '員林市', '小桃94小桃', '', '', '', 'user', 'pending', 13, NULL, '小桃', '0123654789+', 'call_sms', '', 'any', 0, 0, 0, '2025-08-25 11:39:21', '2025-08-25 11:39:21'),
(28, '海獺勇者', '鳥', '海獺勇者', 'male', '幼年', 'yes', '中型', '', '彰化縣', '員林市', '海獺勇者', '', '', '', 'user', 'pending', 13, NULL, '海獺勇者', '123654789', 'call_sms', '', 'any', 0, 0, 0, '2025-08-25 11:49:20', '2025-08-25 11:49:20'),
(29, '小八小八', '貓', '小八', 'female', '老年', 'no', '中型', '', '台中市', '南屯區', '小八小八', '', '', '', 'platform', 'approved', NULL, 15, '小八小八', '0123654789', 'call_sms', '', 'any', 0, 0, 0, '2025-08-25 21:04:28', '2025-08-25 21:04:28'),
(30, '鴨鴨', '貓', '小花喵', 'male', '幼年', 'unknown', '小型', '', '桃園市', '觀音區', '鴨鴨鴨鴨鴨', '', '', '', 'platform', 'approved', NULL, 15, '鴨鴨', '0123654785', 'call_sms', '', 'any', 0, 0, 0, '2025-08-25 21:12:07', '2025-08-25 21:12:07'),
(31, 'bobo', '狗', 'bobo', 'male', '幼年', 'yes', '小型', '', '桃園市', '龜山區', 'bobo94bobo', '', '', '', 'user', 'cancelled', 16, NULL, 'bobo', '0123654789', 'call_sms', '', 'any', 0, 0, 0, '2025-08-25 21:35:41', '2025-08-25 21:35:54');

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `adopt_posts`
--
ALTER TABLE `adopt_posts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_adopt_posts_user` (`posted_by_user_id`);

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `adopt_posts`
--
ALTER TABLE `adopt_posts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=32;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `adopt_posts`
--
ALTER TABLE `adopt_posts`
  ADD CONSTRAINT `fk_adopt_posts_user` FOREIGN KEY (`posted_by_user_id`) REFERENCES `userinfo` (`userid`) ON DELETE SET NULL ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
