package com.ecommerce.modules.chat.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.chat.entity.Knowledge;
import com.ecommerce.modules.chat.mapper.KnowledgeMapper;
import com.ecommerce.modules.chat.service.RagClientService;
import com.ecommerce.modules.chat.service.RagService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库管理控制器
 * 提供知识库的增删改查、检索、同步等API接口
 * 
 * 主要功能：
 * 1. 知识库CRUD操作（添加、删除、修改、查询）
 * 2. 知识检索（向量检索 + 数据库模糊匹配）
 * 3. 知识同步到向量库
 * 4. RAG服务状态检查
 * 
 * @author ecommerce-team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    /**
     * RAG客户端服务，用于与Python RAG服务通信
     */
    @Resource
    private RagClientService ragClientService;
    
    /**
     * 知识库Mapper，用于数据库操作
     */
    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Resource
    private RagService ragService;

    @PostMapping("/extractKeywords")
    public Result extractKeywords(@RequestBody Map<String, Object> params) {
        String content = (String) params.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.fail("内容不能为空");
        }

        String category = "general";
        if (params.get("category") != null) {
            category = params.get("category").toString();
        }

        String prompt = "请从以下知识内容中提取3-8个关键词，用于知识库检索。只返回关键词，用逗号分隔，不要其他内容。\n\n" +
                "分类：" + getCategoryName(category) + "\n" +
                "内容：" + content.trim();

        String aiKeywords = ragService.chat(prompt);

        if (aiKeywords != null && !aiKeywords.isEmpty()) {
            aiKeywords = aiKeywords.replaceAll("[\\s\\n]+", "").replaceAll("[\u3002\u3001\uff1b\uff1a\u201c\u201d\u2018\u2019\u3010\u3011\uff08\uff09\uff01\uff1f]", ",");
            String[] parts = aiKeywords.split("[,，]+");
            StringBuilder cleaned = new StringBuilder();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && trimmed.length() <= 20) {
                    if (cleaned.length() > 0) {
                        cleaned.append(",");
                    }
                    cleaned.append(trimmed);
                }
            }
            aiKeywords = cleaned.toString();
        }

        String localKeywords = extractKeywords(content);

        return Result.success(Map.of(
            "aiKeywords", aiKeywords != null ? aiKeywords : "",
            "localKeywords", localKeywords,
            "content", content.trim()
        ));
    }

    private String getCategoryName(String category) {
        switch (category) {
            case "product": return "商品咨询";
            case "logistics": return "物流配送";
            case "after_sale": return "售后问题";
            case "coupon": return "优惠券活动";
            default: return "通用";
        }
    }

    /**
     * 获取RAG服务状态
     * 检查Python RAG服务是否可用
     * 
     * @return 服务状态信息
     */
    @GetMapping("/status")
    public Result getStatus() {
        boolean available = ragClientService.isAvailable();
        return Result.success(Map.of(
            "available", available,
            "service", available ? "Python RAG Service" : "Unavailable"
        ));
    }

    /**
     * 获取知识列表
     * 返回数据库中所有知识，按创建时间降序排列
     * 
     * @return 知识列表
     */
    @GetMapping("/list")
    public Result getKnowledgeList() {
        List<Knowledge> knowledgeList = knowledgeMapper.selectAll();
        return Result.success(knowledgeList);
    }

    /**
     * 获取知识内容文本
     * 将所有知识内容合并为一段文本，用于预览
     * 
     * @return 合并后的知识内容
     */
    @GetMapping("/content")
    public Result getKnowledgeContent() {
        List<Knowledge> knowledgeList = knowledgeMapper.selectAll();
        String content = knowledgeList.stream()
            .map(Knowledge::getContent)
            .collect(Collectors.joining("\n\n"));
        return Result.success(content);
    }

    /**
     * 添加知识
     * 将知识保存到MySQL数据库，并同步到向量库
     * 
     * @param params 请求参数，包含content(知识内容)和metadata(元数据)
     * @return 添加结果，包含知识ID和同步状态
     */
    @PostMapping("/add")
    public Result addKnowledge(@RequestBody Map<String, Object> params) {
        String content = (String) params.get("content");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) params.get("metadata");
        
        if (content == null || content.trim().isEmpty()) {
            return Result.fail("内容不能为空");
        }
        
        content = content.trim();
        
        Knowledge existing = knowledgeMapper.selectByContent(content);
        if (existing != null) {
            return Result.fail("该知识已存在，请勿重复添加");
        }
        
        String category = "general";
        if (metadata != null && metadata.get("category") != null) {
            category = metadata.get("category").toString();
        }
        
        String keywords = (String) params.get("keywords");
        if (keywords == null || keywords.trim().isEmpty()) {
            keywords = extractKeywords(content);
        }
        
        Knowledge knowledge = new Knowledge(content, category);
        knowledge.setKeywords(keywords);
        knowledgeMapper.insert(knowledge);
        
        boolean ragSuccess = ragClientService.addKnowledge(content, metadata);
        
        if (ragSuccess) {
            return Result.success(Map.of(
                "message", "知识已添加到数据库和向量库",
                "id", knowledge.getId(),
                "ragSync", true
            ));
        } else {
            return Result.success(Map.of(
                "message", "知识已添加到数据库，但向量库同步失败",
                "id", knowledge.getId(),
                "ragSync", false
            ));
        }
    }

    /**
     * 检索知识
     * 优先使用向量检索，如果RAG服务不可用则回退到数据库模糊匹配
     * 
     * @param params 请求参数，包含query(查询文本)、topK(返回数量)、minScore(最小相似度)
     * @return 检索结果
     */
    @PostMapping("/search")
    public Result searchKnowledge(@RequestBody Map<String, Object> params) {
        String query = (String) params.get("query");
        Integer topK = params.get("topK") != null ? 
            Integer.parseInt(params.get("topK").toString()) : 3;
        Double minScore = params.get("minScore") != null ?
            Double.parseDouble(params.get("minScore").toString()) : 0.3;
        
        if (query == null || query.isEmpty()) {
            return Result.fail("查询不能为空");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        // 优先使用向量检索
        if (ragClientService.isAvailable()) {
            Map<String, Object> ragResult = ragClientService.searchKnowledge(query, topK, minScore);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ragResults = (List<Map<String, Object>>) ragResult.get("results");
            Integer vectorStoreCount = ragResult.get("vector_store_count") != null ?
                Integer.parseInt(ragResult.get("vector_store_count").toString()) : 0;
            
            result.put("vectorStoreCount", vectorStoreCount);
            
            if (ragResults != null && !ragResults.isEmpty()) {
                result.put("success", true);
                result.put("results", ragResults);
                result.put("source", "vector_store");
            } else {
                // 向量检索无结果，回退到数据库模糊匹配
                List<Knowledge> dbResults = knowledgeMapper.searchByKeyword(query);
                if (!dbResults.isEmpty()) {
                    result.put("success", true);
                    result.put("results", dbResults.stream()
                        .limit(topK)
                        .map(k -> Map.<String, Object>of("content", k.getContent(), "source", "database"))
                        .collect(java.util.stream.Collectors.toList()));
                    result.put("source", "database_fallback");
                } else {
                    result.put("success", true);
                    result.put("results", java.util.Collections.emptyList());
                    result.put("source", "none");
                }
            }
        } else {
            // RAG服务不可用，使用数据库模糊匹配
            List<Knowledge> dbResults = knowledgeMapper.searchByKeyword(query);
            if (!dbResults.isEmpty()) {
                result.put("success", true);
                result.put("results", dbResults.stream()
                    .limit(topK)
                    .map(k -> Map.<String, Object>of("content", k.getContent(), "source", "database"))
                    .collect(java.util.stream.Collectors.toList()));
                result.put("source", "database_only");
            } else {
                result.put("success", true);
                result.put("results", java.util.Collections.emptyList());
                result.put("source", "none");
            }
            result.put("ragUnavailable", true);
        }
        
        return Result.success(result);
    }

    /**
     * 删除知识
     * 从数据库中删除指定ID的知识
     * 
     * @param id 知识ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result deleteKnowledge(@PathVariable Long id) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return Result.fail("知识不存在");
        }
        
        String content = knowledge.getContent();
        knowledgeMapper.deleteById(id);
        
        boolean ragDeleteSuccess = ragClientService.deleteKnowledge(content);
        if (!ragDeleteSuccess) {
            return Result.success(Map.of(
                "message", "知识已从数据库删除，但向量库同步删除失败，请手动同步向量库",
                "ragSync", false
            ));
        }
        
        return Result.success(Map.of(
            "message", "知识已从数据库和向量库中删除",
            "ragSync", true
        ));
    }

    /**
     * 更新知识
     * 修改知识内容和分类，并同步到向量库
     * 
     * @param id 知识ID
     * @param params 请求参数，包含content(新内容)和category(新分类)
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result updateKnowledge(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return Result.fail("知识不存在");
        }
        
        String oldContent = knowledge.getContent();
        String content = (String) params.get("content");
        String category = (String) params.get("category");
        
        if (content != null && !content.trim().isEmpty()) {
            Knowledge existing = knowledgeMapper.selectByContent(content.trim());
            if (existing != null && !existing.getId().equals(id)) {
                return Result.fail("该知识内容已存在");
            }
            knowledge.setContent(content.trim());
            knowledge.setKeywords(extractKeywords(content.trim()));
        }
        
        if (category != null) {
            knowledge.setCategory(category);
        }
        
        knowledgeMapper.updateById(knowledge);
        
        ragClientService.deleteKnowledge(oldContent);
        boolean ragSuccess = ragClientService.addKnowledge(knowledge.getContent(), Map.of("category", knowledge.getCategory()));
        
        return Result.success(Map.of(
            "message", "知识已更新",
            "ragSync", ragSuccess
        ));
    }

    /**
     * 同步知识库到向量库
     * 清空向量库，然后将数据库中的所有知识重新添加到向量库
     * 
     * @return 同步结果，包含总数、成功数、失败数
     */
    @PostMapping("/syncToRag")
    public Result syncToRag() {
        List<Knowledge> knowledgeList = knowledgeMapper.selectAll();
        
        if (knowledgeList.isEmpty()) {
            return Result.fail("数据库中没有知识数据，请先添加知识");
        }
        
        try {
            // 调用Python RAG服务重建向量库
            String rebuildUrl = "http://localhost:5000/knowledge/rebuild";
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(rebuildUrl))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // 重建成功，逐条添加知识
                int successCount = 0;
                int failCount = 0;
                
                for (Knowledge k : knowledgeList) {
                    boolean success = ragClientService.addKnowledge(k.getContent(), Map.of("category", k.getCategory()));
                    if (success) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                }
                
                return Result.success(Map.of(
                    "total", knowledgeList.size(),
                    "success", successCount,
                    "fail", failCount,
                    "message", String.format("向量库已重建并同步%d条知识", successCount)
                ));
            } else {
                return Result.fail("重建向量库失败: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            // 重建失败，尝试直接添加
            int successCount = 0;
            int failCount = 0;
            
            for (Knowledge k : knowledgeList) {
                boolean success = ragClientService.addKnowledge(k.getContent(), Map.of("category", k.getCategory()));
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            
            return Result.success(Map.of(
                "total", knowledgeList.size(),
                "success", successCount,
                "fail", failCount,
                "message", String.format("同步完成，成功%d条，失败%d条", successCount, failCount)
            ));
        }
    }
    
    /**
     * 从内容中提取关键词
     * 移除标点符号，提取2-10个字符的词语作为关键词
     * 
     * @param content 知识内容
     * @return 关键词字符串，多个关键词用逗号分隔
     */
    private String extractKeywords(String content) {
        String[] words = content.replaceAll("[？?！!。，,、：:\"\"''（）()\\[\\]【】]", " ")
            .split("\\s+");
        StringBuilder keywords = new StringBuilder();
        for (String word : words) {
            if (word.length() >= 2 && word.length() <= 10) {
                if (keywords.length() > 0) {
                    keywords.append(",");
                }
                keywords.append(word);
            }
        }
        return keywords.length() > 200 ? keywords.substring(0, 200) : keywords.toString();
    }
}
