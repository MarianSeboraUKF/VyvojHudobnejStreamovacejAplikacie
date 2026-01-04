package sk.ukf.wavvy;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Playlist;
import sk.ukf.wavvy.model.Song;

public class PlaylistDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYLIST_ID = "playlist_id";
    public static final String EXTRA_PLAYLIST_NAME = "playlist_name";
    private TextView tvPlaylistTitle;
    private TextView tvPlaylistMeta;
    private RecyclerView rvPlaylistSongs;
    private SongAdapter adapter;
    private ArrayList<Song> songsInPlaylist;
    private String playlistId;
    private String playlistName = "Playlist";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        tvPlaylistTitle = findViewById(R.id.tvPlaylistTitle);
        tvPlaylistMeta = findViewById(R.id.tvPlaylistMeta);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);

        Intent intent = getIntent();
        playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID);

        String nameFromIntent = intent.getStringExtra(EXTRA_PLAYLIST_NAME);
        if (nameFromIntent != null && !nameFromIntent.trim().isEmpty()) {
            playlistName = nameFromIntent.trim();
        }
        tvPlaylistTitle.setText(playlistName);

        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
        songsInPlaylist = new ArrayList<>();

        adapter = new SongAdapter(
                songsInPlaylist,
                song -> PlayerLauncher.openQueue(PlaylistDetailActivity.this, songsInPlaylist, song),
                this::showRemoveFromPlaylistDialog
        );
        rvPlaylistSongs.setAdapter(adapter);
        loadSongs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSongs();
    }
    private void showRemoveFromPlaylistDialog(Song song) {
        View card = LayoutInflater.from(this).inflate(R.layout.dialog_remove_song, null);

        TextView tvMessage = card.findViewById(R.id.tvMessage);
        View btnRemove = card.findViewById(R.id.btnRemove);
        View btnCancel = card.findViewById(R.id.btnCancel);

        tvMessage.setText("Odstrániť „" + song.getTitle() + "“ z playlistu?");

        android.app.Dialog dialog =
                WavvyDialogs.showCenteredCardDialog(this, this, card);

        btnRemove.setOnClickListener(x -> {
            if (playlistId != null) {
                PlaylistRepository.removeSongFromPlaylist(this, playlistId, song.getAudioResId());
                dialog.dismiss();
                loadSongs();
            }
        });
        btnCancel.setOnClickListener(x -> dialog.dismiss());
    }
    private void loadSongs() {
        songsInPlaylist.clear();

        if (playlistId == null) {
            tvPlaylistMeta.setText("0 skladieb • 0:00");
            adapter.notifyDataSetChanged();
            return;
        }

        Playlist p = PlaylistRepository.findById(this, playlistId);
        if (p == null) {
            tvPlaylistMeta.setText("0 skladieb • 0:00");
            adapter.notifyDataSetChanged();
            return;
        }

        for (Integer audioResId : p.getSongAudioResIds()) {
            Song s = SongRepository.findByAudioResId(audioResId);
            if (s != null) songsInPlaylist.add(s);
        }

        long totalMs = 0;
        for (Song s : songsInPlaylist) {
            totalMs += getDurationMsFromRaw(s.getAudioResId());
        }

        tvPlaylistMeta.setText(songsInPlaylist.size() + " skladieb • " + formatDuration(totalMs));
        adapter.notifyDataSetChanged();
    }
    private long getDurationMsFromRaw(int rawResId) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            android.content.res.AssetFileDescriptor afd = getResources().openRawResourceFd(rawResId);
            if (afd == null) return 0;

            mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            afd.close();

            if (dur == null) return 0;
            return Long.parseLong(dur);
        } catch (Exception e) {
            return 0;
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }
    private String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}