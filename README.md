<div align="center">
  <h1>🌿 小药药AI Skill管理与分享</h1>
  <p><strong>企业级 AI 智能体技能注册与共享平台</strong></p>
  <p>发布 · 发现 · 管理 · 分享 — 让团队的可复用 AI 技能包在组织内安全流转</p>
</div>

<div align="center">

![Brand](https://img.shields.io/badge/品牌-小药药AI-2D5F3F?style=flat-square)
![Theme](https://img.shields.io/badge/主题-中药铺墨绿-8FBC8F?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)

</div>

---

## 📖 项目简介

**小药药AI Skill管理与分享** 是一个自托管的私有化平台，为团队提供受治理的 AI 智能体（Agent）技能共享空间。在组织内部发布技能包，推送到命名空间，让团队成员通过搜索发现或通过 CLI 安装。专为防火墙后的本地部署而构建，提供与公共注册中心相同的精致体验。

### 🎨 设计语言：中药铺墨绿国风

整体视觉采用传统中药铺的配色美学，传递专业、稳重、信赖的品牌气质：

| 色彩角色 | 色值 | 用途 |
|---------|------|------|
| 🟢 墨绿主色 | `#2D5F3F` | 主标题、按钮、品牌强调 |
| 🌿 苔绿点缀 | `#8FBC8F` | 渐变辅助、图标背景 |
| 🟡 米黄暖色 | `#F5E6C8` | 标签、装饰元素 |
| 📜 宣纸底色 | `#FAF6E8` | 页面背景 |
| 🤍 卡片白 | `#FFFEF7` | 卡片、输入框背景 |

---

## ✨ 核心特性

- 🔒 **自托管与私有化** — 部署在自己的基础设施上，专有技能保留在防火墙后，完全掌控数据主权
- 📦 **发布与版本管理** — 支持语义化版本控制、自定义标签（`beta`、`stable`）和自动 `latest` 跟踪
- 🔍 **智能发现** — 全文搜索，支持按命名空间、下载量、评分、时间等多维度筛选
- 👥 **团队命名空间** — 在团队或全局范围下组织技能，支持角色（Owner / Admin / Member）和发布策略
- 🛡️ **审核与治理** — 团队内审核，平台级审批，全流程审计日志，满足合规要求
- ⭐ **社交功能** — 收藏技能、评分并跟踪下载量，围绕最佳实践构建社区
- 🔐 **账户与令牌** — 账户合并、API 令牌管理，采用基于前缀的安全哈希
- 💻 **CLI 优先** — 原生 REST API，兼容现有 ClawHub 风格注册中心客户端
- 🗃️ **可插拔存储** — 开发环境用本地文件系统，生产环境用 S3 / MinIO
- 🌐 **国际化** — 使用 i18next 支持多语言（中文 / English）

---

## 🚀 快速开始

### Docker Compose 部署（推荐）

```bash
# 克隆仓库
git clone https://github.com/xiaoyaoyao-ai/xiaoyaoyao-skillplatform.git
cd xiaoyaoyao-skillplatform

# 创建环境配置
cp .env.release.example .env.release

# 启动完整服务栈
docker compose --env-file .env.release -f compose.release.yml up -d
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 🌐 Web UI | http://localhost | 前端界面（中药铺墨绿主题） |
| ⚙️ 后端 API | http://localhost:8080 | REST API 服务 |
| 🗄️ PostgreSQL | 127.0.0.1:5432 | 数据库 |
| ⚡ Redis | 127.0.0.1:6379 | 缓存 |
| 🔍 Skill Scanner | 内部 8000 | 安全扫描服务 |

### 默认管理员账户

首次部署时自动创建 bootstrap 管理员：

- 👤 **用户名**：`admin`
- 🔑 **密码**：`ChangeMe!2026`
- ⚠️ **生产环境务必修改密码**

### 停止服务

```bash
docker compose --env-file .env.release -f compose.release.yml down
```

---

## 🛠️ 技术栈

### 后端
- **语言**：Java 21
- **框架**：Spring Boot 3.2
- **数据库**：PostgreSQL 16 + Flyway 迁移
- **缓存**：Redis 7
- **存储**：S3 / MinIO
- **搜索**：PostgreSQL 全文搜索

### 前端
- **语言**：TypeScript
- **框架**：React 19
- **构建**：Vite
- **路由**：TanStack Router
- **数据**：TanStack Query
- **样式**：Tailwind CSS + Radix UI
- **API**：OpenAPI TypeScript（类型安全）
- **国际化**：i18next

### 基础设施
- **容器**：Docker & Docker Compose
- **监控**：Prometheus + Grafana
- **部署**：Kubernetes 清单
- **CI/CD**：GitHub Actions

---

## 📁 项目结构

```
xiaoyaoyao-skillplatform/
├── server/                  # 后端（Java/Spring Boot）
│   ├── skillhub-app/        # 主应用程序
│   ├── skillhub-domain/     # 核心业务逻辑
│   ├── skillhub-auth/       # 认证授权
│   ├── skillhub-search/     # 搜索功能
│   ├── skillhub-storage/    # 存储层
│   └── skillhub-infra/      # 基础设施
├── web/                     # 前端（React/TypeScript）
│   ├── src/
│   │   ├── pages/           # 页面（landing、home、search...）
│   │   ├── features/        # 功能模块
│   │   ├── shared/          # 共享组件
│   │   └── i18n/            # 国际化
│   └── public/              # 静态资源（favicon、logo）
├── scanner/                 # 安全扫描服务
├── cli/                     # CLI 工具
├── docs/                    # 文档
├── deploy/                  # 部署配置
├── monitoring/              # 监控配置
├── scripts/                 # 实用脚本
├── compose.release.yml      # 生产部署 Compose 文件
└── docker-compose.yml       # 本地开发 Compose 文件
```

---

## 💻 开发指南

### 前置要求

- Java 21+
- Node.js 20+
- Docker & Docker Compose
- Make（可选）

### 本地开发

```bash
# 启动后端（Java 21）
make dev-backend

# 启动前端（Vite Dev Server，端口 3000）
cd web
npm install --legacy-peer-deps
npm run dev
```

### 常用命令

```bash
make help                    # 显示所有可用命令
make test                    # 运行后端测试
make typecheck-web           # 前端类型检查
make build-web               # 构建前端
```

---

## 🎨 品牌定制

本项目已定制为"小药药AI Skill管理与分享"品牌：

- **品牌名**：小药药AI Skill管理与分享
- **Logo**：墨绿色"药"字图标（`web/public/favicon.svg`）
- **主题色**：中药铺墨绿国风
- **配置文件**：`web/src/index.css`（CSS 变量定义）

修改主题色只需编辑 `index.css` 中的 CSS 变量：

```css
:root {
  --brand-start: #2D5F3F;           /* 墨绿主色 */
  --brand-end: #3D7A4D;             /* 深青绿 */
  --brand-gradient: linear-gradient(135deg, #2D5F3F 0%, #3D7A4D 50%, #8FBC8F 100%);
}
```

---

## 📞 支持与社区

- 💬 **问题反馈**：[GitHub Issues](https://github.com/xiaoyaoyao-ai/xiaoyaoyao-skillplatform/issues)
- 📚 **使用文档**：参见 `docs/` 目录
- 🤝 **贡献指南**：欢迎提交 Pull Request

---

## 📄 许可证

[Apache License 2.0](./LICENSE)

---

<div align="center">
  <p><strong>🌿 小药药AI Skill管理与分享</strong></p>
  <p><sub>© 2026 小药药AI. All rights reserved.</sub></p>
</div>
