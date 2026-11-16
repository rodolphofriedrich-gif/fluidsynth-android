package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/** Imports SoundFonts into private storage and deduplicates identical files across tracks. */
public final class SoundFontManager {
    private final Context context;

    public SoundFontManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public File importForTrack(Uri uri, int channel) throws Exception {
        return importForTrack(uri, channel, null);
    }

    public File importForTrack(Uri uri, int channel, String displayName) throws Exception {
        if (uri == null || channel < 0 || channel >= 16)
            throw new IllegalArgumentException("Arquivo ou canal inválido");

        File directory = new File(context.getFilesDir(), "soundfonts");
        if (!directory.exists() && !directory.mkdirs())
            throw new IllegalStateException("Não foi possível criar a pasta de SoundFonts");

        String nonce = channel + "-" + System.nanoTime();
        File temp = new File(directory, "import-" + nonce + ".tmp");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException("Não foi possível abrir o arquivo selecionado");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
                out.write(buffer, 0, count);
            }
            out.getFD().sync();
        } catch (Exception error) {
            temp.delete();
            throw error;
        }

        StringBuilder hash = new StringBuilder();
        for (byte value : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        File target = new File(directory, "sf2-" + hash + ".sf2");

        if (target.exists()) {
            temp.delete();
        } else if (!temp.renameTo(target)) {
            temp.delete();
            throw new IllegalStateException("Não foi possível concluir a importação do SF2");
        }
        if (displayName != null && !displayName.trim().isEmpty()) {
            context.getSharedPreferences("soundfont_library_names", Context.MODE_PRIVATE)
                    .edit().putString(target.getName(), displayName.trim()).apply();
        }
        return target;
    }

    public String getDisplayName(File soundFont) {
        if (soundFont == null) return "SoundFont";
        String saved = context.getSharedPreferences("soundfont_library_names", Context.MODE_PRIVATE)
                .getString(soundFont.getName(), null);
        if (saved != null && !saved.trim().isEmpty()) return saved;
        return soundFont.getName().replaceFirst("(?i)\\.sf2$", "");
    }
}