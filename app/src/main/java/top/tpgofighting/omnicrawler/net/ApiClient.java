package top.tpgofighting.omnicrawler.net;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import top.tpgofighting.omnicrawler.model.AppConfig;
import top.tpgofighting.omnicrawler.model.Article;

public class ApiClient {
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static ApiClient instance;

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    private ApiClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public interface StreamListener {
        void onChunk(String chunk);
        void onDone();
        void onError(String error);
    }

    /**
     * 完整 URL 提取器：杜绝正则不全导致的参数截断
     * 保证 100% 完整保留查询参数、哈希、特殊符号
     */
    public static String extractCleanUrl(String rawText) {
        if (rawText == null) return "";
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) return "";

        // 如果用户直接粘贴的是完整 URL（无多余空格/换行），直接返回完整字符串
        if ((trimmed.startsWith("http://") || trimmed.startsWith("https://")) 
                && !trimmed.contains(" ") && !trimmed.contains("\n") && !trimmed.contains("\r") && !trimmed.contains("\t")) {
            return trimmed;
        }

        // 如果是包含分享文案的复杂文本（如："看这篇 https://mp.weixin.qq.com/s?... 推荐"）
        int startIdx = trimmed.indexOf("http://");
        if (startIdx == -1) {
            startIdx = trimmed.indexOf("https://");
        }

        if (startIdx != -1) {
            String sub = trimmed.substring(startIdx);
            int endIdx = -1;
            for (int i = 0; i < sub.length(); i++) {
                char c = sub.charAt(i);
                // 仅在遇到明确的空格、换行或常见中文闭合符号时截断外部文案
                if (Character.isWhitespace(c) || c == ' ' || c == '\n' || c == '\r' || c == '\t' 
                        || c == '）' || c == '」' || c == '》' || c == '】' || c == '”' || c == '"' || c == '\'') {
                    endIdx = i;
                    break;
                }
            }
            if (endIdx != -1) {
                return sub.substring(0, endIdx).trim();
            } else {
                return sub.trim();
            }
        }

