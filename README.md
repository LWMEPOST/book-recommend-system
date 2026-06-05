# 图书推荐系统

## 项目介绍

本项目是一个基于协同过滤算法的图书推荐系统，包含 Spring Boot 后端、Vue 管理/用户前端和数据库脚本，实现用户登录、图书管理、评分收藏和个性化推荐等功能。

## 技术栈

- Spring Boot 2.7
- Spring Security
- MyBatis-Plus
- JWT
- MySQL
- Vue 3
- Vite
- Element Plus
- Pinia

## 部署要求

- JDK 17
- Maven
- MySQL 8.0
- Node.js 18 或以上
- npm/pnpm

## 运行流程

1. 创建 book_recommend 数据库并导入 sql/init.sql。
2. 修改 backend/src/main/resources/application.yml。
3. 进入 backend 执行 mvnw spring-boot:run。
4. 进入 frontend 执行 npm install。
5. 执行 npm run dev 启动前端。

## 项目结构

- backend：后端服务
- frontend：前端项目
- sql：数据库初始化脚本
- scripts：辅助脚本
