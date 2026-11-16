package com.rodolphofriedrich.fluidsynth;

public final class Track {
    public final int channel;
    private String soundFontPath;
    private String soundFontName;
    private String presetName;
    private int bank;
    private int program;
    private boolean enabled = true;
    Track(int channel) { this.channel = channel; }
    public String getSoundFontPath() { return soundFontPath; }
    public String getSoundFontName() { return soundFontName; }
    public String getPresetName() { return presetName; }
    public int getBank() { return bank; }
    public int getProgram() { return program; }
    void setPreset(int bank, int program, String name) { this.bank=bank; this.program=program; this.presetName=name; }
    public boolean isEnabled() { return enabled; }
    void setSoundFont(String path, String name) { soundFontPath = path; soundFontName = name; }
    void setEnabled(boolean value) { enabled = value; }
    public boolean hasSoundFont() { return soundFontPath != null && !soundFontPath.isEmpty(); }
}