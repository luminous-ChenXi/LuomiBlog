---
title: "基于 Agentic RAG 的智能问答系统"
description: "RAG（检索增强生成）技术结合 Agent 智能体，能够基于博主原创内容提供精准的问答服务，打造专属的知识库助手。"
pubDate: 2026-02-25T00:00:00
author: "luminouschenxi"
category: "技术分享"
views: 0
likes: 0
comments: 0
---

# 基于 Agentic RAG 的智能问答系统

在当今信息爆炸的时代，如何让用户快速从你的博客中获取精准答案？Agentic RAG（检索增强生成）技术为我们提供了一个完美的解决方案。本文将详细介绍如何基于阿里云百炼平台构建一个智能问答系统。

## 什么是 RAG？

RAG（Retrieval-Augmented Generation，检索增强生成）是一种将信息检索与文本生成相结合的 AI 技术。它的核心思想是：

1. **检索（Retrieval）**：从知识库中检索与用户问题相关的文档
2. **增强（Augmented）**：将检索到的内容作为上下文
3. **生成（Generation）**：基于上下文生成准确的回答

```
用户提问 → 向量检索 → 获取相关文档 → 构建提示 → LLM生成答案
```

## 为什么需要 Agentic RAG？

传统的 RAG 系统只是简单地将检索结果拼接后送入大模型，而 Agentic RAG 引入了智能体（Agent）的概念，使系统具备：

- **自主规划能力**：决定如何分解复杂问题
- **工具使用能力**：调用搜索、计算等外部工具
- **反思迭代能力**：评估答案质量并改进

### 对比表格

| 特性 | 基础 RAG | Agentic RAG |
|------|----------|-------------|
| 问题分解 | 不支持 | 支持多步推理 |
| 工具调用 | 无 | 可调用多种工具 |
| 上下文管理 | 固定窗口 | 动态管理 |
| 答案质量 | 一般 | 更高 |

## 系统架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        用户界面层                             │
│                   (博客页面嵌入的AI助手)                       │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      API 网关层                              │
│              (Spring Boot + 阿里云百炼 SDK)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                     Agent 智能体层                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ 意图识别    │  │ 任务规划    │  │ 工具调用            │  │
│  │ Intent      │  │ Planning    │  │ Tool Calling        │  │
│  │ Classifier  │  │ Agent       │  │ (搜索/计算/检索)     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                     RAG 核心层                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ 文档分块    │  │ 向量存储    │  │ 重排序优化          │  │
│  │ Chunking    │  │ Vector DB   │  │ Reranking           │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                      数据存储层                              │
│         (MySQL + Redis + 阿里云向量检索服务)                  │
└─────────────────────────────────────────────────────────────┘
```

## 核心实现步骤

### 1. 文档处理与向量化

```java
@Service
public class DocumentProcessingService {
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private VectorStoreService vectorStore;
    
    public void processArticle(Article article) {
        // 1. 文档分块
        List<String> chunks = splitIntoChunks(article.getContent());
        
        // 2. 生成向量
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            Embedding embedding = generateEmbedding(chunk);
            
            // 3. 存储到向量数据库
            Document doc = Document.builder()
                .id(article.getId() + "_" + i)
                .content(chunk)
                .embedding(embedding)
                .metadata(Map.of(
                    "articleId", article.getId(),
                    "title", article.getTitle(),
                    "chunkIndex", i
                ))
                .build();
            
            vectorStore.save(doc);
        }
    }
    
    private List<String> splitIntoChunks(String content) {
        // 使用语义分块策略
        SemanticChunker chunker = new SemanticChunker(
            500,    // 目标块大小
            50,     // 重叠大小
            SplitterType.PARAGRAPH
        );
        return chunker.split(content);
    }
}
```

### 2. 检索服务实现

```java
@Service
public class RetrievalService {
    
    @Autowired
    private VectorStoreService vectorStore;
    
    public List<RetrievedDocument> retrieve(String query, int topK) {
        // 1. 查询向量化
        Embedding queryEmbedding = generateEmbedding(query);
        
        // 2. 向量相似度搜索
        List<Document> candidates = vectorStore.similaritySearch(
            queryEmbedding, 
            topK * 2  // 检索更多候选，用于重排序
        );
        
        // 3. 重排序优化
        List<Document> reranked = rerankDocuments(query, candidates);
        
        // 4. 返回 TopK
        return reranked.stream()
            .limit(topK)
            .map(this::convertToRetrievedDoc)
            .collect(Collectors.toList());
    }
    
    private List<Document> rerankDocuments(String query, List<Document> candidates) {
        // 使用交叉编码器进行重排序
        CrossEncoderReranker reranker = new CrossEncoderReranker();
        return reranker.rerank(query, candidates);
    }
}
```

### 3. Agent 智能体实现

```java
@Component
public class QAAgent {
    
    @Autowired
    private RetrievalService retrievalService;
    
    @Autowired
    private BaiLianLLMService llmService;
    
    public String answer(String question, String conversationId) {
        // 1. 意图识别
        Intent intent = classifyIntent(question);
        
        switch (intent) {
            case GREETING:
                return handleGreeting();
            case ARTICLE_SEARCH:
                return handleArticleSearch(question);
            case KNOWLEDGE_QA:
                return handleKnowledgeQA(question, conversationId);
            case CALCULATION:
                return handleCalculation(question);
            default:
                return handleGeneralQA(question);
        }
    }
    
    private String handleKnowledgeQA(String question, String conversationId) {
        // 1. 检索相关文档
        List<RetrievedDocument> docs = retrievalService.retrieve(question, 5);
        
        // 2. 构建上下文
        String context = docs.stream()
            .map(doc -> String.format("[%s] %s", doc.getTitle(), doc.getContent()))
            .collect(Collectors.joining("\n\n"));
        
        // 3. 获取对话历史
        List<Message> history = getConversationHistory(conversationId);
        
        // 4. 构建提示词
        String prompt = buildPrompt(question, context, history);
        
        // 5. 调用 LLM 生成答案
        return llmService.generate(prompt);
    }
    
