CREATE DATABASE IF NOT EXISTS library_seat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_seat;
DROP TABLE IF EXISTS violation, notice, reservation, seat, reading_room, library, sys_user;

CREATE TABLE sys_user (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(50) NOT NULL UNIQUE,
 password VARCHAR(255) NOT NULL, student_no VARCHAR(50) UNIQUE, name VARCHAR(50) NOT NULL,
 phone VARCHAR(20), role VARCHAR(20) DEFAULT 'STUDENT', status TINYINT DEFAULT 1,
 created_time DATETIME DEFAULT CURRENT_TIMESTAMP, updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE library (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, address VARCHAR(255),
 description VARCHAR(500), status TINYINT DEFAULT 1, created_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE reading_room (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, library_id BIGINT NOT NULL, name VARCHAR(100) NOT NULL,
 floor VARCHAR(20), capacity INT DEFAULT 0, status TINYINT DEFAULT 1, created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(library_id) REFERENCES library(id)
);
CREATE TABLE seat (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, room_id BIGINT NOT NULL, seat_no VARCHAR(30) NOT NULL,
 row_no INT, column_no INT, status TINYINT DEFAULT 1, created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(room_id) REFERENCES reading_room(id)
);
CREATE TABLE reservation (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, seat_id BIGINT NOT NULL,
 reservation_date DATE NOT NULL, start_time TIME NOT NULL, end_time TIME NOT NULL,
 status VARCHAR(20) DEFAULT 'RESERVED', check_in_time DATETIME, check_out_time DATETIME,
 created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES sys_user(id), FOREIGN KEY(seat_id) REFERENCES seat(id)
);
CREATE TABLE violation (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, reservation_id BIGINT,
 reason VARCHAR(255) NOT NULL, status TINYINT DEFAULT 1, created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(user_id) REFERENCES sys_user(id)
);
CREATE TABLE notice (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(200) NOT NULL, content TEXT NOT NULL,
 publisher_id BIGINT, created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
 updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO sys_user(username,password,student_no,name,phone,role,status) VALUES
('admin','123456','ADMIN001','系统管理员','13800000000','ADMIN',1),
('student','123456','20260001','张三','13800000001','STUDENT',1),
('student2','123456','20260002','李四','13800000002','STUDENT',1),
('student3','123456','20260003','王五','13800000003','STUDENT',1);

INSERT INTO library(name,address,description) VALUES
('中心图书馆','大学城校区','主校区综合图书馆，提供自习、阅览和资料查询服务。'),
('东校区图书馆','东校区','东校区学习空间与安静阅览服务。');

INSERT INTO reading_room(library_id,name,floor,capacity) VALUES
(1,'一楼开放自习区','1F',30),(1,'二楼安静阅览室','2F',40),(1,'三楼研讨学习区','3F',24),(2,'东区综合阅览室','2F',30);

INSERT INTO seat(room_id,seat_no,row_no,column_no) VALUES
(1,'A01',1,1),(1,'A02',1,2),(1,'A03',1,3),(1,'A04',1,4),(1,'A05',1,5),(1,'A06',1,6),(1,'A07',2,1),(1,'A08',2,2),(1,'A09',2,3),(1,'A10',2,4),
(1,'A11',2,5),(1,'A12',2,6),(1,'A13',3,1),(1,'A14',3,2),(1,'A15',3,3),(1,'A16',3,4),(1,'A17',3,5),(1,'A18',3,6),
(2,'B01',1,1),(2,'B02',1,2),(2,'B03',1,3),(2,'B04',1,4),(2,'B05',1,5),(2,'B06',1,6),(2,'B07',1,7),(2,'B08',1,8),(2,'B09',2,1),(2,'B10',2,2),(2,'B11',2,3),(2,'B12',2,4),(2,'B13',2,5),(2,'B14',2,6),(2,'B15',2,7),(2,'B16',2,8),
(3,'C01',1,1),(3,'C02',1,2),(3,'C03',1,3),(3,'C04',1,4),(3,'C05',1,5),(3,'C06',1,6),(3,'C07',2,1),(3,'C08',2,2),(3,'C09',2,3),(3,'C10',2,4),
(4,'D01',1,1),(4,'D02',1,2),(4,'D03',1,3),(4,'D04',1,4),(4,'D05',1,5),(4,'D06',1,6);

INSERT INTO notice(title,content,publisher_id) VALUES
('欢迎使用高校图书馆座位预约系统','预约后请按时签到，离馆时及时签退，共同维护良好的学习环境。',1),
('座位预约规则','开放时间 08:00-21:00；单次最长预约 4 小时；每天最多预约 3 次。',1),
('安静阅览室使用提醒','二楼安静阅览室请保持手机静音，禁止大声交谈。',1);

-- 示例预约：用于管理员演示统计
INSERT INTO reservation(user_id,seat_id,reservation_date,start_time,end_time,status,created_time) VALUES
(2,1,CURDATE(),'09:00:00','11:00:00','CHECKED_IN',NOW()),
(3,2,DATE_SUB(CURDATE(),INTERVAL 1 DAY),'14:00:00','16:00:00','COMPLETED',NOW()),
(4,20,DATE_SUB(CURDATE(),INTERVAL 2 DAY),'10:00:00','12:00:00','COMPLETED',NOW()),
(2,21,DATE_SUB(CURDATE(),INTERVAL 3 DAY),'13:00:00','15:00:00','COMPLETED',NOW()),
(3,22,DATE_SUB(CURDATE(),INTERVAL 4 DAY),'09:00:00','11:00:00','COMPLETED',NOW()),
(4,23,DATE_SUB(CURDATE(),INTERVAL 5 DAY),'15:00:00','17:00:00','COMPLETED',NOW()),
(2,24,DATE_SUB(CURDATE(),INTERVAL 6 DAY),'10:00:00','12:00:00','COMPLETED',NOW());
