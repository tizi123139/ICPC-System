-- ICPC 协会管理系统 · 本地数据库初始化（需要用 root 账号执行一次）
-- 作用：创建 icpc_system 库，并把该库的全部权限授予开发账号 tizi1234。
-- 说明：当 icpc_system 尚未创建、或开发账号缺少该库权限时，用 root 执行本脚本补授权。
--       本机已执行过，正常启动项目时无需重复运行。
--
-- 执行方式（二选一）：
--   1) 命令行：mysql -uroot -p < init-database.sql
--   2) Navicat：连接 root → 新建查询 → 粘贴本文件内容 → 运行
--
-- 执行完成后再用 tizi1234 账号导入 schema.sql 建立业务表；
-- 演示数据（会员/题库/训练/集训/赛事等）由应用以 dev profile 启动时自动执行 db/data.sql 导入。

CREATE DATABASE IF NOT EXISTS icpc_system
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 开发账号授权（只授权该库，符合最小权限原则）
GRANT ALL PRIVILEGES ON icpc_system.* TO 'tizi1234'@'localhost';

FLUSH PRIVILEGES;

-- 校验：以下两条应分别返回 icpc_system 与授权记录
SHOW DATABASES LIKE 'icpc_system';
SHOW GRANTS FOR 'tizi1234'@'localhost';
