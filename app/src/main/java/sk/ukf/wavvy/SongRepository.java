package sk.ukf.wavvy;

import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class SongRepository {
    public static ArrayList<Song> getSongs() {
        ArrayList<Song> songs = new ArrayList<>();

        songs.add(new Song("BERI 3", "RAYYY P, Vašo Patejdl, Majkyyy", "kto.som.?", R.drawable.test_cover, R.raw.test_track));
        songs.add(new Song("NEPÝTAM SA", "RAYYY P, Majkyyy", "kto.som.?", R.drawable.test_cover, R.raw.demo_track));
        songs.add(new Song("DO OČÍ", "RAYYY P, Majkyyy", "kto.som.?", R.drawable.test_cover, R.raw.prototype_track));

        return songs;
    }
    public static Song findByAudioResId(int audioResId) {
        for (Song s : getSongs()) {
            if (s.getAudioResId() == audioResId) return s;
        }
        return null;
    }
}