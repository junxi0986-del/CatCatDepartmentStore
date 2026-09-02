"""
电商智能客服RAG服务 - 配置模块
"""
import os
from dataclasses import dataclass
from typing import Optional


@dataclass
class Config:
    """应用配置类"""
    
    deepseek_api_key: Optional[str] = None
    deepseek_base_url: str = "https://api.deepseek.com"
    deepseek_model: str = "deepseek-chat"
    
    persist_dir: str = "./chroma_db"
    knowledge_file: str = "./data/shop_faq.txt"
    retrieve_top_k: int = 5
    
    flask_host: str = "0.0.0.0"
    flask_port: int = 5000
    flask_debug: bool = False
    
    temperature: float = 0.3
    max_tokens: int = 800
    
    @classmethod
    def from_env(cls) -> 'Config':
        """从环境变量加载配置"""
        return cls(
            deepseek_api_key=os.getenv("DEEPSEEK_API_KEY"),
            deepseek_base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
            deepseek_model=os.getenv("DEEPSEEK_MODEL", "deepseek-chat"),
            persist_dir=os.getenv("CHROMA_PERSIST_DIR", "./chroma_db"),
            knowledge_file=os.getenv("KNOWLEDGE_FILE", "./data/shop_faq.txt"),
            flask_port=int(os.getenv("FLASK_PORT", "5000")),
            temperature=float(os.getenv("LLM_TEMPERATURE", "0.3")),
        )
    
    def validate(self) -> bool:
        """验证配置"""
        if not self.deepseek_api_key:
            raise ValueError("请设置 DEEPSEEK_API_KEY 环境变量或通过参数传入")
        return True


PROMPT_TEMPLATE = """你是"猫猫百货商城"的专业电商智能客服，名字叫小猫。

【重要规则】：
1. 根据下方提供的资料回答用户问题，直接给出答案，不要提及"知识库"、"参考资料"等内部信息来源
2. 如果提供的资料中有与用户问题相关的内容，即使只有部分相关，也要据此组织回答，可以结合常识适当补充
3. 如果提供的资料与用户问题完全不相关，请回复："很抱歉，我暂时无法回答您的问题，建议您直接联系在线客服，我们会尽快为您处理。"
4. 不要说"根据参考资料"、"知识库中"等暴露内部检索机制的话
5. 不要重复用户的问题，直接给出答案

【回答格式要求】：
- 使用换行符分隔不同要点，让回答清晰有条理
- 涉及多个要点时，用数字编号（如1. 2. 3.）或符号（如•）列表展示
- 关键信息用简洁的短句表达，避免大段文字
- 涉及步骤或流程时，按顺序分行列出
- 语气亲切友好，像朋友聊天一样自然

【资料】：
{context}

【用户问题】：{question}

请回答："""