        return trimmed;
    }

    public void testConnection(Context context, String testBaseUrl, ApiCallback<String> callback) {
        new Thread(() -> {
            try {
                String base = (testBaseUrl != null && !testBaseUrl.trim().isEmpty())
                        ? testBaseUrl.trim().replaceAll("/+$", "")
                        : AppConfig.getBaseUrl(context);

                Request req = new Request.Builder()
                        .url(base + "/crawler-api/articles?q=")
                        .get()
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    if (res.isSuccessful()) {
                        mainHandler.post(() -> callback.onSuccess("API 连接正常 (HTTP " + res.code() + ")"));
                    } else {
                        mainHandler.post(() -> callback.onError("API 返回异常: HTTP " + res.code()));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("连接失败: " + e.getMessage()));
            }
        }).start();
    }

    public void extract(Context context, String rawUrl, boolean forceRefresh, ApiCallback<Article> callback) {
        new Thread(() -> {
            try {
                String cleanUrl = extractCleanUrl(rawUrl);
                if (cleanUrl.isEmpty()) {
                    mainHandler.post(() -> callback.onError("请输入有效的文章 URL"));
                    return;
                }

                JSONObject json = new JSONObject();
                json.put("url", cleanUrl);
                json.put("force_refresh", forceRefresh);

                String baseUrl = AppConfig.getBaseUrl(context);
                RequestBody body = RequestBody.create(json.toString(), JSON_TYPE);
                Request req = new Request.Builder()
                        .url(baseUrl + "/crawler-api/extract")
                        .post(body)
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    String respStr = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        String errMsg = "提取失败 (HTTP " + res.code() + ")";
                        try {
                            JSONObject errJson = new JSONObject(respStr);
                            if (errJson.has("error")) errMsg = errJson.getString("error");
                        } catch (Exception ignored) {}
                        final String finalMsg = errMsg;
                        mainHandler.post(() -> callback.onError(finalMsg));
                        return;
                    }

                    JSONObject resObj = new JSONObject(respStr);
                    Article article = Article.fromJson(resObj.getJSONObject("data"));
                    if (resObj.optBoolean("cache_hit")) {
                        article.cacheHit = true;
                    }
                    mainHandler.post(() -> callback.onSuccess(article));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("网络请求异常: " + e.getMessage()));
            }
        }).start();
    }

    public void analyzeStream(Context context, String content, String mode, String model, String id, StreamListener listener) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("content", content);
                json.put("mode", mode);
                json.put("model", model);
                if (id != null) json.put("id", id);

                String baseUrl = AppConfig.getBaseUrl(context);
                RequestBody body = RequestBody.create(json.toString(), JSON_TYPE);
                Request req = new Request.Builder()
                        .url(baseUrl + "/crawler-api/analyze")
                        .post(body)
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    if (!res.isSuccessful()) {
                        mainHandler.post(() -> listener.onError("分析失败: HTTP " + res.code()));
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(res.body().byteStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                mainHandler.post(listener::onDone);
                                break;
                            }
                            try {
                                JSONObject chunkObj = new JSONObject(data);
                                if (chunkObj.has("chunk")) {
                                    String chunk = chunkObj.getString("chunk");
                                    mainHandler.post(() -> listener.onChunk(chunk));
                                } else if (chunkObj.has("error")) {
                                    String errMsg = chunkObj.getString("error");
                                    mainHandler.post(() -> listener.onError(errMsg));
                                    break;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError("分析发生异常: " + e.getMessage()));
            }
        }).start();
    }

    public void chatStream(Context context, String content, List<Article.ChatMessage> messages, String model, String id, StreamListener listener) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("content", content);
                json.put("model", model);
                json.put("id", id);

                JSONArray msgArr = new JSONArray();
                for (Article.ChatMessage m : messages) {
                    JSONObject mo = new JSONObject();
                    mo.put("role", m.role);
                    mo.put("content", m.content);
                    msgArr.put(mo);
                }
                json.put("messages", msgArr);

                String baseUrl = AppConfig.getBaseUrl(context);
                RequestBody body = RequestBody.create(json.toString(), JSON_TYPE);
                Request req = new Request.Builder()
                        .url(baseUrl + "/crawler-api/chat")
                        .post(body)
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    if (!res.isSuccessful()) {
                        mainHandler.post(() -> listener.onError("追问失败: HTTP " + res.code()));
                        return;
                    }

                    BufferedReader reader = new BufferedReader(new InputStreamReader(res.body().byteStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                mainHandler.post(listener::onDone);
                                break;
                            }
                            try {
                                JSONObject chunkObj = new JSONObject(data);
                                if (chunkObj.has("chunk")) {
                                    String chunk = chunkObj.getString("chunk");
                                    mainHandler.post(() -> listener.onChunk(chunk));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> listener.onError("追问异常: " + e.getMessage()));
            }
        }).start();
    }

    public void pushFeishu(Context context, String articleId, String title, String url, String platform, String content, String mode, ApiCallback<String> callback) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "direct");
                json.put("articleId", articleId);
                json.put("title", title);
                json.put("url", url);
                json.put("platform", platform);
                json.put("content", content);
                json.put("mode", mode);
                json.put("openId", AppConfig.getFeishuOpenId(context));

                String webhook = AppConfig.getFeishuWebhook(context);
                if (webhook != null && !webhook.trim().isEmpty()) {
                    json.put("webhookUrl", webhook.trim());
                }

                String baseUrl = AppConfig.getBaseUrl(context);
                RequestBody body = RequestBody.create(json.toString(), JSON_TYPE);
                Request req = new Request.Builder()
                        .url(baseUrl + "/crawler-api/push-feishu")
                        .post(body)
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    String respStr = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("推送失败: " + respStr));
                        return;
                    }
                    mainHandler.post(() -> callback.onSuccess("已成功推送到飞书私聊！"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("推送异常: " + e.getMessage()));
            }
        }).start();
    }

    public void fetchHistory(Context context, String query, ApiCallback<List<Article>> callback) {
        new Thread(() -> {
            try {
                String baseUrl = AppConfig.getBaseUrl(context);
                Request req = new Request.Builder()
                        .url(baseUrl + "/crawler-api/articles?q=" + (query != null ? query : ""))
                        .get()
                        .build();

                try (Response res = client.newCall(req).execute()) {
                    String respStr = res.body() != null ? res.body().string() : "";
                    if (!res.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("获取历史失败"));
                        return;
                    }
                    JSONObject resObj = new JSONObject(respStr);
                    JSONArray arr = resObj.getJSONArray("data");
                    List<Article> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        list.add(Article.fromJson(arr.getJSONObject(i)));
                    }
                    mainHandler.post(() -> callback.onSuccess(list));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("获取历史异常: " + e.getMessage()));
            }
        }).start();
    }
}