    private String buildPrompt(String question, String context, List<Message> history) {
        return String.format("""
            你是一个专业的技术博客助手，基于以下参考资料回答用户问题。
            
            ## 参考资料
            %s
            
            ## 对话历史
            %s
            
            ## 用户问题
            %s
            
            ## 回答要求
            1. 基于参考资料回答，不要编造信息
            2. 如果参考资料不足以回答，明确告知用户
            3. 引用相关文章标题
            4. 保持友好、专业的语气
            """,
            context,
            formatHistory(history),
            question
        );
    }
}
```

### 4. 阿里云百炼集成

```java
@Service
public class BaiLianService {
    
    @Value("${aliyun.bailian.api-key}")
    private String apiKey;
    
    @Value("${aliyun.bailian.app-id}")
    private String appId;
    
    private final BaiLianClient client;
    
    public BaiLianService() {
        this.client = BaiLianClient.builder()
            .apiKey(apiKey)
            .build();
    }
    
    public String generateResponse(String prompt) {
        CompletionRequest request = CompletionRequest.builder()
            .appId(appId)
            .prompt(prompt)
            .parameters(Parameters.builder()
                .temperature(0.7)
                .maxTokens(2000)
                .build())
            .build();
        
        CompletionResponse response = client.complete(request);
        return response.getText();
    }
    
    public Embedding generateEmbedding(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-v2")
            .input(text)
            .build();
        
        EmbeddingResponse response = client.embed(request);
        return response.getEmbedding();
    }
}
```

## 前端集成

### Vue 组件实现

```vue
<template>
  <div class="ai-assistant">
    <div class="chat-container" v-show="isOpen">
      <div class="chat-header">
        <h3>AI 助手</h3>
        <button @click="toggleChat">
          <IconX />
        </button>
      </div>
      
      <div class="chat-messages" ref="messagesContainer">
        <div 
          v-for="msg in messages" 
          :key="msg.id"
          :class="['message', msg.role]"
        >
          <div class="avatar">
            <img v-if="msg.role === 'assistant'" src="/ai-avatar.svg" />
            <img v-else src="/user-avatar.svg" />
          </div>
          <div class="content">
            <div class="text" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="msg.sources" class="sources">
              <span class="source-label">参考文章：</span>
              <a 
                v-for="source in msg.sources" 
                :key="source.id"
                :href="`/article/${source.slug}`"
                target="_blank"
              >
                {{ source.title }}
              </a>
            </div>
          </div>
        </div>
        
        <div v-if="isLoading" class="loading">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
      
      <div class="chat-input">
        <input 
          v-model="inputMessage"
          @keyup.enter="sendMessage"
          placeholder="输入问题，AI 助手将基于博客内容回答..."
        />
        <button @click="sendMessage" :disabled="isLoading">
          <IconSend />
        </button>
      </div>
    </div>
    
    <button class="fab" @click="toggleChat" v-show="!isOpen">
      <IconMessageCircle />
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue';
import { marked } from 'marked';

const isOpen = ref(false);
const isLoading = ref(false);
const inputMessage = ref('');
const messages = ref([
  {
    id: 1,
    role: 'assistant',
    content: '你好！我是基于博客内容的 AI 助手，可以帮你查找文章或解答技术问题。'
  }
]);

const toggleChat = () => {
  isOpen.value = !isOpen.value;
};

const sendMessage = async () => {
  if (!inputMessage.value.trim() || isLoading.value) return;
  
  const userMsg = inputMessage.value;
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMsg
  });
  inputMessage.value = '';
  isLoading.value = true;
  
  try {
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: userMsg })
    });
    
    const data = await response.json();
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: data.answer,
      sources: data.sources
    });
  } catch (error) {
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: '抱歉，服务暂时不可用，请稍后重试。'
    });
  } finally {
    isLoading.value = false;
    nextTick(() => {
      scrollToBottom();
    });
  }
};

const renderMarkdown = (text) => {
  return marked(text);
};
</script>
```

## 性能优化

### 1. 缓存策略

```java
@Service
public class CachedRetrievalService {
    
    @Autowired
    private RetrievalService retrievalService;
    
    @Autowired
    private RedisTemplate<String, List<RetrievedDocument>> redisTemplate;
    
    @Cacheable(value = "rag-retrieval", key = "#query.hashCode()")
    public List<RetrievedDocument> retrieve(String query, int topK) {
        return retrievalService.retrieve(query, topK);
    }
}
```

### 2. 异步处理

```java
@Service
public class AsyncDocumentProcessor {
    
    @Async
    public CompletableFuture<Void> processArticleAsync(Long articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        documentProcessingService.processArticle(article);
        return CompletableFuture.completedFuture(null);
    }
}
```

## 总结

Agentic RAG 为博客带来了革命性的交互体验：

1. **精准回答**：基于博客实际内容，避免幻觉
2. **上下文感知**：理解对话历史，提供连贯回答
3. **可追溯性**：每个回答都标注参考文章来源
4. **持续学习**：新文章自动加入知识库

通过阿里云百炼平台的强大能力，我们可以快速构建这样一个智能问答系统，让读者能够更高效地获取博客中的知识。

---

**参考资源**：
- [阿里云百炼平台](https://bailian.aliyun.com/)
- [RAG 论文](https://arxiv.org/abs/2005.11401)
- [LangChain 文档](https://python.langchain.com/)
- [向量数据库对比](https://www.pinecone.io/learn/vector-database/)