-- phpMyAdmin SQL Dump
-- version 5.1.2
-- https://www.phpmyadmin.net/
--
-- 主機： localhost:3306
-- 產生時間： 2025-08-20 07:17:11
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
-- 資料表結構 `userinfo`
--

CREATE TABLE `userinfo` (
  `userid` bigint(20) NOT NULL,
  `phonenumber` varchar(255) DEFAULT NULL,
  `icon` blob COMMENT '頭像',
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `gender` varchar(255) DEFAULT NULL,
  `accountemail` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `experience` varchar(255) DEFAULT NULL,
  `daily` varchar(255) DEFAULT NULL,
  `activities` varchar(255) DEFAULT NULL,
  `pet` varchar(255) DEFAULT NULL,
  `pet_activities` varchar(255) DEFAULT NULL,
  `isaccount` varchar(255) DEFAULT NULL,
  `isblacklist` varchar(255) DEFAULT NULL,
  `email_verification_code` varchar(255) DEFAULT NULL,
  `email_verified` tinyint(1) DEFAULT '0',
  `authority` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- 傾印資料表的資料 `userinfo`
--

INSERT INTO `userinfo` (`userid`, `phonenumber`, `icon`, `username`, `password`, `gender`, `accountemail`, `city`, `district`, `experience`, `daily`, `activities`, `pet`, `pet_activities`, `isaccount`, `isblacklist`, `email_verification_code`, `email_verified`, `authority`, `role`) VALUES
(1, NULL, NULL, 'testuser', '$2a$10$KytjrTyzhDUYOv5jsZVUJe5yTr3b9iHePdRPsXwmG1LZtWqXlelzC', NULL, 'testuser@gmail', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(2, '0912365478', NULL, 'testusertwo', '$2a$10$SbIoId6Zm.Ixtxv9Z85/he.YbOUW7bWXsN6B4OgD7mzs3lZiSCCqq', 'F', 'testusertwo@gmail', 'taipei', 'dt', 'y', '早睡早起', '戶外活動', 'cat', '好動', NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(3, '0998652147', NULL, 'testuserthree', '$2a$10$gwsSSTuP81yQkCIL.h0wwOaMnYwDc2WTlt3MzrCSUF88Z1/Mzgxx2', 'M', 'testuserthree@gmail', 'taipei', 'SI', 'N', '晚睡晚起', '宅在家', 'cat', '安靜', NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(4, '0123654789', NULL, '陳圓圓', '$2a$10$OlG48zcGgGKFywwAUdwi5O9BZ5Lh8OJzx2uiqLiocAVkoyTtDVJqW', NULL, 'testfour@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(5, '0123654789', NULL, '黃圓圓', '$2a$10$Iu9FeKf4VhC4YFvpXNMC9eYCzkVzvlCnHoa9vsRporQxvH9cW6STq', NULL, 'testfive@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(6, '0123654789', NULL, '李圓圓', '$2a$10$r8FY5Lz3MVMEUMU3VEAoXOlztJky/IlkEtbHnbjnk29/OeOm7L3/S', NULL, 'testsix@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(7, '0123654789', NULL, '林圓圓', '$2a$10$qCQnldKh.tX1nemtqC9H6ue1DBFeVQ3W.XHqf3Iwy7230nB7nnPQ2', NULL, 'testseven@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(8, '0123654789', NULL, '吳圓圓', '$2a$10$LvLHMwSMQslZr.6i4/OM3.o45j92rA/Yu2TBjPEXm90g2Ik7IY5li', NULL, 'testeight@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(9, '0123654789', NULL, '吳圓圓', '$2a$10$r3LJo5yNJ.g5GghfC83Zv.1nM5ZiNPS9euv0EtiMaveG19h/yXOj2', NULL, 'testfour1@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '6252a1', 0, 'ROLE_CUSTOMER', 'ROLE_CUSTOMER'),
(10, '0123654789', NULL, '邱源源', '{bcrypt}$2a$10$CtgShlyPXXzCZCVUlCbJiO0A2myk5Jj9lniy94loVd.ZSWGBlmqka', '女', 'testnine@gmail.com', '桃園市', '蘆竹區', '有', '正常作息（7:00-9:00起床）', NULL, '貓', '活潑', '1', '0', NULL, 0, NULL, 'MANAGER'),
(11, '0123654789', NULL, '邱源', '{bcrypt}$2a$10$9gS2XOpFRK8xETm/duJRvuJL.TFHkXOZCR6HEmQ/cEC3WMS9jhOj2', NULL, 'testten@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '1', '0', NULL, 0, NULL, 'MANAGER'),
(12, '0123654789', NULL, '陳圓', '{bcrypt}$2a$10$9RV7bDj8AxtX9PJmF3Bbz.jHJYE9XdqvSUuyFLffEF/tT/9LFYqCm', NULL, 'test11@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '1', '0', NULL, 0, NULL, 'MANAGER'),
(13, '0123654789', NULL, '秋葉原', '{bcrypt}$2a$10$WC4xzNbP7XNlaOyrynFOTuah08B9BB/42LSd2WvcW0VMmiM7Xcx7C', '女', 'test12@gmail.com', '', '', '有', '夜貓子（經常熬夜）', NULL, '狗,貓', '活潑,安靜,怕生', '1', '0', NULL, 0, NULL, 'USER'),
(14, NULL, NULL, '陳虹妗', NULL, NULL, 'hc.chen24@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, 'USER'),
(15, '0123654789', NULL, '吳小寶', '{bcrypt}$2a$10$0d2UODPTqhbjKAfvzuqxFO0XeNqnKxSEaBZeykU59aDzJuisJPcs2', NULL, 'test13@gmail.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '1', '0', NULL, 0, NULL, 'USER');

--
-- 已傾印資料表的索引
--

--
-- 資料表索引 `userinfo`
--
ALTER TABLE `userinfo`
  ADD PRIMARY KEY (`userid`);

--
-- 在傾印的資料表使用自動遞增(AUTO_INCREMENT)
--

--
-- 使用資料表自動遞增(AUTO_INCREMENT) `userinfo`
--
ALTER TABLE `userinfo`
  MODIFY `userid` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
