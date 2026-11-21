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
 * Painel permanente de seleção/configuração do SoundFont.
 * Regra de layout: telefone em retrato = tela cheia; paisagem ou tablet = painel lateral
 * retrátil à direita, preservando a tela MIDI ao fundo. Não converter para bottom sheet.
 */
public final class ChannelInstrumentSheet {
    public interface Listener {
        void onLibrarySelected(int index);
    }

    private ChannelInstrumentSheet() {}

    public static void show(Context context, int channel, String currentInstrument,
            String[] libraries, Listener listener) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AppUi.dp(context, 20), AppUi.dp(context, 18),
                AppUi.dp(context, 20), AppUi.dp(context, 24));
        root.setBackground(AppUi.rounded(context, AppUi.PANEL, 20, AppUi.BORDER));

        TextView title = new TextView(context);
        title.setText(context.getString(R.string.channel_picker_title, channel + 1));
        title.setTextColor(AppUi.TEXT);
        title.setTextSize(18);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText(currentInstrument);
        subtitle.setTextColor(AppUi.MUTED);
        subtitle.setTextSize(13);
        subtitle.setMaxLines(2);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = AppUi.dp(context, 5);
        subtitleParams.bottomMargin = AppUi.dp(context, 12);
        root.addView(subtitle, subtitleParams);

        TextView section = AppUi.sectionLabel(context,
                context.getString(R.string.soundfont_libraries_title));
        root.addView(section);
        LinearLayout libraryList = new LinearLayout(context);
        libraryList.setOrientation(LinearLayout.VERTICAL);
        ScrollView libraryScroll = new ScrollView(context);
        libraryScroll.setFillViewport(false);
        libraryScroll.addView(libraryList, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        int maxListHeight = Math.round(context.getResources().getDisplayMetrics().heightPixels * .55f);
        root.addView(libraryScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                libraries.length == 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : maxListHeight));
        if (libraries.length == 0) {
            TextView empty = new TextView(context);
            empty.setText(context.getString(R.string.no_active_libraries));
            empty.setTextColor(AppUi.MUTED);
            empty.setTextSize(13);
            empty.setPadding(0, AppUi.dp(context, 10), 0, AppUi.dp(context, 10));
            libraryList.addView(empty);
        } else {
            for (int i = 0; i < libraries.length; i++) {
                final int index = i;
                Button library = new Button(context);
                library.setText(libraries[i]);
                library.setContentDescription(context.getString(R.string.channel_library_accessibility, libraries[i]));
                AppUi.styleButton(context, library, AppUi.CONTROL, AppUi.TEXT, 48, 13);
                libraryList.addView(library);
                library.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onLibrarySelected(index);
                });
            }
        }

        Button cancel = new Button(context);
        cancel.setText(R.string.cancel);
        AppUi.styleButton(context, cancel, AppUi.CONTROL, AppUi.MUTED, 44, 13);
        root.addView(cancel);
        cancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root, new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        boolean tablet = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        boolean sidePanel = landscape || tablet;
        float density = context.getResources().getDisplayMetrics().density;
        int screenWidthDp = Math.round(context.getResources().getDisplayMetrics().widthPixels / density);
        int sideWidth = AppUi.dp(context, Math.min(480, Math.max(280,
                Math.round(screenWidthDp * 0.48f))));
        int panelWidth = sidePanel ? sideWidth : WindowManager.LayoutParams.MATCH_PARENT;
        int panelHeight = WindowManager.LayoutParams.MATCH_PARENT;
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = panelWidth;
            params.height = panelHeight;
            params.gravity = sidePanel ? (Gravity.END | Gravity.CENTER_VERTICAL) : Gravity.FILL;
            params.dimAmount = sidePanel ? 0.22f : 0.08f;
            window.setAttributes(params);
            if (sidePanel) window.setWindowAnimations(R.style.SoundFontSidePanelAnimation);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(panelWidth, panelHeight);
            window.setGravity(sidePanel ? (Gravity.END | Gravity.CENTER_VERTICAL) : Gravity.FILL);
        }
    }
}
