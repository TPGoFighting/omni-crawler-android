package top.tpgofighting.omnicrawler;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import top.tpgofighting.omnicrawler.model.AppConfig;
import top.tpgofighting.omnicrawler.model.Article;
import top.tpgofighting.omnicrawler.net.ApiClient;
import top.tpgofighting.omnicrawler.ui.NotionMarkwonPlugin;

public class MainActivity extends AppCompatActivity {

    private EditText editUrl;
    private ImageButton btnClearUrl;
    private MaterialButton btnExtractOnly;
    private MaterialButton btnExtractAndAnalyze;
    private ProgressBar progressBar;
    private Spinner spinnerModel;
    private ImageButton btnSettings;
    private ImageButton btnHistory;
    private NestedScrollView scrollView;

    private TextView tabModeSingle;
    private TextView tabModeCompare;

    // Platform click shortcuts
    private TextView badgeGithub, badgeWechat, badgeZhihu, badgeX, badgeXhs, badgeBili, badgeSearch;

    // Result Section
    private LinearLayout layoutResultSection;
    private TextView tabViewRaw;
    private TextView tabViewReport;

    private HorizontalScrollView scrollDimensions;
    private TextView dimSummary, dimAction, dimCritique, dimRoast;

    private TextView tvHeaderTitle;
    private ImageButton btnCopyHeader;
    private TextView tvPlatformBadge;
    private TextView tvWordsCount;
    private TextView tvCacheBadge;
    private ImageButton btnReExtract;

    private TextView tvBodyHeroTitle;
    private LinearLayout layoutMetaQuote;
    private TextView tvMetaAuthor;
    private TextView tvMetaPublishTime;
    private TextView tvMainContent;

    private LinearLayout bottomBar;
    private MaterialButton btnPushFeishu;
    private MaterialButton btnCopyReport;
    private MaterialButton btnOpenChat;

    private Markwon markwon;
    private Article currentArticle;
    private boolean isReportView = false;
    private String currentDimension = "summary";

    private static final String[] MODELS = {
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.7-flash-thinking",
            "claude-sonnet-4-5",
            "gemini-3.5-pro",
            "gemini-2.5-flash"
    };

    private static final String[] MODEL_NAMES = {
            "Gemini 3.8 Flash (最新)",
            "Gemini 3.7 Flash",
            "Gemini 3.7 Thinking",
            "Claude 4.5 Sonnet",
            "Gemini 3.5 Pro",
            "Gemini 2.5 Flash"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        markwon = Markwon.builder(this)
                .usePlugin(new NotionMarkwonPlugin(this))
                .usePlugin(TablePlugin.create(this))
                .usePlugin(StrikethroughPlugin.create())
                .build();

        initViews();
        setupModelSpinner();
        setupListeners();

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void initViews() {
        editUrl = findViewById(R.id.editUrl);
        btnClearUrl = findViewById(R.id.btnClearUrl);
        btnExtractOnly = findViewById(R.id.btnExtractOnly);
        btnExtractAndAnalyze = findViewById(R.id.btnExtractAndAnalyze);
        progressBar = findViewById(R.id.progressBar);
        spinnerModel = findViewById(R.id.spinnerModel);
        btnSettings = findViewById(R.id.btnSettings);
        btnHistory = findViewById(R.id.btnHistory);
        scrollView = findViewById(R.id.scrollView);

        tabModeSingle = findViewById(R.id.tabModeSingle);
        tabModeCompare = findViewById(R.id.tabModeCompare);

        badgeGithub = findViewById(R.id.badgeGithub);
        badgeWechat = findViewById(R.id.badgeWechat);
        badgeZhihu = findViewById(R.id.badgeZhihu);
        badgeX = findViewById(R.id.badgeX);
        badgeXhs = findViewById(R.id.badgeXhs);
        badgeBili = findViewById(R.id.badgeBili);
        badgeSearch = findViewById(R.id.badgeSearch);

        layoutResultSection = findViewById(R.id.layoutResultSection);
        tabViewRaw = findViewById(R.id.tabViewRaw);
        tabViewReport = findViewById(R.id.tabViewReport);

        scrollDimensions = findViewById(R.id.scrollDimensions);
        dimSummary = findViewById(R.id.dimSummary);
        dimAction = findViewById(R.id.dimAction);
        dimCritique = findViewById(R.id.dimCritique);
        dimRoast = findViewById(R.id.dimRoast);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnCopyHeader = findViewById(R.id.btnCopyHeader);
        tvPlatformBadge = findViewById(R.id.tvPlatformBadge);
        tvWordsCount = findViewById(R.id.tvWordsCount);
        tvCacheBadge = findViewById(R.id.tvCacheBadge);
        btnReExtract = findViewById(R.id.btnReExtract);

        tvBodyHeroTitle = findViewById(R.id.tvBodyHeroTitle);
        layoutMetaQuote = findViewById(R.id.layoutMetaQuote);
        tvMetaAuthor = findViewById(R.id.tvMetaAuthor);
        tvMetaPublishTime = findViewById(R.id.tvMetaPublishTime);
        tvMainContent = findViewById(R.id.tvMainContent);

        bottomBar = findViewById(R.id.bottomBar);
        btnPushFeishu = findViewById(R.id.btnPushFeishu);
        btnCopyReport = findViewById(R.id.btnCopyReport);
        btnOpenChat = findViewById(R.id.btnOpenChat);
    }

    private void setupModelSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MODEL_NAMES);
        spinnerModel.setAdapter(adapter);

