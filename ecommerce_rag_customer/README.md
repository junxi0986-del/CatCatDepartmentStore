# 电商智能客服RAG服务

基于大语言模型API和向量数据库的智能客服系统，支持检索增强生成（RAG）。

## 项目结构

```
ecommerce_rag_customer/
├── config.py           # 配置模块
├── vector_store.py     # 向量数据库服务
├── rag_service.py      # RAG核心服务
├── api.py              # Flask API服务
├── ui.py               # Streamlit界面
├── main.py             # 主入口
├── auth.py             # 用户认证模块
├── requirements.txt    # 依赖列表
├── environment.yml     # Conda环境配置
├── run.bat             # 启动脚本
├── data/
│   └── shop_faq.txt    # 知识库文件
└── chroma_db/          # 向量数据库存储
```

## 环境配置

### 方式一：使用Miniconda

```bash
# 创建Conda环境
conda env create -f environment.yml
conda activate rag_service
```

### 方式二：使用pip

```bash
# 创建虚拟环境
python -m venv venv
venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt
```

## 使用方法

### 1. 设置环境变量

```bash
# Windows
set DEEPSEEK_API_KEY=your_api_key

# Linux/Mac
export DEEPSEEK_API_KEY=your_api_key
```

### 2. 启动服务

**Flask API服务：**
```bash
python main.py api --port 5000
```

**Streamlit界面：**
```bash
streamlit run ui.py
```

**命令行对话：**
```bash
python main.py chat
```

**构建知识库：**
```bash
python main.py build --file ./data/shop_faq.txt
```

### 3. 使用启动脚本

双击 `run.bat` 选择启动方式。

## API接口

| 接口 | 方法 | 说明 |
|-----|------|------|
| /health | GET | 健康检查 |
| /chat | POST | RAG对话 |
| /chat/simple | POST | 简单对话 |
| /knowledge/search | POST | 知识检索 |
| /knowledge/add | POST | 添加知识 |
| /knowledge/list | GET | 列出知识 |
| /knowledge/rebuild | POST | 重建知识库 |

## 技术栈

- **Python 3.11** - 开发语言
- **Flask** - API服务框架
- **Streamlit** - Web界面框架
- **LangChain** - LLM应用框架
- **Chroma** - 向量数据库
- **DeepSeek API** - 大语言模型

## 功能特点

1. **RAG检索增强** - 基于知识库的智能问答
2. **向量检索** - Chroma向量数据库存储和检索
3. **多接口支持** - Flask API + Streamlit界面
4. **知识库管理** - 支持添加、检索、重建知识库
5. **上下文支持** - 支持订单等上下文信息
