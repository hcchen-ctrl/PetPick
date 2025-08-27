-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主機： localhost:8889
-- 產生時間： 2025 年 08 月 27 日 03:42
-- 伺服器版本： 8.0.40
-- PHP 版本： 8.3.14

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 資料庫： `petshop`
--

-- --------------------------------------------------------

--
-- 資料表結構 `categories`
--

CREATE TABLE `categories` (
  `category_id` int NOT NULL,
  `name` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- 資料表結構 `orders`
--

CREATE TABLE `orders` (
  `order_id` int UNSIGNED NOT NULL,
  `user_id` bigint NOT NULL,
  `total_price` int NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'Pending',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `shipped_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `addr` varchar(100) DEFAULT NULL,
  `receiver_zip` varchar(10) DEFAULT NULL,
  `receiver_name` varchar(40) DEFAULT NULL,
  `receiver_phone` varchar(20) DEFAULT NULL,
  `shipping_type` varchar(20) DEFAULT NULL,
  `logistics_subtype` varchar(20) DEFAULT NULL,
  `is_collection` tinyint(1) DEFAULT NULL,
  `store_id` varchar(20) DEFAULT NULL,
  `store_name` varchar(60) DEFAULT NULL,
  `store_address` varchar(120) DEFAULT NULL,
  `store_brand` varchar(20) DEFAULT NULL,
  `logistics_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `tracking_no` varchar(30) DEFAULT NULL,
  `logistics_status` varchar(20) DEFAULT NULL,
  `received_at` datetime DEFAULT NULL,
  `trade_no` varchar(50) DEFAULT NULL,
  `payment_gateway` varchar(20) DEFAULT NULL,
  `paid_at` datetime DEFAULT NULL,
  `payment_fail_reason` varchar(200) DEFAULT NULL,
  `merchant_trade_no` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `orders`
--

INSERT INTO `orders` (`order_id`, `user_id`, `total_price`, `status`, `created_at`, `shipped_at`, `delivered_at`, `addr`, `receiver_zip`, `receiver_name`, `receiver_phone`, `shipping_type`, `logistics_subtype`, `is_collection`, `store_id`, `store_name`, `store_address`, `store_brand`, `logistics_id`, `tracking_no`, `logistics_status`, `received_at`, `trade_no`, `payment_gateway`, `paid_at`, `payment_fail_reason`, `merchant_trade_no`) VALUES
(172, 1, 1399, 'PENDING', '2025-08-21 15:58:45', NULL, NULL, '超商取貨付款', NULL, '卓璥志', '0961559559', 'cvs_cod', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(173, 1, 299, 'PENDING', '2025-08-21 21:50:48', NULL, NULL, '超商取貨付款', NULL, '卓璥志', '0961559559', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(174, 1, 299, 'PENDING', '2025-08-21 23:21:27', NULL, NULL, '超商取貨付款', NULL, '卓璥志', '0961559559', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(175, 1, 1977, 'PENDING', '2025-08-22 00:24:07', NULL, NULL, '超商取貨付款', NULL, '卓璥志', '0961559559', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(176, 1, 1889, 'PENDING', '2025-08-22 09:52:02', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Tony Yeh', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(177, 1, 848, 'PENDING', '2025-08-22 11:58:13', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Covid Nineteen', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(178, 1, 299, 'PENDING', '2025-08-22 11:59:29', NULL, NULL, '台中市南屯區公益路二段1號', NULL, '卓璥志', '0961559559', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(179, 1, 1399, 'PENDING', '2025-08-22 14:39:25', NULL, NULL, '超商取貨付款', NULL, '卓璥志', '0961559559', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(180, 1, 199, 'PENDING', '2025-08-22 15:00:30', NULL, NULL, '超商取貨付款', NULL, 'Janet', '0988776655', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(181, 1, 299, 'PENDING', '2025-08-22 15:01:47', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Louis', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(182, 1, 199, 'PENDING', '2025-08-22 15:32:15', NULL, NULL, '台北市信義區松壽路1號', NULL, '測試', '0912345678', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(183, 1, 199, 'Paid', '2025-08-22 15:32:51', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Otis', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP1831755847971630'),
(184, 1, 1279, 'PENDING', '2025-08-22 16:05:44', NULL, NULL, '中山路201巷11弄40號', NULL, 'Even', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(185, 1, 299, 'PENDING', '2025-08-22 16:06:27', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Roy', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(186, 1, 999, 'PENDING', '2025-08-22 16:09:22', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Finn', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(187, 1, 199, 'PENDING', '2025-08-22 16:23:40', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Tony', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(188, 1, 199, 'PENDING', '2025-08-22 16:39:44', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'George', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(189, 1, 1399, 'PENDING', '2025-08-22 16:46:36', NULL, NULL, '台北市信義區松壽路1號', NULL, '測試', '0912345678', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(190, 1, 2798, 'PENDING', '2025-08-22 21:54:35', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'West', '0912121212', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(191, 1, 299, 'PENDING', '2025-08-22 22:01:34', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Vincent', '0967676767', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(192, 1, 1399, 'PENDING', '2025-08-22 22:43:20', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Hank', '0934343434', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(193, 1, 299, 'PENDING', '2025-08-22 23:03:28', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Eric', '0974747474', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(194, 1, 199, 'PENDING', '2025-08-22 23:13:12', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'Zack', '0976767676', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(195, 1, 199, 'PENDING', '2025-08-22 23:31:31', NULL, NULL, '高雄市大社區中山路1號', NULL, 'Ian', '0933838383', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(196, 1, 299, 'PENDING', '2025-08-22 23:33:07', NULL, NULL, '高雄市大社區中山路201巷11弄1號', NULL, 'Quinn Davis', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(197, 1, 1399, 'Shipped', '2025-08-22 23:52:18', NULL, NULL, '高雄市大社區中山路', NULL, 'Peter Lu', '0998776543', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(198, 1, 299, 'Shipped', '2025-08-23 00:01:15', NULL, NULL, '高雄市大社區', NULL, '陳平安', '0977473645', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(199, 1, 999, 'Shipped', '2025-08-23 00:16:03', NULL, NULL, '高雄市大社區', NULL, 'Vincent He', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(200, 1, 1399, 'CANCELLED', '2025-08-23 00:33:07', NULL, NULL, '高雄市大社區', NULL, 'Mike Mai', '0988888888', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(201, 1, 1399, 'PENDING', '2025-08-23 00:34:33', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, '擢景至', '0988888888', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(202, 1, 1399, 'Shipped', '2025-08-23 00:44:01', NULL, NULL, '台北市中正區忠孝西路一段1號', NULL, 'Cam George', '0977777777', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259712', '', 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(203, 1, 7194, 'Shipped', '2025-08-23 14:10:07', NULL, NULL, '高雄市大社區中山路201巷11弄40號', NULL, 'David Chen', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259783', '', 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(204, 1, 2597, 'Paid', '2025-08-23 16:05:14', NULL, NULL, '台中市西屯區台灣大道二段109號', NULL, 'Royce', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2041755936315041'),
(205, 1, 659, 'PENDING', '2025-08-23 20:34:03', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Uris Guan', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2051755952443972'),
(206, 1, 1399, 'PENDING', '2025-08-23 21:35:36', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Otani Long', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259840', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(207, 1, 1399, 'PENDING', '2025-08-23 22:11:01', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Ben Ten', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259844', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(208, 1, 699, 'PENDING', '2025-08-23 22:13:00', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Cedric', '0911223344', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2081755958380472'),
(209, 1, 659, 'PENDING', '2025-08-23 22:29:59', NULL, NULL, '高雄市大社區中山路201巷1號', NULL, 'Jake', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259846', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(210, 1, 1399, 'PENDING', '2025-08-23 22:31:13', NULL, NULL, '高雄市大社區中山路201巷1號', NULL, 'Manis', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2101755959473484'),
(211, 1, 199, 'PENDING', '2025-08-23 22:46:22', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Stanley', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2111755960382790'),
(212, 1, 1399, 'PENDING', '2025-08-23 22:58:01', NULL, NULL, '超商取貨付款', NULL, 'George', '0998877655', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(213, 1, 299, 'PENDING', '2025-08-23 22:59:03', NULL, NULL, '超商取貨付款', NULL, 'Frog', '0988776655', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(214, 1, 199, 'Cancelled', '2025-08-23 23:02:26', NULL, NULL, '高雄市大社區中山路201巷1號', NULL, '擢景至', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2141755961347101'),
(215, 1, 390, 'Cancelled', '2025-08-23 23:30:14', NULL, NULL, '高雄市大社區中山路1弄40號', NULL, 'West', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2151755963014441'),
(216, 1, 1399, 'Cancelled', '2025-08-23 23:45:03', NULL, NULL, '高雄市大社區中山路201巷40號', NULL, 'Greg', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259852', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(217, 1, 1399, 'Shipped', '2025-08-23 23:46:22', NULL, NULL, '高雄市大社區中山路201巷40號', NULL, 'Tony', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259880', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2171755963982807'),
(218, 1, 1399, 'Shipped', '2025-08-23 23:51:58', NULL, NULL, '高雄市大社區中山路201巷4號', NULL, 'yahoo', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259879', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2181755964318198'),
(219, 1, 1399, 'Shipped', '2025-08-24 00:56:22', NULL, NULL, '高雄市大社區中山路1號', NULL, 'benedick', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260000', 'TCAT0011223355', 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2191755968183060'),
(220, 1, 199, 'Shipped', '2025-08-24 01:13:06', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'ghost', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3259875', 'TEST12345678', 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2201755969186826'),
(221, 1, 199, 'Shipped', '2025-08-24 10:40:44', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Hogan', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '221', 'TCAT0011223344', 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2211756003244184'),
(222, 1, 1399, 'Shipped', '2025-08-24 11:34:23', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Navbar', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260021', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2221756006464071'),
(223, 1, 1399, 'CANCELLED', '2025-08-24 14:26:45', NULL, NULL, '超商取貨付款', NULL, 'Jack', '0988776655', 'cvs_cod', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(224, 1, 999, 'Shipped', '2025-08-24 14:28:14', NULL, NULL, '超商取貨付款', NULL, 'Jack', '0988776655', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(225, 1, 2798, 'PENDING', '2025-08-24 21:53:30', NULL, NULL, '高雄市大社區中山路40號', NULL, 'Roger', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260001', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(226, 1, 1399, 'PENDING', '2025-08-24 21:59:45', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Hank', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260002', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(227, 1, 1399, 'PENDING', '2025-08-24 22:07:12', NULL, NULL, '超商取貨付款', NULL, 'Stanley', '0988776655', 'cvs_cod', NULL, NULL, '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(228, 1, 1399, 'Shipped', '2025-08-24 22:09:49', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Vincent', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260005', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2281756044590098'),
(229, 1, 299, 'Shipped', '2025-08-24 22:42:16', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Greg', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260008', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(230, 1, 1399, 'Shipped', '2025-08-24 23:00:43', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Yanis', '0988888888', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260012', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(231, 1, 299, 'PENDING', '2025-08-24 23:12:37', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Uris', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260013', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(232, 1, 1399, 'PENDING', '2025-08-24 23:16:47', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'Hogan', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260014', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(233, 1, 1399, 'Shipped', '2025-08-24 23:18:04', '2025-08-25 09:42:10', NULL, '台中市南屯區公益路二段1號', NULL, 'David Chen', '0988888888', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260085', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, NULL),
(234, 1, 1399, 'Shipped', '2025-08-24 23:24:16', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'George', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260016', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, 'PP2341756049056612'),
(235, 1, 1399, 'Shipped', '2025-08-24 23:30:15', NULL, NULL, '台中市南屯區公益路二段1號', NULL, 'David Chen', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260020', NULL, 'CREATED', NULL, NULL, NULL, NULL, NULL, NULL),
(236, 1, 299, 'Shipped', '2025-08-25 09:23:35', '2025-08-25 10:04:54', NULL, '台中市南屯區公益路二段1號', NULL, 'Jack', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3260095', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, NULL),
(237, 1, 1399, 'Paid', '2025-08-25 14:52:12', '2025-08-25 14:54:06', NULL, '台中市南屯區公益路二段1號', NULL, 'Frank', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3264253', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2371756104732263'),
(238, 1, 199, 'PAID', '2025-08-25 16:39:44', '2025-08-25 16:41:20', NULL, '台中市南屯區公益路二段1號', NULL, 'Tony', '0943764373', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3266341', NULL, 'IN_TRANSIT', NULL, '', 'Manual', '2025-08-25 16:41:30', NULL, 'PP2381756111184219'),
(239, 1, 299, 'Shipped', '2025-08-25 16:42:57', '2025-08-25 16:44:16', NULL, '台中市南屯區公益路二段1號', NULL, 'Zack', '0988787877', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3266343', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2391756111377454'),
(240, 1, 1399, 'Shipped', '2025-08-26 10:20:59', '2025-08-26 10:45:22', NULL, '台中市南屯區公益路二段1號', NULL, 'Roger', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3269581', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2401756174859400'),
(241, 1, 699, 'Shipped', '2025-08-26 10:27:38', '2025-08-26 14:33:07', NULL, '台中市西屯區台灣大道二段109號', NULL, 'Ellis', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3269659', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, NULL),
(242, 1, 699, 'Shipped', '2025-08-26 11:42:54', '2025-08-26 11:44:15', NULL, '台中市南屯區公益路二段1號', NULL, 'Louis', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3269599', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2421756179774053'),
(243, 1, 299, 'Shipped', '2025-08-26 12:10:20', '2025-08-26 14:28:30', NULL, '台中市南屯區公益路二段1號', NULL, 'Robin May', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3269656', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, NULL),
(244, 1, 1398, 'Shipped', '2025-08-26 14:14:55', '2025-08-26 14:31:53', NULL, '台中市南屯區公益路二段1號', NULL, 'Adam', '0988776655', 'address', NULL, NULL, NULL, NULL, NULL, NULL, '3269658', NULL, 'IN_TRANSIT', NULL, NULL, NULL, NULL, NULL, 'PP2441756188895434');

-- --------------------------------------------------------

--
-- 資料表結構 `order_details`
--

CREATE TABLE `order_details` (
  `order_detail_id` int NOT NULL,
  `order_id` int UNSIGNED NOT NULL,
  `product_id` int NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `unit_price` int NOT NULL,
  `subtotal` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `order_details`
--

INSERT INTO `order_details` (`order_detail_id`, `order_id`, `product_id`, `quantity`, `unit_price`, `subtotal`) VALUES
(19, 172, 9, 1, 1399, 1399),
(20, 173, 15, 1, 299, 299),
(21, 174, 15, 1, 299, 299),
(22, 175, 3, 3, 659, 1977),
(23, 176, 9, 1, 1399, 1399),
(24, 176, 19, 1, 390, 390),
(25, 176, 20, 1, 100, 100),
(26, 177, 13, 1, 549, 549),
(27, 177, 15, 1, 299, 299),
(28, 178, 11, 1, 299, 299),
(29, 179, 9, 1, 1399, 1399),
(30, 180, 10, 1, 199, 199),
(31, 181, 11, 1, 299, 299),
(32, 182, 10, 1, 199, 199),
(33, 183, 10, 1, 199, 199),
(34, 184, 14, 1, 699, 699),
(35, 184, 18, 2, 290, 580),
(36, 185, 12, 1, 299, 299),
(37, 186, 8, 1, 999, 999),
(38, 187, 10, 1, 199, 199),
(39, 188, 10, 1, 199, 199),
(40, 189, 9, 1, 1399, 1399),
(41, 190, 9, 2, 1399, 2798),
(42, 191, 11, 1, 299, 299),
(43, 192, 9, 1, 1399, 1399),
(44, 193, 11, 1, 299, 299),
(45, 194, 10, 1, 199, 199),
(46, 195, 10, 1, 199, 199),
(47, 196, 12, 1, 299, 299),
(48, 197, 9, 1, 1399, 1399),
(49, 198, 11, 1, 299, 299),
(50, 199, 8, 1, 999, 999),
(51, 200, 9, 1, 1399, 1399),
(52, 201, 9, 1, 1399, 1399),
(53, 202, 9, 1, 1399, 1399),
(54, 203, 9, 5, 1399, 6995),
(55, 203, 10, 1, 199, 199),
(56, 204, 8, 1, 999, 999),
(57, 204, 10, 1, 199, 199),
(58, 204, 16, 1, 1399, 1399),
(59, 205, 3, 1, 659, 659),
(60, 206, 9, 1, 1399, 1399),
(61, 207, 9, 1, 1399, 1399),
(62, 208, 14, 1, 699, 699),
(63, 209, 3, 1, 659, 659),
(64, 210, 9, 1, 1399, 1399),
(65, 211, 10, 1, 199, 199),
(66, 212, 9, 1, 1399, 1399),
(67, 213, 11, 1, 299, 299),
(68, 214, 10, 1, 199, 199),
(69, 215, 19, 1, 390, 390),
(70, 216, 9, 1, 1399, 1399),
(71, 217, 9, 1, 1399, 1399),
(72, 218, 9, 1, 1399, 1399),
(73, 219, 16, 1, 1399, 1399),
(74, 220, 10, 1, 199, 199),
(75, 221, 10, 1, 199, 199),
(76, 222, 9, 1, 1399, 1399),
(77, 223, 9, 1, 1399, 1399),
(78, 224, 8, 1, 999, 999),
(79, 225, 9, 2, 1399, 2798),
(80, 226, 16, 1, 1399, 1399),
(81, 227, 9, 1, 1399, 1399),
(82, 228, 9, 1, 1399, 1399),
(83, 229, 11, 1, 299, 299),
(84, 230, 9, 1, 1399, 1399),
(85, 231, 12, 1, 299, 299),
(86, 232, 9, 1, 1399, 1399),
(87, 233, 9, 1, 1399, 1399),
(88, 234, 9, 1, 1399, 1399),
(89, 235, 9, 1, 1399, 1399),
(90, 236, 11, 1, 299, 299),
(91, 237, 9, 1, 1399, 1399),
(92, 238, 10, 1, 199, 199),
(93, 239, 11, 1, 299, 299),
(94, 240, 16, 1, 1399, 1399),
(95, 241, 14, 1, 699, 699),
(96, 242, 14, 1, 699, 699),
(97, 243, 11, 1, 299, 299),
(98, 244, 14, 2, 699, 1398);

-- --------------------------------------------------------

--
-- 資料表結構 `order_payments`
--

CREATE TABLE `order_payments` (
  `id` bigint NOT NULL,
  `order_id` int UNSIGNED NOT NULL,
  `gateway` varchar(20) NOT NULL,
  `amount` int NOT NULL,
  `merchant_trade_no` varchar(50) DEFAULT NULL,
  `trade_no` varchar(50) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `paid_at` datetime DEFAULT NULL,
  `fail_reason` varchar(200) DEFAULT NULL,
  `payload_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `order_payments`
--

INSERT INTO `order_payments` (`id`, `order_id`, `gateway`, `amount`, `merchant_trade_no`, `trade_no`, `status`, `paid_at`, `fail_reason`, `payload_json`, `created_at`) VALUES
(2, 238, 'Manual', 199, 'PP2381756111184219', '', 'SUCCESS', '2025-08-25 16:41:30', NULL, NULL, '2025-08-25 16:41:30');

-- --------------------------------------------------------

--
-- 資料表結構 `order_shipments`
--

CREATE TABLE `order_shipments` (
  `id` bigint NOT NULL,
  `order_id` int UNSIGNED NOT NULL,
  `shipping_type` varchar(20) NOT NULL,
  `logistics_subtype` varchar(20) DEFAULT NULL,
  `is_collection` tinyint(1) DEFAULT NULL,
  `receiver_name` varchar(40) DEFAULT NULL,
  `receiver_phone` varchar(20) DEFAULT NULL,
  `receiver_zip` varchar(10) DEFAULT NULL,
  `receiver_addr` varchar(120) DEFAULT NULL,
  `store_id` varchar(20) DEFAULT NULL,
  `store_name` varchar(60) DEFAULT NULL,
  `store_address` varchar(120) DEFAULT NULL,
  `store_brand` varchar(20) DEFAULT NULL,
  `logistics_id` varchar(30) DEFAULT NULL,
  `tracking_no` varchar(30) DEFAULT NULL,
  `status` varchar(20) DEFAULT NULL,
  `shipped_at` datetime DEFAULT NULL,
  `received_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `payload_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `order_shipments`
--

INSERT INTO `order_shipments` (`id`, `order_id`, `shipping_type`, `logistics_subtype`, `is_collection`, `receiver_name`, `receiver_phone`, `receiver_zip`, `receiver_addr`, `store_id`, `store_name`, `store_address`, `store_brand`, `logistics_id`, `tracking_no`, `status`, `shipped_at`, `received_at`, `delivered_at`, `payload_json`, `created_at`) VALUES
(1, 172, 'cvs_cod', NULL, NULL, '卓璥志', '0961559559', NULL, '超商取貨付款', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-21 15:58:45'),
(2, 173, 'cvs_cod', NULL, NULL, '卓璥志', '0961559559', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-21 21:50:48'),
(3, 174, 'cvs_cod', NULL, NULL, '卓璥志', '0961559559', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-21 23:21:27'),
(4, 175, 'cvs_cod', NULL, NULL, '卓璥志', '0961559559', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-22 00:24:07'),
(5, 176, 'address', NULL, NULL, 'Tony Yeh', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-22 09:52:02'),
(8, 177, 'address', NULL, 0, 'Covid Nineteen', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 11:58:13'),
(9, 178, 'address', NULL, 0, '卓璥志', '0961559559', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 11:59:29'),
(10, 179, 'cvs_cod', 'FAMIC2C', 1, '卓璥志', '0961559559', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 14:39:25'),
(11, 180, 'cvs_cod', 'FAMIC2C', 1, 'Janet', '0988776655', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 15:00:30'),
(12, 181, 'address', NULL, 0, 'Louis', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 15:01:47'),
(13, 182, 'address', NULL, 0, '測試', '0912345678', NULL, '台北市信義區松壽路1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 15:32:15'),
(14, 183, 'address', NULL, 0, 'Otis', '0988776655', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 15:32:51'),
(15, 184, 'address', NULL, 0, 'Even', '0988776655', NULL, '中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 16:05:44'),
(16, 185, 'address', NULL, 0, 'Roy', '0988776655', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 16:06:27'),
(17, 186, 'address', NULL, 0, 'Finn', '0988776655', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 16:09:22'),
(18, 187, 'address', NULL, 0, 'Tony', '0988776655', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 16:23:40'),
(19, 188, 'address', NULL, 0, 'George', '0988776655', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 16:39:44'),
(20, 189, 'address', NULL, 0, '測試', '0912345678', NULL, '台北市信義區松壽路1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 16:46:36'),
(21, 190, 'address', NULL, 0, 'West', '0912121212', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 21:54:35'),
(22, 191, 'address', NULL, 0, 'Vincent', '0967676767', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 22:01:34'),
(23, 192, 'address', NULL, 0, 'Hank', '0934343434', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 22:43:20'),
(24, 193, 'address', NULL, 0, 'Eric', '0974747474', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 23:03:28'),
(25, 194, 'address', NULL, 0, 'Zack', '0976767676', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 23:13:12'),
(26, 195, 'address', NULL, 0, 'Ian', '0933838383', NULL, '高雄市大社區中山路1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 23:31:31'),
(27, 196, 'address', NULL, 0, 'Quinn Davis', '0988776655', NULL, '高雄市大社區中山路201巷11弄1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 23:33:07'),
(28, 197, 'address', NULL, 0, 'Peter Lu', '0998776543', NULL, '高雄市大社區中山路', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-22 23:52:18'),
(29, 198, 'address', NULL, 0, '陳平安', '0977473645', NULL, '高雄市大社區', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 00:01:15'),
(30, 199, 'address', NULL, 0, 'Vincent He', '0988776655', NULL, '高雄市大社區', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 00:16:03'),
(31, 200, 'address', NULL, 0, 'Mike Mai', '0988888888', NULL, '高雄市大社區', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 00:33:07'),
(32, 201, 'address', NULL, 0, '擢景至', '0988888888', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 00:34:33'),
(33, 202, 'address', NULL, 0, 'Cam George', '0977777777', NULL, '台北市中正區忠孝西路一段1號', NULL, NULL, NULL, NULL, '3259712', '', 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 00:44:01'),
(34, 203, 'address', NULL, 0, 'David Chen', '0988776655', NULL, '高雄市大社區中山路201巷11弄40號', NULL, NULL, NULL, NULL, '3259783', '', 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 14:10:07'),
(35, 204, 'address', NULL, 0, 'Royce', '0988776655', NULL, '台中市西屯區台灣大道二段109號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 16:05:15'),
(36, 205, 'address', NULL, 0, 'Uris Guan', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 20:34:03'),
(37, 206, 'address', NULL, 0, 'Otani Long', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 21:35:36'),
(38, 207, 'address', NULL, 0, 'Ben Ten', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:11:01'),
(39, 208, 'address', NULL, 0, 'Cedric', '0911223344', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:13:00'),
(40, 209, 'address', NULL, 0, 'Jake', '0988776655', NULL, '高雄市大社區中山路201巷1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:29:59'),
(41, 210, 'address', NULL, 0, 'Manis', '0988776655', NULL, '高雄市大社區中山路201巷1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:31:13'),
(42, 211, 'address', NULL, 0, 'Stanley', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:46:22'),
(43, 212, 'cvs_cod', 'FAMIC2C', 1, 'George', '0998877655', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:58:01'),
(44, 213, 'cvs_cod', 'FAMIC2C', 1, 'Frog', '0988776655', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 22:59:03'),
(45, 214, 'address', NULL, 0, '擢景至', '0988776655', NULL, '高雄市大社區中山路201巷1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 23:02:26'),
(46, 215, 'address', NULL, 0, 'West', '0988776655', NULL, '高雄市大社區中山路1弄40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 23:30:14'),
(47, 216, 'address', NULL, 0, 'Greg', '0988776655', NULL, '高雄市大社區中山路201巷40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 23:45:03'),
(48, 217, 'address', NULL, 0, 'Tony', '0988776655', NULL, '高雄市大社區中山路201巷40號', NULL, NULL, NULL, NULL, '3259880', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 23:46:22'),
(49, 218, 'address', NULL, 0, 'yahoo', '0988776655', NULL, '高雄市大社區中山路201巷4號', NULL, NULL, NULL, NULL, '3259879', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-23 23:51:58'),
(50, 219, 'address', NULL, 0, 'benedick', '0988776655', NULL, '高雄市大社區中山路1號', NULL, NULL, NULL, NULL, '3260000', 'TCAT0011223355', 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 00:56:22'),
(51, 220, 'address', NULL, 0, 'ghost', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3259875', 'TEST12345678', 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 01:13:06'),
(52, 221, 'address', NULL, 0, 'Hogan', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3259934', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 10:40:44'),
(53, 222, 'address', NULL, 0, 'Navbar', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260021', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 11:34:23'),
(54, 223, 'cvs_cod', NULL, 1, 'Jack', '0988776655', NULL, '超商取貨付款', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 14:26:45'),
(55, 224, 'cvs_cod', 'FAMIC2C', 1, 'Jack', '0988776655', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 14:28:14'),
(56, 225, 'address', NULL, 0, 'Roger', '0988776655', NULL, '高雄市大社區中山路40號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 21:53:30'),
(57, 226, 'address', NULL, 0, 'Hank', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-24 21:59:45'),
(58, 227, 'cvs_cod', 'FAMIC2C', 1, 'Stanley', '0988776655', NULL, '超商取貨付款', '006598', '台醫店', '台北市中正區中山南路７號１樓', '全家', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-24 22:07:12'),
(59, 228, 'address', NULL, 0, 'Vincent', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260005', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 22:09:49'),
(60, 229, 'address', NULL, 0, 'Greg', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260008', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 22:42:16'),
(61, 230, 'address', NULL, 0, 'Yanis', '0988888888', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260012', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 23:00:43'),
(62, 231, 'address', NULL, 0, 'Uris', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2025-08-24 23:12:37'),
(63, 232, 'address', NULL, 0, 'Hogan', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, NULL, NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 23:16:47'),
(64, 233, 'address', NULL, 0, 'David Chen', '0988888888', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260085', NULL, 'IN_TRANSIT', '2025-08-25 09:42:10', NULL, NULL, NULL, '2025-08-24 23:18:04'),
(65, 234, 'address', NULL, 0, 'George', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260016', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 23:24:16'),
(66, 235, 'address', NULL, 0, 'David Chen', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260020', NULL, 'CREATED', NULL, NULL, NULL, NULL, '2025-08-24 23:30:15'),
(67, 236, 'address', NULL, 0, 'Jack', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3260095', NULL, 'IN_TRANSIT', '2025-08-25 10:04:54', NULL, NULL, NULL, '2025-08-25 09:23:35'),
(68, 237, 'address', NULL, 0, 'Frank', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3264253', NULL, 'IN_TRANSIT', '2025-08-25 14:54:06', NULL, NULL, NULL, '2025-08-25 14:52:12'),
(69, 238, 'address', NULL, 0, 'Tony', '0943764373', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3266341', NULL, 'IN_TRANSIT', '2025-08-25 16:41:20', NULL, NULL, NULL, '2025-08-25 16:39:44'),
(70, 239, 'address', NULL, 0, 'Zack', '0988787877', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3266343', NULL, 'IN_TRANSIT', '2025-08-25 16:44:16', NULL, NULL, NULL, '2025-08-25 16:42:57'),
(71, 240, 'address', NULL, 0, 'Roger', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3269581', NULL, 'IN_TRANSIT', '2025-08-26 10:45:22', NULL, NULL, NULL, '2025-08-26 10:20:59'),
(72, 241, 'address', NULL, 0, 'Ellis', '0988776655', NULL, '台中市西屯區台灣大道二段109號', NULL, NULL, NULL, NULL, '3269659', NULL, 'IN_TRANSIT', '2025-08-26 14:33:07', NULL, NULL, NULL, '2025-08-26 10:27:38'),
(73, 242, 'address', NULL, 0, 'Louis', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3269599', NULL, 'IN_TRANSIT', '2025-08-26 11:44:15', NULL, NULL, NULL, '2025-08-26 11:42:54'),
(74, 243, 'address', NULL, 0, 'Robin May', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3269656', NULL, 'IN_TRANSIT', '2025-08-26 14:28:30', NULL, NULL, NULL, '2025-08-26 12:10:20'),
(75, 244, 'address', NULL, 0, 'Adam', '0988776655', NULL, '台中市南屯區公益路二段1號', NULL, NULL, NULL, NULL, '3269658', NULL, 'IN_TRANSIT', '2025-08-26 14:31:53', NULL, NULL, NULL, '2025-08-26 14:14:55');

-- --------------------------------------------------------

--
-- 資料表結構 `order_status_history`
--

CREATE TABLE `order_status_history` (
  `id` bigint NOT NULL,
  `order_id` int UNSIGNED NOT NULL,
  `from_status` varchar(20) DEFAULT NULL,
  `to_status` varchar(20) NOT NULL,
  `note` varchar(200) DEFAULT NULL,
  `actor` varchar(40) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `order_status_history`
--

INSERT INTO `order_status_history` (`id`, `order_id`, `from_status`, `to_status`, `note`, `actor`, `created_at`) VALUES
(1, 179, 'PENDING', 'PENDING', 'cvs store selected', 'system', '2025-08-22 14:39:29'),
(2, 180, 'PENDING', 'PENDING', 'cvs store selected', 'system', '2025-08-22 15:00:33'),
(3, 202, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-23 01:07:14'),
(4, 200, 'PENDING', 'CANCELLED', '', 'admin', '2025-08-23 01:10:26'),
(5, 200, 'PENDING', 'CANCELLED', '', 'admin', '2025-08-23 01:10:26'),
(6, 199, 'PENDING', 'Shipped', '', 'admin', '2025-08-23 01:10:44'),
(7, 198, 'PENDING', 'Shipped', '', 'admin', '2025-08-23 01:10:44'),
(8, 197, 'PENDING', 'Shipped', '', 'admin', '2025-08-23 01:10:44'),
(9, 203, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-23 14:10:43'),
(10, 203, 'Shipped', 'Shipped', '', 'admin', '2025-08-23 14:10:46'),
(11, 204, 'PENDING', 'Paid', '', 'admin', '2025-08-23 20:31:53'),
(12, 212, 'PENDING', 'PENDING', 'cvs store selected', 'system', '2025-08-23 22:58:03'),
(13, 213, 'PENDING', 'PENDING', 'cvs store selected', 'system', '2025-08-23 22:59:05'),
(14, 220, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 01:20:39'),
(15, 220, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 01:21:15'),
(16, 219, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 01:56:33'),
(17, 219, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 01:56:44'),
(18, 218, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 01:58:27'),
(19, 217, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 01:58:40'),
(20, 216, 'PENDING', 'Cancelled', '', 'admin', '2025-08-24 02:01:09'),
(21, 215, 'Paid', 'Cancelled', '', 'admin', '2025-08-24 02:01:09'),
(22, 214, 'PENDING', 'Cancelled', '', 'admin', '2025-08-24 02:01:09'),
(23, 221, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 10:42:38'),
(24, 221, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 10:42:47'),
(25, 222, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 11:37:20'),
(26, 222, 'Shipped', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 12:56:41'),
(27, 222, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 12:57:26'),
(28, 220, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 13:01:34'),
(29, 220, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 13:01:59'),
(30, 222, 'Shipped', 'Paid', '', 'admin', '2025-08-24 13:06:46'),
(31, 217, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 13:16:35'),
(32, 219, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 13:17:39'),
(33, 220, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 13:36:15'),
(34, 222, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 13:38:41'),
(35, 222, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 13:38:45'),
(36, 224, 'PENDING', 'PENDING', 'cvs store selected', 'system', '2025-08-24 14:28:17'),
(37, 223, 'PENDING', 'CANCELLED', '', 'admin', '2025-08-24 20:43:35'),
(38, 223, 'PENDING', 'CANCELLED', '', 'admin', '2025-08-24 20:43:35'),
(39, 219, 'Shipped', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 21:48:39'),
(40, 224, 'PENDING', 'Shipped', '', 'admin', '2025-08-24 21:49:13'),
(41, 224, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 21:49:20'),
(42, 224, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 21:58:52'),
(43, 222, 'Shipped', 'Delivered', '', 'admin', '2025-08-24 22:02:35'),
(44, 222, 'Delivered', 'Delivered', '', 'admin', '2025-08-24 22:04:59'),
(45, 227, 'PENDING', 'PENDING', 'cvs store selected', 'system', '2025-08-24 22:07:15'),
(46, 228, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 22:17:05'),
(47, 228, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 22:17:10'),
(48, 229, 'PENDING', 'Shipped', '', 'admin', '2025-08-24 22:44:17'),
(49, 229, 'Shipped', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 22:44:54'),
(50, 230, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 23:05:16'),
(51, 230, 'Shipped', 'Shipped', '', 'admin', '2025-08-24 23:05:20'),
(52, 234, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 23:27:26'),
(53, 235, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 23:49:22'),
(54, 222, 'Delivered', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-24 23:51:09'),
(55, 236, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-25 09:24:48'),
(56, 236, 'Shipped', 'Shipped', '', 'admin', '2025-08-25 09:30:03'),
(57, 233, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-25 09:42:09'),
(58, 236, 'Shipped', 'Shipped', '', 'admin', '2025-08-25 10:03:42'),
(59, 236, 'Shipped', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-25 10:04:54'),
(60, 237, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-25 14:54:06'),
(61, 237, 'Shipped', 'Paid', '', 'admin', '2025-08-25 16:28:49'),
(62, 238, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-25 16:41:19'),
(63, 238, 'Shipped', 'PAID', 'payment ok', 'system', '2025-08-25 16:41:30'),
(65, 239, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-25 16:44:16'),
(66, 240, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-26 10:45:22'),
(69, 242, 'Paid', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-26 11:44:15'),
(70, 243, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-26 14:28:29'),
(71, 244, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-26 14:31:52'),
(72, 241, 'PENDING', 'Shipped', 'Admin 建立綠界宅配', 'admin', '2025-08-26 14:33:07');

-- --------------------------------------------------------

--
-- 資料表結構 `products`
--

CREATE TABLE `products` (
  `product_id` int NOT NULL,
  `pname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `price` int NOT NULL,
  `stock` int NOT NULL DEFAULT '0',
  `published` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1=上架,0=下架',
  `category_id` int DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `type` varchar(100) DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_active` tinyint(1) NOT NULL DEFAULT '1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `products`
--

INSERT INTO `products` (`product_id`, `pname`, `description`, `price`, `stock`, `published`, `category_id`, `image_url`, `created_at`, `type`, `updated_at`, `is_active`) VALUES
(3, '5kg貓咪飼料', '營養均衡，貓咪最愛。', 659, 17, 1, NULL, 'https://i.ibb.co/234vMVPZ/jia-yong-chong-wu-si-liao-de-zu-cheng.jpg', NULL, NULL, '2025-08-23 14:31:06', 1),
(8, '10kg貓咪飼料', '營養均衡，貓咪最愛。', 999, 15, 1, NULL, 'https://i.ibb.co/234vMVPZ/jia-yong-chong-wu-si-liao-de-zu-cheng.jpg', NULL, NULL, '2025-08-24 14:28:14', 1),
(9, '15kg貓咪飼料', '營養均衡，貓咪最愛。', 1399, 15, 1, NULL, 'https://i.ibb.co/234vMVPZ/jia-yong-chong-wu-si-liao-de-zu-cheng.jpg', NULL, 'newest', '2025-08-25 16:28:49', 1),
(10, '狗狗玩具球', '耐咬不破，讓狗狗玩整天！', 199, 47, 1, NULL, 'https://i.ibb.co/23CqQTVN/chao-shi-de-shi-wu-biao-zhi.jpg', NULL, NULL, '2025-08-25 16:41:30', 1),
(11, '寵物洗澡精', '不流淚配方，呵護毛孩肌膚。', 299, 39, 1, NULL, 'https://i.ibb.co/NgqXpyd3/pet-Shampoo.jpg', NULL, NULL, '2025-08-23 22:59:03', 1),
(12, '5kg狗狗飼料', '營養均衡，汪星人最愛。', 299, 30, 1, NULL, 'https://i.ibb.co/4nt9J52H/dogFood.jpg', NULL, NULL, '2025-08-23 14:31:06', 1),
(13, '10kg狗狗飼料', '營養均衡，汪星人最愛。', 549, 10, 1, NULL, 'https://i.ibb.co/4nt9J52H/dogFood.jpg', NULL, NULL, '2025-08-23 14:31:06', 1),
(14, '12kg狗狗飼料', '營養均衡，汪星人最愛。', 699, 20, 1, NULL, 'https://i.ibb.co/4nt9J52H/dogFood.jpg', NULL, 'newest\r\n', '2025-08-23 14:31:06', 1),
(15, '貓抓板', '50cm*30cm*5cm, 抒發喵星人手癢難耐！', 299, 15, 1, NULL, '', NULL, 'popular', '2025-08-23 14:31:06', 1),
(16, '新手組合包（狗）！', '5公斤飼料包、飼料碗＊2、外出繩(請備註大小s、m、L)、睡墊、玩具球。', 1399, 78, 1, NULL, '', NULL, 'popular', '2025-08-23 20:31:53', 1),
(17, '新手組合包（貓）！', '5公斤飼料包、飼料碗＊2、貓砂與盆、貓抓板、貓窩。', 1599, 78, 1, NULL, '', NULL, NULL, '2025-08-23 14:31:06', 1),
(18, '外出繩', '3公尺伸縮牽繩，有「黑、紅、藍」三色，請於備註欄備註。', 290, 25, 1, NULL, 'https://i.ibb.co/KcJtF4GZ/close-up-dog-leash.jpg', NULL, NULL, '2025-08-23 14:31:06', 1),
(19, '貓窩', '喵星人的舒適小窩，有「灰、粉、米」三色，請於備註欄備註', 390, 6, 1, NULL, 'https://i.ibb.co/k6JD9NVw/cute-cat-spending-time-outdoors.jpg', NULL, NULL, '2025-08-24 13:52:16', 0),
(20, '逗貓棒', '每天與愛寵貓貓互動！！', 100, 10, 1, NULL, 'https://i.ibb.co/LXTp273N/Lovepik-com-401692075-funny-cat-stick.png', NULL, NULL, '2025-08-24 13:52:17', 0);

-- --------------------------------------------------------

--
-- 資料表結構 `shipments`
--

CREATE TABLE `shipments` (
  `id` bigint NOT NULL,
  `order_id` int UNSIGNED NOT NULL,
  `type` varchar(20) NOT NULL,
  `sub_type` varchar(30) DEFAULT NULL,
  `is_collection` char(1) DEFAULT NULL,
  `store_id` varchar(20) DEFAULT NULL,
  `store_name` varchar(100) DEFAULT NULL,
  `store_address` varchar(200) DEFAULT NULL,
  `store_tel` varchar(20) DEFAULT NULL,
  `status` varchar(30) DEFAULT NULL,
  `merchant_trade_no` varchar(30) DEFAULT NULL,
  `all_pay_logistics_id` varchar(50) DEFAULT NULL,
  `rrm` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `shipments`
--

INSERT INTO `shipments` (`id`, `order_id`, `type`, `sub_type`, `is_collection`, `store_id`, `store_name`, `store_address`, `store_tel`, `status`, `merchant_trade_no`, `all_pay_logistics_id`, `rrm`, `created_at`, `updated_at`) VALUES
(1, 102, 'CVS', NULL, 'Y', NULL, NULL, NULL, NULL, 'NEW', 'L1755615022745', NULL, NULL, '2025-08-19 22:50:23', '2025-08-19 22:50:23');

-- --------------------------------------------------------

--
-- 資料表結構 `shopping_cart`
--

CREATE TABLE `shopping_cart` (
  `cart_id` int NOT NULL,
  `user_id` bigint NOT NULL,
  `product_id` int NOT NULL,
  `quantity` int NOT NULL DEFAULT '1',
  `added_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `reserved_by_order_id` int DEFAULT NULL,
  `reserved_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- 傾印資料表的資料 `shopping_cart`
--

INSERT INTO `shopping_cart` (`cart_id`, `user_id`, `product_id`, `quantity`, `added_at`, `reserved_by_order_id`, `reserved_at`) VALUES
(11, 2, 14, 10, '2025-08-11 09:42:53', NULL, NULL),
(254, 1, 9, 1, '2025-08-27 10:26:12', NULL, NULL);

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`category_id`);

--
-- 資料表索引 `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`order_id`),
  ADD UNIQUE KEY `trade_no` (`trade_no`),
  ADD UNIQUE KEY `uk_orders_merchant_trade_no` (`merchant_trade_no`),
  ADD UNIQUE KEY `uk_orders_trade_no` (`trade_no`),
  ADD KEY `idx_orders_mtn` (`merchant_trade_no`),
  ADD KEY `idx_orders_user_created` (`user_id`,`created_at` DESC),
  ADD KEY `idx_orders_status_created` (`status`,`created_at` DESC),
  ADD KEY `idx_orders_logistics` (`logistics_id`),
  ADD KEY `idx_orders_user` (`user_id`,`created_at`),
  ADD KEY `idx_orders_status` (`status`,`created_at`),
  ADD KEY `idx_orders_tracking` (`tracking_no`);

--
-- 資料表索引 `order_details`
--
ALTER TABLE `order_details`
  ADD PRIMARY KEY (`order_detail_id`),
  ADD KEY `fk_order_items_order` (`order_id`),
  ADD KEY `fk_order_items_product` (`product_id`);

--
-- 資料表索引 `order_payments`
--
ALTER TABLE `order_payments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_pay_trade_no` (`trade_no`),
  ADD KEY `idx_pay_order` (`order_id`,`created_at`);

--
-- 資料表索引 `order_shipments`
--
ALTER TABLE `order_shipments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_ship_order` (`order_id`,`created_at`),
  ADD KEY `idx_ship_tracking` (`tracking_no`);

--
-- 資料表索引 `order_status_history`
--
ALTER TABLE `order_status_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_osh_order` (`order_id`,`created_at`);

--
-- 資料表索引 `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`product_id`),
  ADD KEY `fk_products_category` (`category_id`);

--
-- 資料表索引 `shipments`
--
ALTER TABLE `shipments`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_ship_order` (`order_id`);

--
-- 資料表索引 `shopping_cart`
--
ALTER TABLE `shopping_cart`
  ADD PRIMARY KEY (`cart_id`),
  ADD UNIQUE KEY `unique_member_product` (`user_id`,`product_id`) USING BTREE,
  ADD KEY `fk_cart_product` (`product_id`),
  ADD KEY `idx_cart_reserved` (`reserved_by_order_id`) USING BTREE;

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `categories`
--
ALTER TABLE `categories`
  MODIFY `category_id` int NOT NULL AUTO_INCREMENT;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `orders`
--
ALTER TABLE `orders`
  MODIFY `order_id` int UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=245;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `order_details`
--
ALTER TABLE `order_details`
  MODIFY `order_detail_id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=99;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `order_payments`
--
ALTER TABLE `order_payments`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `order_shipments`
--
ALTER TABLE `order_shipments`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=76;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `order_status_history`
--
ALTER TABLE `order_status_history`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=73;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `products`
--
ALTER TABLE `products`
  MODIFY `product_id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `shipments`
--
ALTER TABLE `shipments`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `shopping_cart`
--
ALTER TABLE `shopping_cart`
  MODIFY `cart_id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=255;

--
-- 已傾印資料表的限制式
--

--
-- 資料表的限制式 `order_details`
--
ALTER TABLE `order_details`
  ADD CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE RESTRICT;

--
-- 資料表的限制式 `order_payments`
--
ALTER TABLE `order_payments`
  ADD CONSTRAINT `fk_pay_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`);

--
-- 資料表的限制式 `order_shipments`
--
ALTER TABLE `order_shipments`
  ADD CONSTRAINT `fk_ship_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`);

--
-- 資料表的限制式 `order_status_history`
--
ALTER TABLE `order_status_history`
  ADD CONSTRAINT `fk_osh_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`);

--
-- 資料表的限制式 `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE SET NULL;

--
-- 資料表的限制式 `shopping_cart`
--
ALTER TABLE `shopping_cart`
  ADD CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
