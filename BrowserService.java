package com.lzmhc.mcpserver;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 浏览器查询
 */
@Service
public class BrowserService {
    private static final String WEB_SEARCH_URL = "https://api.search.brave.com/res/v1/web/search";
    private static final String LOCAL_POIS_URL = "https://api.search.brave.com/res/v1/local/pois";
    private static final String LOCAL_DESC_URL = "https://api.search.brave.com/res/v1/local/descriptions";
    @Value("${spring.ai.mcp.server.BRAVE_API}")
    private String BRAVE_API_KEY;
    private final ObjectMapper objectMapper;

    @Autowired
    public BrowserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip");
        headers.set("X-Subscription-Token", BRAVE_API_KEY);
        return headers;
    }
    @Tool(description = "Performs a web search using the Brave Search API, ideal for general queries, news, articles, and online content. " +
            "Use this for broad information gathering, recent events, or when you need diverse web sources. " +
            "Supports pagination, content filtering, and freshness controls. " +
            "Maximum 20 results per request, with offset for pagination. ")
    public String brave_web_search(@ToolParam(description = "Search query (max 400 chars, 50 words)") String query,
                                 @ToolParam(description = "Number of results (1-20, default 10)") int count,
                                 @ToolParam(description = "Pagination offset (max 9, default 0)") int offset){
        try {
            // 构建请求URL
            String url = String.format(
                    "%s?q=%s&count=%d&offset=%d",
                    WEB_SEARCH_URL,
                    query,
                    Math.min(count, 20),
                    Math.min(offset, 90)
            );
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
            // 创建请求对象并设置超时
            HttpRequest request = HttpRequest.get(url)
                    .setConnectionTimeout(25000)
                    .setReadTimeout(25000)
                    .setProxy(proxy);

            // 添加请求头
            HttpHeaders headers = createHeaders();
            headers.forEach((key, values) ->
                    request.header(key, String.join(",", values))
            );

            // 发送API请求并获取响应
            HttpResponse response = request.execute();

            // 检查响应状态
            if (!response.isOk()) {
                throw new RuntimeException("Brave API error: " + response.getStatus());
            }

            // 解析并格式化结果
            return formatWebResults(response.body());
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    private String formatWebResults(String jsonBody) throws Exception {
        try {
            JSONObject json = JSONUtil.parseObj(jsonBody);
            JSONObject web = json.getJSONObject("web");
            if (web == null) return "No results found";

            JSONArray results = web.getJSONArray("results");
            if (results == null || results.isEmpty()) return "No results found";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                JSONObject item = results.getJSONObject(i);
                sb.append("Title: ").append(item.getStr("title", "N/A"))
                        .append("\nDescription: ").append(item.getStr("description", "N/A"))
                        .append("\nURL: ").append(item.getStr("url", "N/A"))
                        .append("\n\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Error parsing results: " + e.getMessage();
        }
    }
//    @Tool(description =  "Searches for local businesses and places using Brave's Local Search API. " +
//            "Best for queries related to physical locations, businesses, restaurants, services, etc. " +
//            "Returns detailed information including:\n" +
//            "- Business names and addresses\n" +
//            "- Ratings and review counts\n" +
//            "- Phone numbers and opening hours\n" +
//            "Use this when the query implies 'near me' or mentions specific locations. " +
//            "Automatically falls back to web search if no local results are found.")
//    public void brave_local_search(){
//
//    }
}
