package sk.ukf.wavvy;

import java.util.ArrayList;
import sk.ukf.wavvy.model.Song;

public class SongRepository {
    private static ArrayList<Song> cached;

    public static ArrayList<Song> getSongs() {
        if (cached != null) return new ArrayList<>(cached);

        cached = new ArrayList<>();
        cached.add(new Song("BERI 3", "RAYYY P, Vašo Patejdl, Majkyyy", "kto.som.?", R.drawable.test_cover, R.raw.test_track));
        cached.add(new Song("NEPÝTAM SA", "RAYYY P, Majkyyy", "kto.som.?", R.drawable.test_cover, R.raw.demo_track));
        cached.add(new Song("DO OČÍ", "RAYYY P, Majkyyy", "kto.som.?", R.drawable.test_cover, R.raw.prototype_track));

        return new ArrayList<>(cached);
    }

    public static Song findByAudioResId(int audioResId) {
        ArrayList<Song> songs = getSongs();
        for (Song s : songs) {
            if (s.getAudioResId() == audioResId) return s;
        }
        return null;
    }
}