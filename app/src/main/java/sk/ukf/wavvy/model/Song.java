package sk.ukf.wavvy.model;

public class Song {
    private final String title;
    private final String artist;
    private final String album;
    private final int coverResId;
    private final int audioResId;
    public Song(String title, String artist, String album, int coverResId, int audioResId) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.coverResId = coverResId;
        this.audioResId = audioResId;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public String getAlbum() { return album; }
    public int getCoverResId() {
        return coverResId;
    }
    public int getAudioResId() {
        return audioResId;
    }
}