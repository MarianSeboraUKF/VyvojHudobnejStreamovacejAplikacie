package sk.ukf.wavvy;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Song;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvSongs = view.findViewById(R.id.rvSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));

        ArrayList<Song> songs = new ArrayList<>();
        songs.add(new Song("BERI 3", "RAYYY P, Vašo Patejdl, Majkyyy", R.drawable.test_cover, R.raw.test_track));
        songs.add(new Song("NEPÝTAM SA", "RAYYY P, Majkyyy", R.drawable.test_cover, R.raw.demo_track));
        songs.add(new Song("DO OČÍ", "RAYYY P, Majkyyy", R.drawable.test_cover, R.raw.prototype_track));

        SongAdapter adapter = new SongAdapter(songs, song -> {
            Intent intent = new Intent(requireContext(), PlayerActivity.class);
            intent.putExtra("title", song.getTitle());
            intent.putExtra("artist", song.getArtist());
            intent.putExtra("coverResId", song.getCoverResId());
            intent.putExtra("audioResId", song.getAudioResId());
            startActivity(intent);
        });
        rvSongs.setAdapter(adapter);

        return view;
    }
}