# 🚀 全栈演示项目 - Spring Boot + Vue3

📋 项目简介
> 现代化前后端分离架构演示 | 展示企业级开发最佳实践
基于Spring Boot 3.x构建的企业级后端服务，集成Spring Security安全框架和JPA数据持久层，
> 采用H2嵌入式数据库，支持多环境配置。

## ✨ 特性展示
- ✅ **前后端分离架构** - 清晰的职责分离
- ✅ **RESTFul API设计** - 标准接口规范
- ✅ **多环境配置** - Dev/Prod环境隔离
- ✅ **安全框架集成** - Spring Security基础配置
- ✅ **数据可视化** - ECharts图表集成
- ✅ **响应式UI** - Element Plus组件库

## 🛠 技术架构
### 后端技术栈
| 技术 | 版本 | 说明     |
|------|------|--------|
| Spring Boot | 3.3.4 | 后端框架   |
| Java | 17 | 开发语言   |
| Spring Data JPA | 3.3.4 | 数据持久化  |
| H2 Database | - | 嵌入式数据库 |
| Spring Security | 3.3.4 | 安全框架   |
| Maven | 3.6+ | 依赖管理   |

### 前端技术栈
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.5.24 | 前端框架 |
| Element Plus | 2.11.8 | UI组件库 |
| Vite | 7.2.4 | 构建工具 |
| ECharts | 6.0.0 | 数据可视化 |

## 🚀 快速启动

### 后端启动
```bash
# 开发环境
mvn spring-boot:run -P dev

# 或生产环境  
mvn spring-boot:run -P prod

# 安装依赖
npm install

# 开发环境
npm run dev

# 生产构建
npm run build:prod
8 年 Java 后端 | SpringBoot / MyBatis-Plus / Redis / MySQL
可接：SpringBoot 整包、旧系统重构、小程序云开发、鸿蒙开发
邮箱：youtyuugi@163.com | 微信：YZY92305320