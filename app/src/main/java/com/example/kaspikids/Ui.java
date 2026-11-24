package com.example.kaspikids;
import android.content.Context;
import android.view.View;
final class Ui {
    private Ui() {
    }
    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    @SuppressWarnings("deprecation") static void applyInsets(View root) {
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        }
        );
        root.requestApplyInsets();
    }
}
