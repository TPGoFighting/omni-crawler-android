package top.tpgofighting.omnicrawler.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Article implements Serializable {
    public String id;
    public String url;
    public String title;
    public String platform;
    public String author;
    public String content;
    public boolean cacheHit;
    public Map<String, String> analyses = new HashMap<>();
    public List<ChatMessage> chatHistory = new ArrayList<>();

    public static class ChatMessage implements Serializable {
        public String role;
        public String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static Article fromJson(JSONObject json) {
        Article a = new Article();
        a.id = json.optString("id");
        a.url = json.optString("url");
        a.title = json.optString("title", "未命名文章");
        a.platform = json.optString("platform", "WEB");
        a.author = json.optString("author", "");
        a.content = json.optString("content", "");
        a.cacheHit = json.optBoolean("cache_hit", false) || json.optBoolean("from_cache", false);

        JSONObject an = json.optJSONObject("analyses");
        if (an != null) {
            Iterator<String> keys = an.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                a.analyses.put(k, an.optString(k));
            }
        }

        JSONArray chatArr = json.optJSONArray("chat_history");
        if (chatArr != null) {
            for (int i = 0; i < chatArr.length(); i++) {
                JSONObject msg = chatArr.optJSONObject(i);
                if (msg != null) {
                    a.chatHistory.add(new ChatMessage(msg.optString("role"), msg.optString("content")));
                }
            }
        }
        return a;
    }
}
