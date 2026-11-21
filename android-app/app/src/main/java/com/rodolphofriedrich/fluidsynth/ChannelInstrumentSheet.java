package com.rodolphofriedrich.fluidsynth;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Painel permanente de configuração do SoundFont por canal.
 * Regra: celular em retrato = tela cheia; paisagem ou tablet = painel lateral
 * retrátil à direita, mantendo a tela MIDI visível. Não converter para bottom sheet.
 */
public final class ChannelInstrumentSheet {
    private static Dialog activeDialog;

    public interface Listener {
        void onLibrarySelected(int index);
    }

    private ChannelInstrumentSheet() {}

    public static void show(Context context, int channel, String currentInstrument,
            String[] libraries, Listener listener) {
        showChoices(context, context.getString(R.string.channel_picker_title, channel + 1),
                currentInstrument, context.getString(R.string.soundfont_libraries_title),
                libraries, context.getString(R.string.cancel), null, listener);
    }

    public static void showPresets(Context context, int channel, String libraryName,
            String[] presets, Runnable onBack, Listener listener) {
        showChoices(context, "Presets · Canal " + (channel + 1), libraryName,
                "INSTRUMENTOS DISPONÍVEIS", presets, "Voltar", onBack, listener);
    }

    private static void showChoices(Context context, String titleText, String subtitleText,
            String sectionText, String[] choices, String closeText, Runnable onBack,
            Listener listener) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(context, 20), AppUi.dp(context, 18),
                AppUi.dp(context, 20), AppUi.dp(context, 24));
        root.setBackground(AppUi.rounded(context, AppUi.PANEL, 20, AppUi.BORDER));

        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextColor(AppUi.TEXT);
        title.setTextSize(18);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText(subtitleText);
        subtitle.setTextColor(AppUi.MUTED);
        subtitle.setTextSize(13);
        subtitle.setMaxLines(2);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = AppUi.dp(context, 5);
        subtitleParams.bottomMargin = AppUi.dp(context, 12);
        root.addView(subtitle, subtitleParams);

        root.addView(AppUi.sectionLabel(context, sectionText));
        LinearLayout choiceList = new LinearLayout(context);
        choiceList.setOrientation(LinearLayout.VERTICAL);
        ScrollView choiceScroll = new ScrollView(context);
        choiceScroll.setFillViewport(false);
        choiceScroll.addView(choiceList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        float displayDensity = context.getResources().getDisplayMetrics().density;
        int screenHeightDp = Math.round(context.getResources().getDisplayMetrics().heightPixels / displayDensity);
        int maxListHeight = AppUi.dp(context, Math.max(120, Math.min(420, screenHeightDp - 190)));
        root.addView(choiceScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                choices.length == 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : maxListHeight));
        if (choices.length == 0) {
            TextView empty = new TextView(context);
            empty.setText(context.getString(R.string.no_active_libraries));
            empty.setTextColor(AppUi.MUTED);
            empty.setTextSize(13);
            empty.setPadding(0, AppUi.dp(context, 10), 0, AppUi.dp(context, 10));
            choiceList.addView(empty);
        } else {
            for (int i = 0; i < choices.length; i++) {
                final int index = i;
                Button choice = new Button(context);
                choice.setText(choices[i]);
                choice.setContentDescription(choices[i]);
                AppUi.styleButton(context, choice, AppUi.CONTROL, AppUi.TEXT, 48, 13);
                choiceList.addView(choice);
                choice.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onLibrarySelected(index);
                });
            }
        }

        Button close = new Button(context);
        close.setText(closeText);
        AppUi.styleButton(context, close, AppUi.CONTROL, AppUi.MUTED, 44, 13);
        root.addView(close);
        close.setOnClickListener(v -> {
            dialog.dismiss();
            if (onBack != null) onBack.run();
        });

        dialog.setContentView(root, new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
        dialog.setCanceledOnTouchOutside(true);

        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) {
                activeDialog = null;
            }
        });
        dialog.show();
        activeDialog = dialog;
        applyResponsiveWindow(dialog, context);
    }

    /**
     * Recalcula o painel se o aparelho girar enquanto a lista de SoundFonts/presets estiver aberta.
     * O Activity usa configChanges para preservar o motor de áudio; por isso o diálogo precisa
     * receber explicitamente as novas dimensões e orientação.
     */
    public static void onConfigurationChanged(Context context) {
        Dialog dialog = activeDialog;
        if (dialog == null || !dialog.isShowing()) return;
        applyResponsiveWindow(dialog, context);
    }

    private static void applyResponsiveWindow(Dialog dialog, Context context) {
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        boolean tablet = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        boolean sidePanel = landscape || tablet;
        int screenWidthDp = context.getResources().getConfiguration().screenWidthDp;
        if (screenWidthDp <= 0) {
            float density = context.getResources().getDisplayMetrics().density;
            screenWidthDp = Math.round(context.getResources().getDisplayMetrics().widthPixels / density);
        }
        int sideWidth = AppUi.dp(context, Math.min(480, Math.max(280,
                Math.round(screenWidthDp * 0.48f))));
        int panelWidth = sidePanel ? sideWidth : WindowManager.LayoutParams.MATCH_PARENT;
        int panelHeight = WindowManager.LayoutParams.MATCH_PARENT;

        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = panelWidth;
        params.height = panelHeight;
        params.gravity = sidePanel ? (Gravity.END | Gravity.CENTER_VERTICAL) : Gravity.FILL;
        params.dimAmount = 0f;
        if (sidePanel) {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        }
        params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(params);
        window.setWindowAnimations(sidePanel ? R.style.SoundFontSidePanelAnimation : 0);
        window.setLayout(panelWidth, panelHeight);
        window.setGravity(params.gravity);
        dialog.getWindow().getDecorView().requestLayout();
    }
}
