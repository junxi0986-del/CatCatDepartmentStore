"""
电商智能客服RAG服务 - RAG核心服务
"""
import re
from typing import Optional, Dict, Any, List

from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnablePassthrough
from langchain_core.output_parsers import StrOutputParser
from langchain_openai import ChatOpenAI

from config import Config, PROMPT_TEMPLATE
from vector_store import VectorStoreService

GREETING_PATTERNS = [
    (r'^(你好|您好|hi|hello|hey|嗨|哈喽|在吗|在不在|有人吗|亲|亲亲)', 'greeting'),
    (r'^(谢谢|感谢|多谢|thanks|thank you|3q|谢啦|谢了)', 'thanks'),
    (r'^(再见|拜拜|bye|goodbye|下次见|回见|拜)', 'farewell'),
]

GREETING_RESPONSES = {
    'greeting': '您好！欢迎来到猫猫百货商城，请问有什么可以帮您的？',
    'thanks': '不客气！很高兴能帮到您，还有其他问题吗？',
    'farewell': '感谢您的咨询，祝您购物愉快！欢迎随时再来哦',
}

# 关键词匹配规则：短查询直接匹配预设回答，绕过向量检索
KEYWORD_RULES = [
    {
        'keywords': ['七天无理由', '7天无理由', '7天退换', '七天退换', '无理由退', '无理由换', '退换货政策', '退换政策'],
        'answer': '本店所有常规商品，签收后7天内，商品完好、不影响二次销售的前提下，均可申请七天无理由退换货。\n\n1. 无质量问题的退换货，运费由买家自行承担\n2. 商品存在质量问题的退换货，运费由店铺全额承担\n3. 贴身衣物、定制类商品、特价清仓商品不支持七天无理由退换\n\n如需申请退换货，可在订单详情页点击"申请退款"或联系在线客服处理。'
    },
    {
        'keywords': ['退款', '退钱', '申请退款', '怎么退款', '如何退款', '能退款吗', '可以退款吗'],
        'answer': '关于退款规则：\n\n1. 订单未发货：可直接申请全额退款，秒审核秒通过，无任何手续费\n2. 订单已发货：不支持直接取消，可拒收快递，包裹退回仓库签收后办理全额退款\n3. 订单已签收：可按售后规则申请退换货退款\n\n退款到账时间：微信、支付宝付款即时到账；银行卡付款1-3个工作日原路退回。'
    },
    {
        'keywords': ['退货', '退换', '换货', '怎么退货', '如何退货', '能退货吗', '可以退货吗', '怎么换货'],
        'answer': '关于退换货：\n\n1. 签收后7天内，商品完好、不影响二次销售可申请退换货\n2. 商品质量问题：运费店铺全包，可免费补发或全额退款\n3. 个人原因退换：买家承担往返运费\n4. 退换货流程：联系客服 → 提供订单号和问题照片 → 寄回商品 → 仓库签收后24小时内处理\n\n以下情况不支持退换：贴身衣物、美妆用品、人为损坏、超过7天售后时效、特价清仓定制类商品。'
    },
    {
        'keywords': ['货到付款', '到付', '现金支付', '付现金'],
        'answer': '很抱歉，本店暂不支持货到付款。目前支持的支付方式有：微信支付、支付宝支付、银行卡支付、平台余额支付，所有支付渠道均为官方正规渠道，安全无风险。'
    },
    {
        'keywords': ['发票', '开票', '开发票', '要发票', '增值税'],
        'answer': '关于发票：\n\n1. 本店支持免费开具普通电子发票和增值税专用发票\n2. 下单时可直接勾选开票选项，填写抬头、税号、邮箱\n3. 下单未开票的用户，收货后30天内可联系客服补开\n4. 电子发票实时发送至邮箱，纸质发票默认随货寄出\n5. 发票信息开错可免费重开（未入账情况下）'
    },
    {
        'keywords': ['运费', '邮费', '包邮', '快递费', '要运费吗'],
        'answer': '关于运费：\n\n1. 全国大部分地区全场包邮\n2. 新疆、西藏、青海、内蒙古等偏远地区不包邮，下单页面会自动显示补差运费\n3. 店铺活动期间，部分时段偏远地区可享受限时包邮福利\n4. 退换货运费：质量问题店铺承担，个人原因买家承担'
    },
    {
        'keywords': ['支付方式', '怎么付款', '怎么支付', '付款方式', '能用微信', '能用支付宝'],
        'answer': '本店支持以下支付方式：\n\n1. 微信支付\n2. 支付宝支付\n3. 银行卡支付\n4. 平台余额支付\n\n所有支付渠道均为官方正规渠道，安全无风险，实时到账。'
    },
    {
        'keywords': ['投诉', '不满意', '差评', '维权', '举报','垃圾'],
        'answer': '如果您对商品或服务不满意：\n\n1. 可直接联系店铺售后客服反馈，我们将优先加急处理\n2. 简单问题即时解决，复杂问题24小时内给出最终处理方案\n3. 店铺未妥善解决的，可申请平台官方介入维权\n\n我们承诺全程跟进直至问题解决，保障您的合法权益。'
    },
]