        String savedModel = AppConfig.getModel(this);
        for (int i = 0; i < MODELS.length; i++) {
            if (MODELS[i].equals(savedModel)) {
                spinnerModel.setSelection(i);
                break;
            }
        }
    }

    private String getSelectedModelId() {
        int pos = spinnerModel.getSelectedItemPosition();
        if (pos >= 0 && pos < MODELS.length) {
            return MODELS[pos];
        }
        return "gemini-3.8-flash";
    }

    private void triggerHaptic(View v) {
        if (v != null) {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private void setupListeners() {
        editUrl.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearUrl.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearUrl.setOnClickListener(v -> {
            triggerHaptic(v);
            editUrl.setText("");
        });

        btnExtractOnly.setOnClickListener(v -> {
            triggerHaptic(v);
            startProcess(false, false);
        });

        btnExtractAndAnalyze.setOnClickListener(v -> {
            triggerHaptic(v);
            startProcess(true, false);
        });

        btnReExtract.setOnClickListener(v -> {
            triggerHaptic(v);
            startProcess(true, true);
        });

        // Mode Switching
        tabModeSingle.setOnClickListener(v -> {
            triggerHaptic(v);
            tabModeSingle.setBackgroundResource(R.drawable.bg_screenshot_pill_active);
            tabModeSingle.setTextColor(0xFF1A1A1A);
            tabModeSingle.setTypeface(null, android.graphics.Typeface.BOLD);
            tabModeCompare.setBackground(null);
            tabModeCompare.setTextColor(0xFF787774);
            tabModeCompare.setTypeface(null, android.graphics.Typeface.NORMAL);
        });

        tabModeCompare.setOnClickListener(v -> {
            triggerHaptic(v);
            tabModeCompare.setBackgroundResource(R.drawable.bg_screenshot_pill_active);
            tabModeCompare.setTextColor(0xFF1A1A1A);
            tabModeCompare.setTypeface(null, android.graphics.Typeface.BOLD);
            tabModeSingle.setBackground(null);
            tabModeSingle.setTextColor(0xFF787774);
            tabModeSingle.setTypeface(null, android.graphics.Typeface.NORMAL);
            Toast.makeText(this, "多源对比模式：可换行输入多个文章 URL 进行交叉对比", Toast.LENGTH_SHORT).show();
        });

        // View Segmented Switcher (提取正文 vs AI深度研报)
        tabViewRaw.setOnClickListener(v -> switchMainView(false));
        tabViewReport.setOnClickListener(v -> switchMainView(true));

        // Dimension Tabs
        dimSummary.setOnClickListener(v -> switchDimension("summary", dimSummary));
        dimAction.setOnClickListener(v -> switchDimension("action_guide", dimAction));
        dimCritique.setOnClickListener(v -> switchDimension("critique", dimCritique));
        dimRoast.setOnClickListener(v -> switchDimension("creative_roast", dimRoast));

        btnCopyHeader.setOnClickListener(v -> {
            triggerHaptic(v);
            copyText(tvHeaderTitle.getText().toString());
        });

        btnCopyReport.setOnClickListener(v -> {
            triggerHaptic(v);
            copyCurrentText();
        });

        btnPushFeishu.setOnClickListener(v -> {
            triggerHaptic(v);
            showExportOptions();
        });
        btnPushFeishu.setOnLongClickListener(v -> {
            triggerHaptic(v);
            saveCurrentToObsidian();
            return true;
        });

        btnOpenChat.setOnClickListener(v -> {
            triggerHaptic(v);
            openChatDialog();
        });

        btnHistory.setOnClickListener(v -> {
            triggerHaptic(v);
            openHistoryDialog();
        });

        btnSettings.setOnClickListener(v -> {
            triggerHaptic(v);
            openSettingsDialog();
        });
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                String cleanUrl = ApiClient.extractCleanUrl(sharedText);
                if (!cleanUrl.isEmpty()) {
                    editUrl.setText(cleanUrl);
                    Toast.makeText(this, "正在自动研读分享内容...", Toast.LENGTH_SHORT).show();
                    startProcess(true, false);
                }
            }
        }
    }

    private void startProcess(boolean autoAnalyze, boolean forceRefresh) {
        String rawInput = editUrl.getText().toString().trim();
        String cleanUrl = ApiClient.extractCleanUrl(rawInput);
        if (cleanUrl.isEmpty()) {
            Toast.makeText(this, "请输入文章链接", Toast.LENGTH_SHORT).show();
            return;
        }
        editUrl.setText(cleanUrl);

        progressBar.setVisibility(View.VISIBLE);
        btnExtractOnly.setEnabled(false);
        btnExtractAndAnalyze.setEnabled(false);

        ApiClient.getInstance().extract(this, cleanUrl, forceRefresh, new ApiClient.ApiCallback<Article>() {
            @Override
            public void onSuccess(Article article) {
                progressBar.setVisibility(View.GONE);
                btnExtractOnly.setEnabled(true);
                btnExtractAndAnalyze.setEnabled(true);
                currentArticle = article;
                displayArticle(article, autoAnalyze);

                if (autoAnalyze) {
                    switchMainView(true);
                } else {
                    switchMainView(false);
                }
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                btnExtractOnly.setEnabled(true);
                btnExtractAndAnalyze.setEnabled(true);
                Toast.makeText(MainActivity.this, "提取失败: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayArticle(Article article, boolean autoAnalyze) {
        layoutResultSection.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);

        // Header Card
        tvHeaderTitle.setText(article.title != null ? article.title : "未命名文章");
        tvBodyHeroTitle.setText(article.title != null ? article.title : "未命名文章");

        int len = article.content != null ? article.content.length() : 0;
        tvWordsCount.setText(len + " 字");
        tabViewRaw.setText("📄 提取正文  " + len + "字");

        // Platform Badge Styling
        setupPlatformBadge(article.platform);

        if (article.cacheHit) {
            tvCacheBadge.setVisibility(View.VISIBLE);
        } else {
            tvCacheBadge.setVisibility(View.GONE);
        }

        // Meta Quote
        if (article.author != null && !article.author.isEmpty()) {
            layoutMetaQuote.setVisibility(View.VISIBLE);
            tvMetaAuthor.setText("作者: " + article.author);
            tvMetaPublishTime.setText("平台: " + (article.platform != null ? article.platform : "WEB"));
        } else {
            layoutMetaQuote.setVisibility(View.GONE);
        }

        layoutResultSection.setAlpha(0f);
        layoutResultSection.setTranslationY(20f);
        layoutResultSection.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void setupPlatformBadge(String platform) {
        if (platform == null) platform = "WEB";
        String p = platform.toUpperCase();

        if (p.contains("TWITTER") || p.contains("X")) {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_x);
            tvPlatformBadge.setTextColor(0xFF000000);
            tvPlatformBadge.setText("𝕏 X (Twitter)");
        } else if (p.contains("WECHAT") || p.contains("MP")) {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_wechat);
            tvPlatformBadge.setTextColor(0xFF07C160);
            tvPlatformBadge.setText("💬 微信公众号");
        } else if (p.contains("ZHIHU")) {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_zhihu);
            tvPlatformBadge.setTextColor(0xFF0084FF);
            tvPlatformBadge.setText("知 知乎专栏");
        } else if (p.contains("GITHUB")) {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_github);
            tvPlatformBadge.setTextColor(0xFF1A1A1A);
            tvPlatformBadge.setText("🐙 GitHub");
        } else if (p.contains("XHS") || p.contains("RED")) {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_xhs);
            tvPlatformBadge.setTextColor(0xFFFF2442);
            tvPlatformBadge.setText("📕 小红书");
        } else if (p.contains("BILI")) {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_bili);
            tvPlatformBadge.setTextColor(0xFFFB7299);
            tvPlatformBadge.setText("📺 Bilibili");
        } else {
            tvPlatformBadge.setBackgroundResource(R.drawable.bg_badge_github);
            tvPlatformBadge.setTextColor(0xFF1A1A1A);
            tvPlatformBadge.setText("🌐 " + platform);
        }
    }

    private void switchMainView(boolean isReport) {
        triggerHaptic(isReport ? tabViewReport : tabViewRaw);
        isReportView = isReport;

        if (isReport) {
            tabViewReport.setBackgroundResource(R.drawable.bg_screenshot_pill_active);
            tabViewReport.setTextColor(0xFF1A1A1A);
            tabViewReport.setTypeface(null, android.graphics.Typeface.BOLD);

            tabViewRaw.setBackground(null);
            tabViewRaw.setTextColor(0xFF787774);
            tabViewRaw.setTypeface(null, android.graphics.Typeface.NORMAL);

            scrollDimensions.setVisibility(View.VISIBLE);
            renderDimensionContent();
        } else {
            tabViewRaw.setBackgroundResource(R.drawable.bg_screenshot_pill_active);
            tabViewRaw.setTextColor(0xFF1A1A1A);
            tabViewRaw.setTypeface(null, android.graphics.Typeface.BOLD);

            tabViewReport.setBackground(null);
            tabViewReport.setTextColor(0xFF2383E2);
            tabViewReport.setTypeface(null, android.graphics.Typeface.NORMAL);

            scrollDimensions.setVisibility(View.GONE);
            if (currentArticle != null) {
                markwon.setMarkdown(tvMainContent, currentArticle.content != null ? currentArticle.content : "（无正文）");
            }
        }
    }

    private void switchDimension(String dimKey, View tabView) {
        triggerHaptic(tabView);
        currentDimension = dimKey;

        TextView[] dims = {dimSummary, dimAction, dimCritique, dimRoast};
        String[] keys = {"summary", "action_guide", "critique", "creative_roast"};

        for (int i = 0; i < dims.length; i++) {
            if (keys[i].equals(dimKey)) {
                dims[i].setBackgroundResource(R.drawable.bg_screenshot_pill_active);
                dims[i].setTextColor(0xFF1A1A1A);
                dims[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                dims[i].setBackground(null);
                dims[i].setTextColor(0xFF787774);
                dims[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }

        renderDimensionContent();
    }

    private void renderDimensionContent() {
        if (currentArticle == null) return;

        if (currentArticle.analyses.containsKey(currentDimension)) {
            markwon.setMarkdown(tvMainContent, currentArticle.analyses.get(currentDimension));
        } else {
            runAnalysisForDimension(currentDimension);
        }
    }

    private void runAnalysisForDimension(String dimKey) {
        if (currentArticle == null || currentArticle.content == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvMainContent.setText("AI 正在深度研读与推理中...");

        final StringBuilder sb = new StringBuilder();
        ApiClient.getInstance().analyzeStream(this, currentArticle.content, dimKey, getSelectedModelId(), currentArticle.id, new ApiClient.StreamListener() {
            @Override
            public void onChunk(String chunk) {
                sb.append(chunk);
                markwon.setMarkdown(tvMainContent, sb.toString());
            }

            @Override
            public void onDone() {
                progressBar.setVisibility(View.GONE);
                currentArticle.analyses.put(dimKey, sb.toString());
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExportOptions() {
        if (currentArticle == null) return;
        String[] options = new String[]{"推送到飞书私聊 (1v1)", "归档至 Obsidian 知识库 (飞书收藏)", "复制研报排版"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("知识流转与归档")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pushCurrentToFeishu();
                    } else if (which == 1) {
                        saveCurrentToObsidian();
                    } else if (which == 2) {
                        copyCurrentText();
                    }
                })
                .show();
    }

    private void saveCurrentToObsidian() {
        if (currentArticle == null) return;
        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getInstance().saveToObsidian(this, currentArticle.id, new ApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "归档失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pushCurrentToFeishu() {
        if (currentArticle == null) return;
        String contentToPush = isReportView ? currentArticle.analyses.get(currentDimension) : currentArticle.content;
        if (contentToPush == null) contentToPush = currentArticle.content;

        progressBar.setVisibility(View.VISIBLE);
        ApiClient.getInstance().pushFeishu(this, currentArticle.id, currentArticle.title, currentArticle.url, currentArticle.platform, contentToPush, currentDimension, new ApiClient.ApiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void copyCurrentText() {
        if (currentArticle == null) return;
        String text = isReportView ? currentArticle.analyses.get(currentDimension) : currentArticle.content;
        if (text == null) text = currentArticle.content;
        copyText(text);
    }

    private void copyText(String text) {
        if (text == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("OmniCrawler", text));
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettingsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);
        dialog.setContentView(view);

        EditText editBaseUrl = view.findViewById(R.id.editSettingBaseUrl);
        EditText editOpenId = view.findViewById(R.id.editSettingOpenId);
        EditText editWebhook = view.findViewById(R.id.editSettingWebhook);
        TextView tvPingResult = view.findViewById(R.id.tvPingResult);
        MaterialButton btnTestPing = view.findViewById(R.id.btnTestPing);
        MaterialButton btnResetDefault = view.findViewById(R.id.btnResetDefault);
        MaterialButton btnSaveSettings = view.findViewById(R.id.btnSaveSettings);

        editBaseUrl.setText(AppConfig.getBaseUrl(this));
        editOpenId.setText(AppConfig.getFeishuOpenId(this));
        editWebhook.setText(AppConfig.getFeishuWebhook(this));

        btnTestPing.setOnClickListener(v -> {
            triggerHaptic(v);
            String targetUrl = editBaseUrl.getText().toString().trim();
            tvPingResult.setVisibility(View.VISIBLE);
            tvPingResult.setText("测试中...");
            tvPingResult.setTextColor(0xFF787774);

            ApiClient.getInstance().testConnection(this, targetUrl, new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    tvPingResult.setText(result);
                    tvPingResult.setTextColor(0xFF1C3829);
                }

                @Override
                public void onError(String error) {
                    tvPingResult.setText(error);
                    tvPingResult.setTextColor(0xFFD44C47);
                }
            });
        });

        btnResetDefault.setOnClickListener(v -> {
            triggerHaptic(v);
            AppConfig.resetToDefaults(this);
            editBaseUrl.setText(AppConfig.DEFAULT_BASE_URL);
            editOpenId.setText(AppConfig.DEFAULT_FEISHU_OPENID);
            editWebhook.setText("");
            tvPingResult.setVisibility(View.GONE);
            Toast.makeText(this, "已恢复默认配置", Toast.LENGTH_SHORT).show();
        });

        btnSaveSettings.setOnClickListener(v -> {
            triggerHaptic(v);
            AppConfig.setBaseUrl(this, editBaseUrl.getText().toString().trim());
            AppConfig.setFeishuOpenId(this, editOpenId.getText().toString().trim());
            AppConfig.setFeishuWebhook(this, editWebhook.getText().toString().trim());
            AppConfig.setModel(this, getSelectedModelId());
            Toast.makeText(this, "配置已保存并生效", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void openChatDialog() {
        if (currentArticle == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_chat, null);
        dialog.setContentView(view);

        RecyclerView recycler = view.findViewById(R.id.recyclerChat);
        EditText editChat = view.findViewById(R.id.editChatInput);
        MaterialButton btnSend = view.findViewById(R.id.btnSendChat);
        TextView tvModelBadge = view.findViewById(R.id.tvChatModelBadge);

        tvModelBadge.setText(getSelectedModelId());
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<Article.ChatMessage> messages = new ArrayList<>(currentArticle.chatHistory);
        ChatAdapter adapter = new ChatAdapter(messages, markwon);
        recycler.setAdapter(adapter);
        if (!messages.isEmpty()) recycler.scrollToPosition(messages.size() - 1);

        btnSend.setOnClickListener(v -> {
            triggerHaptic(v);
            String q = editChat.getText().toString().trim();
            if (q.isEmpty()) return;

            editChat.setText("");
            Article.ChatMessage userMsg = new Article.ChatMessage("user", q);
            messages.add(userMsg);
            adapter.notifyItemInserted(messages.size() - 1);

            Article.ChatMessage assistantMsg = new Article.ChatMessage("assistant", "正在推理...");
            messages.add(assistantMsg);
            int assistantPos = messages.size() - 1;
            adapter.notifyItemInserted(assistantPos);
            recycler.scrollToPosition(assistantPos);

            StringBuilder replyBuilder = new StringBuilder();
            ApiClient.getInstance().chatStream(this, currentArticle.content, messages.subList(0, messages.size() - 1), getSelectedModelId(), currentArticle.id, new ApiClient.StreamListener() {
                @Override
                public void onChunk(String chunk) {
                    replyBuilder.append(chunk);
                    assistantMsg.content = replyBuilder.toString();
                    adapter.notifyItemChanged(assistantPos);
                }

                @Override
                public void onDone() {
                    currentArticle.chatHistory = new ArrayList<>(messages);
                }

                @Override
                public void onError(String error) {
                    assistantMsg.content = "错误: " + error;
                    adapter.notifyItemChanged(assistantPos);
                }
            });
        });

        dialog.show();
    }

    private void openHistoryDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_history, null);
        dialog.setContentView(view);

        EditText search = view.findViewById(R.id.editHistorySearch);
        RecyclerView recycler = view.findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        ApiClient.getInstance().fetchHistory(this, "", new ApiClient.ApiCallback<List<Article>>() {
            @Override
            public void onSuccess(List<Article> list) {
                HistoryAdapter adapter = new HistoryAdapter(list, article -> {
                    dialog.dismiss();
                    currentArticle = article;
                    editUrl.setText(article.url);
                    displayArticle(article, true);
                    switchMainView(true);
                });
                recycler.setAdapter(adapter);

                search.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        ApiClient.getInstance().fetchHistory(MainActivity.this, s.toString(), new ApiClient.ApiCallback<List<Article>>() {
                            @Override public void onSuccess(List<Article> filtered) { adapter.updateList(filtered); }
                            @Override public void onError(String error) {}
                        });
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // Chat RecyclerView Adapter
    private static class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
        private final List<Article.ChatMessage> list;
        private final Markwon markwon;

        public ChatAdapter(List<Article.ChatMessage> list, Markwon markwon) {
            this.list = list;
            this.markwon = markwon;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Article.ChatMessage msg = list.get(position);
            boolean isUser = "user".equals(msg.role);
            holder.tvSender.setText(isUser ? "你" : "AI 研报分析师");
            holder.tvSender.setTextColor(isUser ? 0xFF2383E2 : 0xFF9B9A97);
            markwon.setMarkdown(holder.tvContent, msg.content);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSender, tvContent;
            ViewHolder(View v) {
                super(v);
                tvSender = v.findViewById(R.id.tvMsgSender);
                tvContent = v.findViewById(R.id.tvMsgContent);
            }
        }
    }

    // History RecyclerView Adapter
    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Article> list;
        private final OnItemClick listener;

        interface OnItemClick { void onClick(Article a); }

        public HistoryAdapter(List<Article> list, OnItemClick listener) {
            this.list = list;
            this.listener = listener;
        }

        public void updateList(List<Article> newList) {
            this.list = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_article, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Article a = list.get(position);
            holder.tvTitle.setText(a.title);
            holder.tvPlatform.setText(a.platform != null ? a.platform : "WEB");
            holder.tvWords.setText((a.content != null ? a.content.length() : 0) + " 字");
            holder.itemView.setOnClickListener(v -> listener.onClick(a));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvPlatform, tvWords;
            ViewHolder(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvHistoryTitle);
                tvPlatform = v.findViewById(R.id.tvHistoryPlatform);
                tvWords = v.findViewById(R.id.tvHistoryWords);
            }
        }
    }
}
