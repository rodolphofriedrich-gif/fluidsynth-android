package com.rodolphofriedrich.fluidsynth;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.security.MessageDigest;

/** Imports SoundFonts into private storage and deduplicates identical files across tracks. */
public final class SoundFontManager {
    public interface ProgressListener {
        void onProgress(long copiedBytes, long totalBytes);
    }

    private final Context context;

    public SoundFontManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public File importForTrack(Uri uri, int channel) throws Exception {
        return importForTrack(uri, channel, null);
    }

    public File importForTrack(Uri uri, int channel, String displayName) throws Exception {
        return importForTrack(uri, channel, displayName, null);
    }

    public File importForTrack(Uri uri, int channel, String displayName,
                               ProgressListener progressListener) throws Exception {
        if (uri == null || channel < 0 || channel >= 16)
            throw new IllegalArgumentException("Arquivo ou canal inválido");

        File directory = new File(context.getFilesDir(), "soundfonts");
        if (!directory.exists() && !directory.mkdirs())
            throw new IllegalStateException("Não foi possível criar a pasta de SoundFonts");

        String nonce = channel + "-" + System.nanoTime();
        File temp = new File(directory, "import-" + nonce + ".tmp");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long totalBytes = getContentLength(uri);
        long copiedBytes = 0;
        long lastReportedBytes = 0;
        if (progressListener != null) progressListener.onProgress(0, totalBytes);

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException("Não foi possível abrir o arquivo selecionado");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
                out.write(buffer, 0, count);
                copiedBytes += count;
                if (progressListener != null
                        && (copiedBytes - lastReportedBytes >= 262144
                            || (totalBytes > 0 && copiedBytes >= totalBytes))) {
                    progressListener.onProgress(copiedBytes, totalBytes);
                    lastReportedBytes = copiedBytes;
                }
            }
            out.getFD().sync();
            if (progressListener != null && lastReportedBytes != copiedBytes) {
                progressListener.onProgress(copiedBytes, totalBytes);
            }
        } catch (Exception error) {
            temp.delete();
            throw error;
        }

        try {
            validateSf2Header(temp);
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

    private long getContentLength(Uri uri) {
        try (AssetFileDescriptor descriptor =
                     context.getContentResolver().openAssetFileDescriptor(uri, "r")) {
            return descriptor == null ? -1 : descriptor.getLength();
        } catch (Exception ignored) {
            return -1;
        }
    }

    /** Rejects unrelated files before they are registered as SoundFonts. */
    private static void validateSf2Header(File file) throws Exception {
        if (file.length() < 12) {
            throw new IllegalArgumentException("O arquivo é muito pequeno para ser um SoundFont SF2 válido.");
        }
        byte[] header = new byte[12];
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            input.readFully(header);
        }
        String container = new String(header, 0, 4, StandardCharsets.US_ASCII);
        String form = new String(header, 8, 4, StandardCharsets.US_ASCII);
        if (!"RIFF".equals(container) || !"sfbk".equals(form)) {
            throw new IllegalArgumentException("O arquivo selecionado não possui a estrutura de um SoundFont SF2 (RIFF/sfbk).");
        }

        // RIFF stores the payload length as an unsigned little-endian 32-bit value.
        // Reject truncated containers before passing an untrusted file to the native SF2 parser.
        long riffPayloadLength = ((long) header[4] & 0xffL)
                | (((long) header[5] & 0xffL) << 8)
                | (((long) header[6] & 0xffL) << 16)
                | (((long) header[7] & 0xffL) << 24);
        long declaredFileLength = riffPayloadLength + 8L;
        if (riffPayloadLength < 4L || declaredFileLength > file.length()) {
            throw new IllegalArgumentException("O arquivo SoundFont está incompleto ou possui tamanho RIFF inválido.");
        }
    }

    public void removeDisplayName(File soundFont) {
        if (soundFont == null) return;
        context.getSharedPreferences("soundfont_library_names", Context.MODE_PRIVATE)
                .edit().remove(soundFont.getName()).apply();
    }

    public String getDisplayName(File soundFont) {
        if (soundFont == null) return "SoundFont";
        String saved = context.getSharedPreferences("soundfont_library_names", Context.MODE_PRIVATE)
                .getString(soundFont.getName(), null);
        if (saved != null && !saved.trim().isEmpty()) return saved;
        android.content.SharedPreferences tracks = context.getSharedPreferences("track_state", Context.MODE_PRIVATE);
        for (int channel = 0; channel < 16; channel++) {
            String path = tracks.getString("track_" + channel + "_path", null);
            String name = tracks.getString("track_" + channel + "_name", null);
            if (soundFont.getAbsolutePath().equals(path) && name != null && !name.trim().isEmpty()) return name;
        }
        return soundFont.getName().replaceFirst("(?i)\\.sf2$", "");
    }
}