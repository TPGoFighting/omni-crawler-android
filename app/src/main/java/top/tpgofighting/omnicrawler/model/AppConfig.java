package top.tpgofighting.omnicrawler.model;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    private static final String PREF_NAME = "omni_crawler_config";
    public static final String DEFAULT_BASE_URL = "https://token.tpgofighting.top";
    public static final String DEFAULT_MODEL = "gemini-3.7-flash";
    public static final String DEFAULT_FEISHU_OPENID = "ou_18b51124eb33d550d9131231fac34797";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getBaseUrl(Context context) {
        String url = getPrefs(context).getString("base_url", DEFAULT_BASE_URL);
        if (url == null || url.trim().isEmpty()) {
            return DEFAULT_BASE_URL;
        }
        return url.trim().replaceAll("/+$", "");
    }

    public static void setBaseUrl(Context context, String url) {
        getPrefs(context).edit().putString("base_url", url != null ? url.trim() : DEFAULT_BASE_URL).apply();
    }

    public static String getModel(Context context) {
        return getPrefs(context).getString("selected_model", DEFAULT_MODEL);
    }

    public static void setModel(Context context, String model) {
        getPrefs(context).edit().putString("selected_model", model).apply();
    }

    public static String getFeishuOpenId(Context context) {
        return getPrefs(context).getString("feishu_openid", DEFAULT_FEISHU_OPENID);
    }

    public static void setFeishuOpenId(Context context, String openId) {
        getPrefs(context).edit().putString("feishu_openid", openId != null ? openId.trim() : "").apply();
    }

    public static String getFeishuWebhook(Context context) {
        return getPrefs(context).getString("feishu_webhook", "");
    }

    public static void setFeishuWebhook(Context context, String webhook) {
        getPrefs(context).edit().putString("feishu_webhook", webhook != null ? webhook.trim() : "").apply();
    }

    public static void resetToDefaults(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}
