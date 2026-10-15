# 高校图书馆座位预约系统 1.1（丰富版）

这是在原始可运行项目基础上升级的毕业设计演示版。

## 技术栈
- 后端：Spring Boot 3.4.5 + MyBatis-Plus + MySQL 8
- 前端：Vue 3 + Vite + Element Plus + Axios
- JDK：17+

## 本版新增
### 学生端
- 更完整的首页与仪表盘
- 阅览室选择
- 日期、开始/结束时间选择
- 座位二维可视化
- 空闲/已预约/已选择/故障状态
- 实时预约冲突提示
- 每日最多 3 次预约
- 单次最多 4 小时
- 开放时间 08:00-21:00
- 我的预约
- 签到/签退
- 自动刷新座位占用状态
- 公告展示
- 更完整的响应式 UI

### 管理端
- 数据统计
- 近 7 天预约趋势
- 用户管理与启用/禁用
- 图书馆 CRUD
- 阅览室 CRUD
- 座位 CRUD
- 预约记录查看
- 违规记录与处理
- 公告 CRUD
- 管理员登录 Token

### 自动业务
系统每 5 分钟检查当天超过预约开始时间 30 分钟仍未签到的预约：
- 状态改为 NO_SHOW
- 自动生成一条违规记录

## 运行
1. 确保 MySQL、JDK17、Maven、Node.js 已安装。
2. 使用根目录 `library_seat.sql` 初始化数据库。
   注意：该 SQL 会 DROP 原有业务表后重新创建并插入演示数据。
3. 修改：
   `library-seat-server/src/main/resources/application.yml`
   中的 MySQL username/password。
4. 启动后端：
   `cd library-seat-server`
   `mvn spring-boot:run`
5. 启动前端：
   `cd library-seat-web`
   `npm install`
   `npm run dev`
6. 浏览器访问：
   `http://localhost:5173`

## 测试账号
学生：
student / 123456

管理员：
admin / 123456

## 注意
这是毕业设计/课程设计演示版。Token 使用内存存储，密码目前为明文，正式生产系统应改为 BCrypt + JWT/Redis + 完整权限体系。
