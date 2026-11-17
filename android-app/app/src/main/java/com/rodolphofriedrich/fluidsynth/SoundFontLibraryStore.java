package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Tracks which imported SoundFonts are available for new instrument selections. */
public final class SoundFontLibraryStore {
    private final File directory;
    private final SharedPreferences preferences;
    private final SoundFontManager soundFontManager;

    public SoundFontLibraryStore(Context context, SoundFontManager soundFontManager) {
        Context appContext = context.getApplicationContext();
        this.directory = new File(appContext.getFilesDir(), "soundfonts");
        this.preferences = appContext.getSharedPreferences(
                "soundfont_library_state", Context.MODE_PRIVATE);
        this.soundFontManager = soundFontManager;
    }

    public synchronized List<File> getLibraries() {
        File[] files = directory.listFiles((dir, name) ->
                name.toLowerCase(java.util.Locale.ROOT).endsWith(".sf2"));
        List<File> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) result.add(file);
            }
        }
        Collections.sort(result, Comparator.comparing(
                soundFontManager::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return Collections.unmodifiableList(result);
    }

    /** Newly imported libraries are enabled by default. */
    public boolean isEnabled(File library) {
        if (library == null) return false;
        return preferences.getBoolean(preferenceKey(library), true);
    }

    public void setEnabled(File library, boolean enabled) {
        if (library == null) throw new IllegalArgumentException("Biblioteca inválida");
        preferences.edit().putBoolean(preferenceKey(library), enabled).apply();
    }

    private String preferenceKey(File library) {
        return "enabled_" + library.getName();
    }
}
