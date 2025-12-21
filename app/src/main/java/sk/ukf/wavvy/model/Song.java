package sk.ukf.wavvy.model;

public class Song {
    private final String title;
    private final String artist;
    private final int coverResId;
    private final int audioResId;
    public Song(String title, String artist, int coverResId, int audioResId) {
        this.title = title;
        this.artist = artist;
        this.coverResId = coverResId;
        this.audioResId = audioResId;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public int getCoverResId() {
        return coverResId;
    }
    public int getAudioResId() {
        return audioResId;
    }
}