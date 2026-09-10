# 猫猫百货商城 - 电商平台

基于 Spring Boot + Vue 3 + Python RAG 的全栈电商平台，包含用户端、管理端和 AI 智能客服。支持本地运行与 Cloudflare Pages 公网部署两种模式。

- GitHub 仓库：https://github.com/junxi0986-del/CatCatDepartmentStore

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.1.12 + MyBatis + MyBatis-Plus |
| 数据库 | MySQL 8.0（本地）/ TiDB Cloud Serverless（云端，MySQL 兼容） |
| 用户端前端 | Vue 3 + Vite + Element Plus |
| 管理端前端 | Vue 3 + Vite + Ant Design Vue |
| 公网部署 | Cloudflare Pages（前端静态托管）+ Pages Functions（API 反向代理）+ Cloudflare Tunnel（后端穿透） |
| AI 客服 | Python Flask + ChromaDB + SentenceTransformer + DeepSeek API |
| AI 购物顾问 | DeepSeek + 商品精选协议（SELECTED_IDS） |
| 实时通信 | WebSocket |

## 项目结构

```
ecommerce-platform/
├── ecommerce-server/          # Java 后端服务 (端口 8083)
├── ecommerce-frontend/        # 用户端前端 (端口 5173，部署于 Cloudflare Pages)
├── ecommerce-admin/           # 管理端前端 (端口 5174，部署于 Cloudflare Pages)
├── ecommerce_rag_customer/    # Python RAG 智能客服 (端口 5050)
├── sql/                       # 建库建表与 RBAC 初始化脚本
├── start.py                   # 一键启动脚本（跨平台）
└── start.bat                  # Windows 双击启动入口
```

## 冷启动步骤

### 方式一：一键启动（推荐）

Windows 下直接**双击 `start.bat`**，或执行：

```powershell
cd D:\Desktop\ecommerce-platform
python start.py
```

脚本会按顺序启动 Java 后端、RAG 服务、两个前端，并等待各端口就绪。关闭时在窗口按 `Ctrl+C` 停止全部服务。

> DeepSeek API Key 通过环境变量 `DEEPSEEK_API_KEY` 提供（Java 后端、RAG 服务、启动脚本均读取该变量），建议写入系统用户环境变量，一次性配置：
> ```powershell
> [Environment]::SetEnvironmentVariable('DEEPSEEK_API_KEY', '你的Key', 'User')
> ```

### 方式二：手动启动

#### 1. 启动 MySQL

确保 MySQL 服务已运行，数据库 `ecommerce_system` 可访问（建表脚本见 `sql/` 目录）。

#### 2. 启动 Java 后端

在 IntelliJ IDEA 中运行 Spring Boot 主类，或在终端执行：

```powershell
cd D:\Desktop\ecommerce-platform\ecommerce-server
.\mvnw.cmd spring-boot:run
```

数据库连接支持环境变量覆盖：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`（默认连本地 `ecommerce_system`）。

#### 3. 启动 Python RAG 服务

```powershell
cd D:\Desktop\ecommerce-platform\ecommerce_rag_customer
$env:HF_HUB_OFFLINE=1
$env:DEEPSEEK_API_KEY="你的DeepSeek API Key"   # 也可写入系统环境变量，一次性配置
.venv\Scripts\python main.py api --port 5050
```

> `HF_HUB_OFFLINE=1` 必须设置，否则 HuggingFace 联网检查更新会超时导致启动失败。
> 请勿将真实 API Key 提交到仓库。

#### 4. 启动用户端前端

```powershell
cd D:\Desktop\ecommerce-platform\ecommerce-frontend
npm run dev
```

#### 5. 启动管理端前端

```powershell
cd D:\Desktop\ecommerce-platform\ecommerce-admin
npm run dev
```

### 启动顺序

| 顺序 | 服务 | 端口 | 依赖 |
|:---:|------|:---:|------|
| 1 | MySQL | 3306 | 无 |
| 2 | Java 后端 | 8083 | MySQL |
| 3 | Python RAG | 5050 | 无 |
| 4 | 用户端前端 | 5173 | Java 后端 |
| 5 | 管理端前端 | 5174 | Java 后端 |

> Java 后端和 RAG 服务可以同时启动，互不依赖。前端必须在 Java 后端启动后再启动。

## 公网部署（Cloudflare）

前端托管在 Cloudflare Pages，后端运行于本地电脑，通过 Cloudflare Tunnel 暴露公网：

```
用户浏览器 → catcat-shop.pages.dev (CF Pages 静态资源)
           → Pages Functions /api 反向代理 → BACKEND_URL (Cloudflare Tunnel) → 本地 Spring Boot / RAG
