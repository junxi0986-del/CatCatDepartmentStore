"""
电商智能客服RAG服务 - 主入口

功能：
1. 基于大语言模型API（DeepSeek）实现智能客服
2. 使用Chroma向量数据库实现知识库检索增强（RAG）
3. 提供Flask API服务和Streamlit界面两种交互方式

使用方法：
1. Flask API服务: python main.py api --api-key YOUR_API_KEY
2. Streamlit界面: streamlit run ui.py
3. 命令行对话: python main.py chat --api-key YOUR_API_KEY

环境变量：
- DEEPSEEK_API_KEY: DeepSeek API密钥
- DEEPSEEK_BASE_URL: API基础URL（可选）
- FLASK_PORT: Flask服务端口（可选）
"""
import argparse
import os
import sys

from config import Config
from api import run_server
from rag_service import RAGService


def run_api(args: argparse.Namespace) -> None:
    """运行Flask API服务"""
    config = Config.from_env()
    
    if args.api_key:
        config.deepseek_api_key = args.api_key
    if args.port:
        config.flask_port = args.port
    
    config.validate()
    run_server(config)


def run_chat(args: argparse.Namespace) -> None:
    """运行命令行对话"""
    config = Config.from_env()
    
    if args.api_key:
        config.deepseek_api_key = args.api_key
    
    config.validate()
    
    rag_service = RAGService(config)
    rag_service.initialize()
    
    print("\n===== 电商智能客服(RAG) 已启动，输入 exit 退出 =====")
    
    while True:
        try:
            user_query = input("用户：")
            if user_query.lower() == "exit":
                print("客服：感谢咨询，祝您购物愉快！")
                break
            
            answer = rag_service.chat(user_query)
            print(f"客服：{answer}\n")
        
        except KeyboardInterrupt:
            print("\n客服：感谢咨询，再见！")
            break


def run_build(args: argparse.Namespace) -> None:
    """构建知识库"""
    config = Config.from_env()
    
    if args.api_key:
        config.deepseek_api_key = args.api_key
    
    rag_service = RAGService(config)
    
    file_path = args.file if args.file else config.knowledge_file
    count = rag_service.rebuild_knowledge(file_path)
    
    print(f"知识库构建完成，共处理 {count} 个文本片段")


def main() -> None:
    """主函数"""
    parser = argparse.ArgumentParser(
        description="电商智能客服RAG服务",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 启动Flask API服务
  python main.py api --api-key <你的DeepSeek API Key> --port 5000
  
  # 启动命令行对话
  python main.py chat --api-key <你的DeepSeek API Key>
  
  # 构建知识库
  python main.py build --file ./data/shop_faq.txt
  
  # 启动Streamlit界面
  streamlit run ui.py
        """
    )
    
    parser.add_argument(
        "--api-key",
        type=str,
        help="DeepSeek API Key"
    )
    
    subparsers = parser.add_subparsers(dest="command", help="可用命令")
    
    api_parser = subparsers.add_parser("api", help="启动Flask API服务")
    api_parser.add_argument("--port", type=int, default=5000, help="服务端口")
    api_parser.set_defaults(func=run_api)
    
    chat_parser = subparsers.add_parser("chat", help="启动命令行对话")
    chat_parser.set_defaults(func=run_chat)
    
    build_parser = subparsers.add_parser("build", help="构建知识库")
    build_parser.add_argument("--file", type=str, help="知识库文件路径")
    build_parser.set_defaults(func=run_build)
    
    args = parser.parse_args()
    
    if args.command is None:
        parser.print_help()
        sys.exit(1)
    
    args.func(args)


if __name__ == "__main__":
    main()
