# 猫猫百货商城 - 电商平台

基于 Spring Boot + Vue 3 + Python RAG 的全栈电商平台，包含用户端、管理端和 AI 智能客服。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.1.12 + MyBatis + MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 用户端前端 | Vue 3 + Vite + Element Plus |
| 管理端前端 | Vue 3 + Vite + Ant Design Vue |
| AI 客服 | Python Flask + ChromaDB + SentenceTransformer + DeepSeek API |
| 实时通信 | WebSocket |

## 项目结构

```
ecommerce-platform/
├── ecommerce-server/          # Java 后端服务 (端口 8083)
├── ecommerce-frontend/        # 用户端前端 (端口 5173)
├── ecommerce-admin/           # 管理端前端 (端口 5174)
├── ecommerce_rag_customer/    # Python RAG 智能客服 (端口 5000)
└── create_rbac_tables.sql     # RBAC 权限管理建表脚本
```

## 冷启动步骤

### 方式一：一键启动（推荐）

使用提供的 Python 脚本一键启动所有服务：

```powershell
cd D:\Desktop\ecommerce-platform
python start.py
```

### 方式二：手动启动

#### 1. 启动 MySQL

确保 MySQL 服务已运行，数据库 `ecommerce` 可访问。

#### 2. 启动 Java 后端

在 IntelliJ IDEA 中运行 Spring Boot 主类，或在终端执行：

```powershell
cd D:\Desktop\ecommerce-platform\ecommerce-server
mvn spring-boot:run
```

#### 3. 启动 Python RAG 服务

```powershell
cd D:\Desktop\ecommerce-platform\ecommerce_rag_customer
$env:HF_HUB_OFFLINE=1
$env:DEEPSEEK_API_KEY="你的DeepSeek API Key"   # 也可写入系统环境变量，一次性配置
python main.py api --port 5050
```

> `HF_HUB_OFFLINE=1` 必须设置，否则 HuggingFace 联网检查更新会超时导致启动失败。
> DeepSeek API Key 通过环境变量 `DEEPSEEK_API_KEY` 提供（Java 后端、RAG 服务、一键启动脚本均读取该变量），请勿将真实 Key 提交到仓库。

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
| 3 | Python RAG | 5000 | 无 |
| 4 | 用户端前端 | 5173 | Java 后端 |
| 5 | 管理端前端 | 5174 | Java 后端 |

> Java 后端和 RAG 服务可以同时启动，互不依赖。前端必须在 Java 后端启动后再启动。

## 核心功能

### 用户端

- 商品浏览与搜索（支持 AI 自然语言搜索）
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

首次使用需执行 `create_rbac_tables.sql` 创建权限表并初始化数据。

### AI 智能客服

- RAG 检索增强生成：基于 ChromaDB 向量数据库 + SentenceTransformer 嵌入
- 订单查询：自动识别订单号和订单查询意图，仅返回当前用户的订单
- 物流时效：识别发货时间相关问题，查询待发货订单并给出时效回答
- 问候识别：灵活匹配问候/感谢/告别语
- 超时管理：1 分钟无回复警告倒计时，2 分钟自动退出
- 多用户并发：WebSocket 多 Session 支持

## 环境要求

- JDK 21
- Node.js 16+
- Python 3.11
- MySQL 8.0
- Maven 3.8+
