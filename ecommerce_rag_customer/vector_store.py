"""
电商智能客服RAG服务 - 向量数据库服务
"""
import os
import shutil
import uuid
from typing import List, Dict, Any, Optional

import chromadb
from langchain_core.documents import Document
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import CharacterTextSplitter

try:
    from sentence_transformers import SentenceTransformer
    USE_SENTENCE_TRANSFORMER = True
except ImportError:
    USE_SENTENCE_TRANSFORMER = False
    print("警告: sentence-transformers未安装，使用简单嵌入")


class SentenceTransformerEmbeddings:
    """基于SentenceTransformer的嵌入实现"""
    
    def __init__(self, model_name: str = "paraphrase-multilingual-MiniLM-L12-v2"):
        self.model = SentenceTransformer(model_name)
    
    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        embeddings = self.model.encode(texts, convert_to_numpy=True, normalize_embeddings=True)
        return embeddings.tolist()
    
    def embed_query(self, text: str) -> List[float]:
        embedding = self.model.encode(text, convert_to_numpy=True, normalize_embeddings=True)
        return embedding.tolist()


class SimpleEmbeddings:
    """简单的字符级嵌入实现（备用）"""
    
    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return [self._embed(text) for text in texts]
    
    def embed_query(self, text: str) -> List[float]:
        return self._embed(text)
    
    def _embed(self, text: str) -> List[float]:
        embedding = [0.0] * 384
        for i, char in enumerate(text[:384]):
            embedding[i] = ord(char) / 255.0
        return embedding


def get_embeddings():
    global USE_SENTENCE_TRANSFORMER
    try:
        if USE_SENTENCE_TRANSFORMER:
            print("正在加载SentenceTransformer嵌入模型...")
            embeddings = SentenceTransformerEmbeddings()
            print("SentenceTransformer嵌入模型加载成功")
            return embeddings
    except Exception as e:
        print(f"SentenceTransformer加载失败: {e}")
        USE_SENTENCE_TRANSFORMER = False
    
    print("使用简单嵌入模型（备用）")
    return SimpleEmbeddings()


COLLECTION_NAME = "ecommerce_knowledge"