```

| 组件 | 说明 |
|------|------|
| Pages 项目 ×2 | `ecommerce-frontend` 与 `ecommerce-admin` 各建一个项目，构建命令 `npm run build`，输出目录 `dist` |
| 环境变量 | `BACKEND_URL` 指向后端公网地址（Pages Functions 运行时读取）；`NODE_VERSION=20` |
| Tunnel | `cloudflared tunnel --url http://localhost:8083`，地址变化后更新两个 Pages 项目的 `BACKEND_URL` 并 Retry deployment |
| 云端数据库 | 可选：TiDB Cloud Serverless（MySQL 兼容），通过 `DB_*` 环境变量切换 |

## 核心功能

### 用户端

- 商品浏览与搜索（支持 AI 自然语言搜索）
- AI 智能购物顾问：对话式选品，商品卡片附带推荐理由；商城无匹配商品时明确告知并推荐同品类替代品；支持日常闲聊
- 购物车管理
- 订单创建、支付、取消、退款
- 收货地址管理（订单发货前可更换地址）
- 优惠券领取与使用
- AI 智能客服（RAG 检索增强生成）
- 商品评价
- 协同过滤个性化推荐

### 管理端

- 数据概览仪表盘
- 用户管理
- 商品管理（含规格、图片）
- 订单管理（发货、退款审核）
- 促销管理（优惠券）
- 客服中心（聊天记录查看）
- 知识库管理（AI 客服知识源）
- 用户行为分析
- 系统配置
- 管理员管理（RBAC 权限控制）

### RBAC 权限管理

| 角色 | 权限范围 |
|------|---------|
| 超级管理员 | 全部权限，可管理其他管理员角色 |
| 客服管理员 | 数据概览、订单管理、客服中心 |
| 商城管理员 | 数据概览、用户管理、商品管理、订单管理、知识库管理、促销管理 |

首次使用需执行 `sql/create_rbac_tables.sql` 创建权限表并初始化数据。

### AI 智能客服

- RAG 检索增强生成：基于 ChromaDB 向量数据库 + SentenceTransformer 嵌入
- 订单查询：自动识别订单号和订单查询意图，仅返回当前用户的订单
- 物流时效：识别发货时间相关问题，查询待发货订单并给出时效回答
- 问候识别：灵活匹配问候/感谢/告别语
- 超时管理：1 分钟无回复警告倒计时，2 分钟自动退出
- 多用户并发：WebSocket 多 Session 支持

### AI 购物顾问（精选协议）

后端通过 `SELECTED_IDS:id:推荐理由|...` 协议让 DeepSeek 从全量商品中精选：

- 品牌一致：用户指定品牌时只推荐该品牌商品
- 品类一致：禁止推荐无关品类配件
- 替代推荐：无匹配价位/型号时明确告知，并推荐同品类最接近的商品、说明差异
- 推荐理由：每个商品卡片展示 AI 给出的一句话推荐理由
- 兜底机制：AI 未按协议输出时回退到名称关键词匹配

## 环境要求

- JDK 21
- Node.js 18+（部署 Pages 需 20）
- Python 3.11
- MySQL 8.0
- Maven 3.8+（或直接使用项目自带 `mvnw`）
- cloudflared（可选，公网访问需要）
