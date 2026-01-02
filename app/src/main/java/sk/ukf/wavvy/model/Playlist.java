package sk.ukf.wavvy.model;

import java.util.ArrayList;

public class Playlist {
    private String id;
    private String name;
    private ArrayList<Integer> songAudioResIds;

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
        this.songAudioResIds = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public ArrayList<Integer> getSongAudioResIds() {
        if (songAudioResIds == null) songAudioResIds = new ArrayList<>();
        return songAudioResIds;
    }
    public void addSong(int audioResId) {
        if (!getSongAudioResIds().contains(audioResId)) {
            getSongAudioResIds().add(audioResId);
        }
    }
    public void removeSong(int audioResId) {
        getSongAudioResIds().remove((Integer) audioResId);
    }
}