class VectorStoreService:
    """向量数据库服务"""
    
    def __init__(self, persist_dir: str = "./chroma_db"):
        self.persist_dir = persist_dir
        self.embeddings = get_embeddings()
        self._client = None
        self._collection = None
    
    def initialize(self) -> None:
        print(f"初始化向量数据库，目录: {self.persist_dir}")
        print(f"嵌入模型类型: {type(self.embeddings).__name__}")
        
        self._client = chromadb.PersistentClient(path=self.persist_dir)
        
        existing_collections = [c.name for c in self._client.list_collections()]
        
        if COLLECTION_NAME in existing_collections:
            print("加载现有向量数据库...")
            self._collection = self._client.get_collection(name=COLLECTION_NAME)
        else:
            print("创建新的向量数据库（余弦距离）...")
            self._collection = self._client.create_collection(
                name=COLLECTION_NAME,
                metadata={"hnsw:space": "cosine"}
            )
        
        count = self.get_count()
        print(f"向量数据库文档数: {count}")
        print(f"集合元数据: {self._collection.metadata}")
    
    def build_from_file(
        self,
        file_path: str,
        chunk_size: int = 300,
        chunk_overlap: int = 50
    ) -> int:
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"知识库文件不存在: {file_path}")
        
        loader = TextLoader(file_path, encoding="utf-8")
        documents = loader.load()
        
        text_splitter = CharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separator="\n"
        )
        split_docs = text_splitter.split_documents(documents)
        
        if os.path.exists(self.persist_dir):
            shutil.rmtree(self.persist_dir)
        
        self._client = chromadb.PersistentClient(path=self.persist_dir)
        self._collection = self._client.create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"}
        )
        
        texts = [doc.page_content for doc in split_docs]
        metadatas = [doc.metadata for doc in split_docs]
        embeddings = self.embeddings.embed_documents(texts)
        ids = [str(uuid.uuid4()) for _ in texts]
        
        self._collection.add(
            documents=texts,
            embeddings=embeddings,
            metadatas=metadatas,
            ids=ids
        )
        
        return len(split_docs)
    
    def add_documents(self, documents: List[Document]) -> None:
        if self._collection is None:
            self.initialize()
        texts = [doc.page_content for doc in documents]
        metadatas = [doc.metadata for doc in documents]
        embeddings = self.embeddings.embed_documents(texts)
        ids = [str(uuid.uuid4()) for _ in texts]
        
        self._collection.add(
            documents=texts,
            embeddings=embeddings,
            metadatas=metadatas,
            ids=ids
        )
    
    def add_text(self, content: str, metadata: Optional[Dict] = None) -> None:
        doc = Document(page_content=content, metadata=metadata or {})
        self.add_documents([doc])
    
    def search(self, query: str, top_k: int = 3, min_score: float = 0.3) -> List[Document]:
        if self._collection is None:
            self.initialize()
        
        try:
            query_embedding = self.embeddings.embed_query(query)
            
            results = self._collection.query(
                query_embeddings=[query_embedding],
                n_results=min(top_k * 3, self._collection.count())
            )
            
            print(f"检索查询: {query}")
            print(f"原始结果数: {len(results['ids'][0]) if results['ids'] else 0}")
            
            if not results['ids'] or not results['ids'][0]:
                return []
            
            filtered_results = []
            for i in range(len(results['ids'][0])):
                doc_content = results['documents'][0][i]
                distance = results['distances'][0][i]
                similarity = 1.0 - distance
                similarity = max(0.0, min(1.0, similarity))
                metadata = results['metadatas'][0][i] if results['metadatas'] and results['metadatas'][0] else {}
                print(f"  内容片段: {doc_content[:50]}... 余弦距离: {distance:.4f} 相似度: {similarity:.4f}")
                if similarity >= min_score:
                    filtered_results.append(Document(page_content=doc_content, metadata=metadata))
            
            print(f"过滤后结果数: {len(filtered_results)}")
            return filtered_results[:top_k]
        except Exception as e:
            print(f"检索错误: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def search_with_scores(self, query: str, top_k: int = 3) -> List[tuple]:
        if self._collection is None:
            self.initialize()
        
        query_embedding = self.embeddings.embed_query(query)
        results = self._collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k
        )
        
        output = []
        if results['ids'] and results['ids'][0]:
            for i in range(len(results['ids'][0])):
                doc_content = results['documents'][0][i]
                distance = results['distances'][0][i]
                metadata = results['metadatas'][0][i] if results['metadatas'] and results['metadatas'][0] else {}
                doc = Document(page_content=doc_content, metadata=metadata)
                output.append((doc, 1.0 - distance))
        
        return output
    
    def get_count(self) -> int:
        if self._collection is None:
            self.initialize()
        try:
            return self._collection.count()
        except:
            return 0
    
    def delete_by_content(self, content: str) -> int:
        if self._collection is None:
            self.initialize()
        try:
            all_data = self._collection.get()
            if not all_data['ids']:
                return 0
            ids_to_delete = []
            for i, doc_content in enumerate(all_data['documents']):
                if content.strip() in doc_content or doc_content.strip() in content:
                    ids_to_delete.append(all_data['ids'][i])
            if ids_to_delete:
                self._collection.delete(ids=ids_to_delete)
                print(f"已从向量库删除 {len(ids_to_delete)} 条匹配文档")
            return len(ids_to_delete)
        except Exception as e:
            print(f"删除知识失败: {e}")
            import traceback
            traceback.print_exc()
            return 0

    def get_all(self) -> Dict[str, Any]:
        if self._collection is None:
            self.initialize()
        return self._collection.get()
    
    def get_retriever(self, top_k: int = 3):
        return None
