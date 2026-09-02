"""
电商智能客服RAG服务 - Streamlit界面模块
"""
import streamlit as st
from typing import Optional

from config import Config
from rag_service import RAGService


def create_streamlit_app(config: Optional[Config] = None) -> None:
    """
    创建Streamlit应用
    
    Args:
        config: 应用配置
    """
    st.set_page_config(
        page_title="电商RAG智能客服",
        page_icon="🛒",
        layout="wide"
    )
    
    if config is None:
        config = Config.from_env()
    
    if 'rag_service' not in st.session_state:
        try:
            rag_service = RAGService(config)
            rag_service.initialize()
            st.session_state['rag_service'] = rag_service
        except Exception as e:
            st.error(f"初始化失败: {e}")
            return
    
    rag_service = st.session_state['rag_service']
    
    st.title("🛒 电商智能客服系统（RAG）")
    
    tab1, tab2, tab3 = st.tabs(["💬 智能对话", "📚 知识库管理", "⚙️ 系统设置"])
    
    with tab1:
        render_chat_tab(rag_service)
    
    with tab2:
        render_knowledge_tab(rag_service)
    
    with tab3:
        render_settings_tab(config, rag_service)


def render_chat_tab(rag_service: RAGService) -> None:
    """渲染对话标签页"""
    st.header("智能客服对话")
    
    if 'messages' not in st.session_state:
        st.session_state['messages'] = []
    
    for message in st.session_state['messages']:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])
    
    if prompt := st.chat_input("请输入您的问题..."):
        st.session_state['messages'].append({"role": "user", "content": prompt})
        
        with st.chat_message("user"):
            st.markdown(prompt)
        
        with st.chat_message("assistant"):
            with st.spinner("正在思考..."):
                answer = rag_service.chat(prompt)
                st.markdown(answer)
                st.session_state['messages'].append({"role": "assistant", "content": answer})
    
    col1, col2 = st.columns(2)
    with col1:
        if st.button("清空对话"):
            st.session_state['messages'] = []
            st.rerun()
    
    with col2:
        st.write(f"对话轮数: {len(st.session_state['messages']) // 2}")


def render_knowledge_tab(rag_service: RAGService) -> None:
    """渲染知识库管理标签页"""
    st.header("知识库管理")
    
    col1, col2 = st.columns(2)
    
    with col1:
        st.subheader("🔍 知识检索")
        search_query = st.text_input("输入查询内容", key="search_query")
        top_k = st.slider("返回结果数", 1, 10, 3)
        
        if st.button("检索"):
            if search_query:
                results = rag_service.search_knowledge(search_query, top_k)
                if results:
                    for i, result in enumerate(results):
                        st.write(f"**结果 {i+1}:**")
                        st.info(result['content'])
                else:
                    st.warning("未找到相关内容")
            else:
                st.warning("请输入查询内容")
    
    with col2:
        st.subheader("➕ 添加知识")
        new_content = st.text_area("知识内容", height=150)
        
        if st.button("添加"):
            if new_content:
                success = rag_service.add_knowledge(new_content)
                if success:
                    st.success("知识已添加到向量库")
                else:
                    st.error("添加失败")
            else:
                st.warning("请输入知识内容")
    
    st.divider()
    
    st.subheader("📋 知识库列表")
    if st.button("刷新知识库"):
        try:
            all_docs = rag_service.vector_store.get_all()
            st.write(f"共 {len(all_docs['ids'])} 条知识")
            for doc, id in zip(all_docs['documents'], all_docs['ids']):
                with st.expander(f"ID: {id[:8]}..."):
                    st.write(doc)
        except Exception as e:
            st.error(f"获取知识库失败: {e}")


def render_settings_tab(config: Config, rag_service: RAGService) -> None:
    """渲染设置标签页"""
    st.header("系统设置")
    
    st.subheader("当前配置")
    col1, col2 = st.columns(2)
    
    with col1:
        st.write(f"**模型:** {config.deepseek_model}")
        st.write(f"**温度:** {config.temperature}")
        st.write(f"**检索Top-K:** {config.retrieve_top_k}")
    
    with col2:
        st.write(f"**向量库路径:** {config.persist_dir}")
        st.write(f"**知识库文件:** {config.knowledge_file}")
    
    st.divider()
    
    st.subheader("知识库操作")
    if st.button("重建知识库"):
        try:
            with st.spinner("正在重建..."):
                count = rag_service.rebuild_knowledge()
                st.success(f"知识库重建完成，共处理 {count} 个文本片段")
        except Exception as e:
            st.error(f"重建失败: {e}")


if __name__ == "__main__":
    create_streamlit_app()
