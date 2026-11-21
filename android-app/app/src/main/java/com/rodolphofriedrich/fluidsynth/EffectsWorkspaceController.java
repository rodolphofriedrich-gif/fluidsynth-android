package com.rodolphofriedrich.fluidsynth;

import android.app.Activity;
import android.content.res.Configuration;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/** Gerencia o painel de efeitos responsivo, sem acessar o motor de áudio. */
public final class EffectsWorkspaceController {
    private final Activity activity;
    private final LinearLayout workspace;
    private final View midiView;
    private final View effectsView;
    private final Button toggleButton;
    private boolean open;

    public EffectsWorkspaceController(Activity activity, LinearLayout workspace,
            View midiView, View effectsView, Button toggleButton) {
        this.activity = activity;
        this.workspace = workspace;
        this.midiView = midiView;
        this.effectsView = effectsView;
        this.toggleButton = toggleButton;
        apply(false);
    }

    public boolean isOpen() { return open; }

    public void toggle() { setOpen(!open); }

    public void setOpen(boolean value) {
        open = value;
        apply(true);
    }

    public void onConfigurationChanged() {
        // A rotação altera as dimensões imediatamente; não mantenha uma transição antiga ativa.
        apply(false);
    }

    private boolean splitMode() {
        Configuration configuration = activity.getResources().getConfiguration();
        // Em paisagem, telefones também usam MIDI + efeitos lado a lado.
        // Tablets mantêm a divisão em qualquer orientação.
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                || configuration.smallestScreenWidthDp >= 600;
    }

    private void apply(boolean animate) {
        TransitionManager.endTransitions(workspace);
        if (animate && workspace.isAttachedToWindow()) {
            TransitionManager.beginDelayedTransition(workspace,
                    new AutoTransition().setDuration(220));
        }

        LinearLayout.LayoutParams midiParams;
        LinearLayout.LayoutParams effectsParams;
        if (!open) {
            midiView.setVisibility(View.VISIBLE);
            effectsView.setVisibility(View.GONE);
            midiParams = new LinearLayout.LayoutParams(-1, -1, 1f);
            effectsParams = new LinearLayout.LayoutParams(0, 0, 0f);
        } else if (splitMode()) {
            midiView.setVisibility(View.VISIBLE);
            effectsView.setVisibility(View.VISIBLE);
            // Mais largura para os efeitos evita rótulos quebrados e knobs comprimidos.
            midiParams = new LinearLayout.LayoutParams(0, -1, .56f);
            effectsParams = new LinearLayout.LayoutParams(0, -1, .44f);
            effectsParams.leftMargin = AppUi.dp(activity, 8);
        } else {
            midiView.setVisibility(View.GONE);
            effectsView.setVisibility(View.VISIBLE);
            midiParams = new LinearLayout.LayoutParams(0, 0, 0f);
            effectsParams = new LinearLayout.LayoutParams(-1, -1, 1f);
        }

        midiView.setLayoutParams(midiParams);
        effectsView.setLayoutParams(effectsParams);
        toggleButton.setSelected(open);
        toggleButton.setBackground(AppUi.ripple(activity,
                open ? AppUi.ACCENT : AppUi.CONTROL, 10, AppUi.BORDER));
        toggleButton.setContentDescription(activity.getString(open
                ? R.string.effects_panel_close : R.string.effects_panel_open));
    }
}