def classify_greeting(message: str) -> Optional[str]:
    msg = message.strip()
    for pattern, greeting_type in GREETING_PATTERNS:
        if re.match(pattern, msg, re.IGNORECASE) and len(msg) <= 10:
            return greeting_type
    return None


def match_keyword(message: str) -> Optional[str]:
    """关键词匹配，短查询直接返回预设回答"""
    msg = message.strip()
    for rule in KEYWORD_RULES:
        for kw in rule['keywords']:
            if kw in msg:
                return rule['answer']
    return None


class RAGService:
    """RAG检索增强生成服务"""
    
    def __init__(self, config: Config):
        """
        初始化RAG服务
        
        Args:
            config: 应用配置
        """
        self.config = config
        self.vector_store = VectorStoreService(config.persist_dir)
        self.rag_chain = None
        self._initialized = False
    
    def initialize(self) -> None:
        """初始化RAG服务"""
        self.config.validate()
        
        self.vector_store.initialize()
        
        self.llm = ChatOpenAI(
            model=self.config.deepseek_model,
            api_key=self.config.deepseek_api_key,
            base_url=self.config.deepseek_base_url,
            temperature=self.config.temperature
        )
        
        self._initialized = True
    
    def chat(self, message: str, context: Optional[Dict[str, Any]] = None) -> str:
        """
        对话生成
        
        Args:
            message: 用户消息
            context: 上下文信息
            
        Returns:
            AI回复
        """
        if not self._initialized:
            self.initialize()
        
        try:
            greeting_type = classify_greeting(message)
            if greeting_type:
                print(f"识别为问候语: {greeting_type}")
                return GREETING_RESPONSES[greeting_type]

            keyword_answer = match_keyword(message)
            if keyword_answer:
                print(f"关键词匹配命中: {message}")
                return keyword_answer

            docs = self.vector_store.search(message, self.config.retrieve_top_k, 0.35)
            
            print(f"\n=== RAG对话调试 ===")
            print(f"用户问题: {message}")
            print(f"检索到 {len(docs)} 条相关知识:")
            for i, doc in enumerate(docs):
                print(f"  [{i+1}] {doc.page_content[:80]}...")
            
            if not docs:
                print("未检索到相关知识，返回兜底回复")
                print(f"===================\n")
                return "很抱歉，我暂时无法回答您的问题，建议您直接联系在线客服，我们会尽快为您处理。"
            
            knowledge_context = "\n\n".join([doc.page_content for doc in docs])
            print(f"知识上下文长度: {len(knowledge_context)} 字符")
            
            if context:
                context_str = "\n".join([f"{k}: {v}" for k, v in context.items()])
                full_question = f"用户订单信息：\n{context_str}\n\n用户问题：{message}"
            else:
                full_question = message
            
            print(f"===================\n")
            
            from langchain_core.output_parsers import StrOutputParser
            
            prompt = PromptTemplate(
                template=PROMPT_TEMPLATE,
                input_variables=["context", "question"]
            )
            
            chain = prompt | self.llm | StrOutputParser()
            
            return chain.invoke({"context": knowledge_context, "question": full_question})
            
        except Exception as e:
            print(f"RAG对话错误: {e}")
            import traceback
            traceback.print_exc()
            return "很抱歉，我暂时无法回答您的问题，建议您直接联系在线客服，我们会尽快为您处理。"
    
    def search_knowledge(self, query: str, top_k: int = 3, min_score: float = 0.3) -> list:
        """
        检索知识库
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
            min_score: 最小相似度阈值
            
        Returns:
            检索结果列表
        """
        if not self._initialized:
            self.initialize()
        
        docs = self.vector_store.search(query, top_k, min_score)
        return [{"content": doc.page_content, "metadata": doc.metadata} for doc in docs]
    
    def get_vector_store_count(self) -> int:
        """获取向量库中的文档数量"""
        if not self._initialized:
            self.initialize()
        return self.vector_store.get_count()
    
    def add_knowledge(self, content: str, metadata: Optional[Dict] = None) -> bool:
        """
        添加知识到向量库
        
        Args:
            content: 知识内容
            metadata: 元数据
            
        Returns:
            是否成功
        """
        try:
            if not self._initialized:
                self.initialize()
            self.vector_store.add_text(content, metadata)
            return True
        except Exception as e:
            print(f"添加知识失败: {e}")
            return False
    
    def rebuild_knowledge(self, file_path: Optional[str] = None) -> int:
        """
        重建知识库
        
        Args:
            file_path: 知识库文件路径
            
        Returns:
            处理的文本块数量
        """
        file_path = file_path or self.config.knowledge_file
        return self.vector_store.build_from_file(file_path)
