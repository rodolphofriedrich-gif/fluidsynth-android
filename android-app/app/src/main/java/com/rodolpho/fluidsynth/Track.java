package com.rodolpho.fluidsynth;

public final class Track {
    public final int channel;
    private String soundFontPath;
    private String soundFontName;
    private boolean enabled = true;

    Track(int channel) { this.channel = channel; }
    public String getSoundFontPath() { return soundFontPath; }
    public String getSoundFontName() { return soundFontName; }
    public boolean isEnabled() { return enabled; }
    void setSoundFont(String path, String name) { soundFontPath = path; soundFontName = name; }
    void setEnabled(boolean value) { enabled = value; }
    public boolean hasSoundFont() { return soundFontPath != null && !soundFontPath.isEmpty(); }
}