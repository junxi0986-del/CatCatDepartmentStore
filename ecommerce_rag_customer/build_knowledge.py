
from langchain_community.document_loaders import TextLoader
from langchain_text_splitters import CharacterTextSplitter
from langchain_chroma import Chroma
from langchain_core.embeddings import Embeddings
import os
import numpy as np
from typing import List

KNOWLEDGE_FILE = "./data/shop_faq.txt"
PERSIST_DIR = "./chroma_db"

class SimpleEmbeddings(Embeddings):
    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return [self._embed(text) for text in texts]
    
    def embed_query(self, text: str) -> List[float]:
        return self._embed(text)
    
    def _embed(self, text: str) -> List[float]:
        embedding = [0.0] * 384
        for i, char in enumerate(text[:384]):
            embedding[i] = ord(char) / 255.0
        return embedding

def build_vector_db():
    loader = TextLoader(KNOWLEDGE_FILE, encoding="utf-8")
    documents = loader.load()

    text_splitter = CharacterTextSplitter(
        chunk_size=300,
        chunk_overlap=50,
        separator="\n"
    )
    split_docs = text_splitter.split_documents(documents)

    embeddings = SimpleEmbeddings()

    if os.path.exists(PERSIST_DIR) and os.listdir(PERSIST_DIR):
        db = Chroma(persist_directory=PERSIST_DIR, embedding_function=embeddings)
    else:
        db = Chroma.from_documents(
            documents=split_docs,
            embedding=embeddings,
            persist_directory=PERSIST_DIR
        )
    print("✅ 电商知识库向量化完成，向量库已保存至 chroma_db")
    print(f"   共处理 {len(split_docs)} 个文本片段")

if __name__ == "__main__":
    build_vector_db()
