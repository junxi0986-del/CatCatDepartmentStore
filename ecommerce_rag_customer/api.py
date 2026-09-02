"""
电商智能客服RAG服务 - Flask API模块
"""
from flask import Flask, request, jsonify
from flask_cors import CORS
from typing import Optional

from config import Config
from rag_service import RAGService


def create_app(config: Optional[Config] = None) -> Flask:
    """
    创建Flask应用
    
    Args:
        config: 应用配置
        
    Returns:
        Flask应用实例
    """
    app = Flask(__name__)
    CORS(app)
    
    if config is None:
        config = Config.from_env()
    
    rag_service = RAGService(config)
    
    @app.route('/health', methods=['GET'])
    def health():
        """健康检查接口"""
        return jsonify({
            "status": "ok",
            "service": "rag-service",
            "model": config.deepseek_model
        })
    
    @app.route('/chat', methods=['POST'])
    def chat():
        """
        RAG对话接口
        
        Request:
            {
                "message": "用户消息",
                "context": {"key": "value"}  // 可选
            }
        """
        try:
            data = request.get_json()
            message = data.get('message', '')
            context = data.get('context')
            
            if not message:
                return jsonify({"error": "消息不能为空"}), 400
            
            answer = rag_service.chat(message, context)
            
            return jsonify({
                "success": True,
                "answer": answer
            })
        
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e),
                "answer": "抱歉，服务出现错误，请稍后再试。"
            })
    
    @app.route('/chat/simple', methods=['POST'])
    def simple_chat():
        """简单对话接口（无上下文）"""
        try:
            data = request.get_json()
            message = data.get('message', '')
            
            if not message:
                return jsonify({"error": "消息不能为空"}), 400
            
            answer = rag_service.chat(message)
            
            return jsonify({
                "success": True,
                "answer": answer
            })
        
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    @app.route('/knowledge/search', methods=['POST'])
    def search_knowledge():
        """
        知识库检索接口
        
        Request:
            {
                "query": "查询文本",
                "top_k": 3,  // 可选
                "min_score": 0.3  // 可选，最小相似度阈值
            }
        """
        try:
            data = request.get_json()
            query = data.get('query', '')
            top_k = data.get('top_k', 3)
            min_score = data.get('min_score', 0.3)
            
            if not query:
                return jsonify({"error": "查询不能为空"}), 400
            
            results = rag_service.search_knowledge(query, top_k, min_score)
            
            return jsonify({
                "success": True,
                "results": results,
                "count": len(results),
                "vector_store_count": rag_service.get_vector_store_count()
            })
        
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    @app.route('/knowledge/count', methods=['GET'])
    def get_knowledge_count():
        """获取向量库文档数量"""
        try:
            count = rag_service.get_vector_store_count()
            return jsonify({
                "success": True,
                "count": count
            })
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    @app.route('/knowledge/add', methods=['POST'])
    def add_knowledge():
        """
        添加知识到向量库
        
        Request:
            {
                "content": "知识内容",
                "metadata": {}  // 可选
            }
        """
        try:
            data = request.get_json()
            content = data.get('content', '')
            metadata = data.get('metadata', {})
            
            if not content:
                return jsonify({"error": "内容不能为空"}), 400
            
            success = rag_service.add_knowledge(content, metadata)
            
            return jsonify({
                "success": success,
                "message": "知识添加成功" if success else "知识添加失败"
            })
        
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    @app.route('/knowledge/delete', methods=['POST'])
    def delete_knowledge():
        """
        从向量库删除知识
        
        Request:
            {
                "content": "要删除的知识内容"
            }
        """
        try:
            data = request.get_json()
            content = data.get('content', '')
            
            if not content:
                return jsonify({"error": "内容不能为空"}), 400
            
            deleted_count = rag_service.vector_store.delete_by_content(content)
            
            return jsonify({
                "success": True,
                "message": f"已从向量库删除 {deleted_count} 条匹配文档",
                "deleted_count": deleted_count
            })
        
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    @app.route('/knowledge/rebuild', methods=['POST'])
    def rebuild_knowledge():
        """清空并重建向量库"""
        try:
            import shutil
            import os
            import gc
            
            persist_dir = rag_service.vector_store.persist_dir
            print(f"准备清空向量库目录: {persist_dir}")
            
            rag_service.vector_store._collection = None
            rag_service.vector_store._client = None
            gc.collect()
            
            if os.path.exists(persist_dir):
                max_retries = 3
                for attempt in range(max_retries):
                    try:
                        shutil.rmtree(persist_dir)
                        print(f"已清空向量库目录: {persist_dir}")
                        break
                    except PermissionError:
                        if attempt < max_retries - 1:
                            import time
                            time.sleep(0.5)
                            gc.collect()
                        else:
                            print(f"警告: 无法删除目录 {persist_dir}，将尝试清空集合")
            
            os.makedirs(persist_dir, exist_ok=True)
            
            rag_service.vector_store.initialize()
            
            print(f"向量库重建完成，当前文档数: {rag_service.vector_store.get_count()}")
            
            return jsonify({
                "success": True,
                "message": "向量库已清空并重新初始化",
                "count": rag_service.vector_store.get_count()
            })
        except Exception as e:
            print(f"重建向量库失败: {e}")
            import traceback
            traceback.print_exc()
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    @app.route('/knowledge/list', methods=['GET'])
    def list_knowledge():
        """列出所有知识"""
        try:
            all_docs = rag_service.vector_store.get_all()
            results = [
                {"id": id, "content": doc}
                for doc, id in zip(all_docs['documents'], all_docs['ids'])
            ]
            
            return jsonify({
                "success": True,
                "count": len(results),
                "results": results
            })
        
        except Exception as e:
            return jsonify({
                "success": False,
                "error": str(e)
            })
    
    return app


def run_server(config: Config) -> None:
    """
    运行Flask服务器
    
    Args:
        config: 应用配置
    """
    app = create_app(config)
    print(f"RAG服务启动在端口 {config.flask_port}")
    app.run(
        host=config.flask_host,
        port=config.flask_port,
        debug=config.flask_debug,
        threaded=True
    )
