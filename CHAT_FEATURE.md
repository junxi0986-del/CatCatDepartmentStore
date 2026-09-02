# 实时在线聊天功能实现文档

## 功能概述

本项目实现了一个实时在线聊天功能，允许普通用户与后台管理员客服进行双向实时对话。主要功能包括：

- 普通用户在订单页面点击"联系客服"按钮进入聊天页面
- 管理员在后台"客服中心"查看和回复用户消息
- 实时消息收发，支持文字消息
- 聊天记录存储和历史记录查看
- 未读消息提醒
- 区分用户和客服消息样式

## 技术栈

### 后端

- Spring Boot 3.1.12
- WebSocket API
- MyBatis-Plus
- MySQL

### 前端

- Vue 3
- Element Plus (用户端)
- Ant Design Vue (管理端)
- WebSocket API

## 部署步骤

### 1. 数据库准备

1. 执行 SQL 脚本创建聊天消息表：
   ```bash
   # 执行 SQL 脚本
   Get-Content "src/main/resources/sql/create_chat_table.sql" | mysql -u root ecommerce_system
   ```

### 2. 后端部署

1. 构建后端项目：
   ```bash
   cd ecommerce-server
   mvn clean package -DskipTests
   ```
2. 启动后端服务：
   ```bash
   java -jar target/ecommerce-server-1.0.0.jar
   ```
   服务将在 <http://localhost:8083/api> 启动

### 3. 前端部署

#### 用户端

1. 构建前端项目：
   ```bash
   cd ecommerce-frontend
   npm install
   npm run build
   ```
2. 部署构建产物：
   将 `dist` 目录部署到 Web 服务器

#### 管理端

1. 构建前端项目：
   ```bash
   cd ecommerce-admin
   npm install
   npm run build
   ```
2. 部署构建产物：
   将 `dist` 目录部署到 Web 服务器

## 使用说明

### 普通用户

1. 登录用户账号
2. 进入订单列表页面（<http://localhost:5176/order）>
3. 点击任意订单的"联系客服"按钮
4. 在聊天页面中输入消息并发送
5. 等待客服回复

### 管理员客服

1. 登录管理后台（<http://localhost:5174/login）>
2. 点击左侧菜单的"客服中心"
3. 在会话列表中选择一个用户会话
4. 查看聊天历史记录
5. 输入回复消息并发送

## 关键代码结构

### 后端

- `ChatMessage.java` - 聊天消息实体类
- `ChatMapper.java` - 聊天消息数据访问接口
- `ChatWebSocketHandler.java` - WebSocket处理器
- `WebSocketConfig.java` - WebSocket配置
- `ChatController.java` - 聊天相关API控制器

### 前端

#### 用户端

- `src/views/chat/index.vue` - 聊天页面组件
- `src/views/order/index.vue` - 订单列表页面（添加了联系客服按钮）
- `src/views/order/detail.vue` - 订单详情页面（添加了联系客服按钮）
- `src/router/index.js` - 路由配置（添加了聊天页面路由）

#### 管理端

- `src/views/service/index.vue` - 客服中心页面
- `src/router/index.js` - 路由配置（添加了客服中心路由）
- `src/layout/index.vue` - 侧边栏（添加了客服中心菜单）

## 注意事项

1. **WebSocket连接**：确保WebSocket连接地址正确，默认使用 `ws://localhost:8083/api/ws/chat`
2. **管理员ID**：当前实现中，管理员ID固定为1，可根据实际情况修改
3. **会话ID**：会话ID由前端生成，格式为 `user_{userId}_{timestamp}`
4. **消息存储**：所有聊天消息都会存储到数据库中，可根据需要定期清理历史消息
5. **未读消息**：系统会记录未读消息数，并在管理员端显示未读提醒
6. **错误处理**：当前实现包含基本的错误处理，可根据需要增强

## 扩展建议

**消息类型扩展**：支持图片、文件等多种消息类型

- **消息通知**：添加消息通知功能，当有新消息时提醒用户
- **客服分配**：实现客服自动分配或手动分配功能
- **消息加密**：添加消息加密功能，提高安全性
- **聊天机器人**：集成聊天机器人，处理常见问题

## 测试流程

1. 启动后端服务
2. 启动前端用户端和管理端
3. 登录用户账号，进入订单页面，点击"联系客服"
4. 发送消息
5. 登录管理后台，进入"客服中心"，查看并回复消息
6. 验证消息是否实时收发，历史记录是否正确存储

