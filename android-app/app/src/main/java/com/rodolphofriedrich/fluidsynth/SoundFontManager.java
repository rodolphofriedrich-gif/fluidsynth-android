package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Copies selected SoundFonts into app-private storage, one file per track. */
public final class SoundFontManager {
    private final Context context;
    public SoundFontManager(Context context) { this.context = context.getApplicationContext(); }
    public File importForTrack(Uri uri, int channel) throws Exception {
        if (uri == null || channel < 0 || channel >= 16) throw new IllegalArgumentException("Arquivo ou canal inválido");
        File directory = new File(context.getFilesDir(), "soundfonts");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Não foi possível criar a pasta de SoundFonts");
        String revision = Long.toString(System.currentTimeMillis());
        File target = new File(directory, "channel-" + (channel + 1) + "-" + revision + ".sf2");
        File temp = new File(directory, "channel-" + (channel + 1) + "-" + revision + ".sf2.tmp");
        try (InputStream in = context.getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException("Não foi possível abrir o arquivo selecionado");
            byte[] buffer = new byte[65536]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.getFD().sync();
        } catch (Exception error) { temp.delete(); throw error; }
        if (target.exists() && !target.delete()) { temp.delete(); throw new IllegalStateException("Não foi possível substituir o SF2 anterior"); }
        if (!temp.renameTo(target)) { temp.delete(); throw new IllegalStateException("Não foi possível concluir a importação do SF2"); }
        return target;
    }
}