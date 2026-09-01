package top.tpgofighting.omnicrawler.ui;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.NonNull;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.core.MarkwonTheme;

public class NotionMarkwonPlugin extends AbstractMarkwonPlugin {
    private final Context context;

    public NotionMarkwonPlugin(Context context) {
        this.context = context;
    }

    @Override
    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
        int inkPrimary = 0xFF37352F;
        int calloutBg = 0xFFF7F7F5;
        int borderSubtle = 0xFFEDEDEB;
        int linkBlue = 0xFF2383E2;

        builder
                .linkColor(linkBlue)
                .isLinkUnderlined(true)
                // BlockQuote: Left 3dp solid charcoal line, soft callout background
                .blockQuoteColor(inkPrimary)
                .blockQuoteWidth(dpToPx(3))
                // Code inline & block
                .codeBackgroundColor(calloutBg)
                .codeTextColor(inkPrimary)
                .codeBlockBackgroundColor(calloutBg)
                .codeBlockTextColor(inkPrimary)
                // Heading text styling
                .headingBreakColor(borderSubtle)
                .headingBreakHeight(dpToPx(1))
                .headingTextSizeMultipliers(new float[]{1.35F, 1.2F, 1.08F, 1.0F, 0.9F, 0.8F})
                .headingTypeface(Typeface.create("sans-serif", Typeface.BOLD))
                // Bullet styling
                .bulletWidth(dpToPx(4))
                // Thematic break (hr)
                .thematicBreakColor(borderSubtle)
                .thematicBreakHeight(dpToPx(1));
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
