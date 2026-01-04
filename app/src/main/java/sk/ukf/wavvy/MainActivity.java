package sk.ukf.wavvy;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import sk.ukf.wavvy.model.Song;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private ConstraintLayout miniPlayer;
    private ImageView ivMiniCover;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_search) {
                loadFragment(new SearchFragment());
                return true;
            } else if (id == R.id.nav_playlists) {
                loadFragment(new PlaylistsFragment());
                return true;
            }
            return false;
        });

        miniPlayer = findViewById(R.id.miniPlayer);
        ivMiniCover = findViewById(R.id.ivMiniCover);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPlay = findViewById(R.id.btnMiniPlay);

        miniPlayer.setOnClickListener(v -> openPlayerFromNowPlaying());
        btnMiniPlay.setOnClickListener(v -> openPlayerFromNowPlaying());
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.navHost, fragment)
                .commit();
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }
    private void updateMiniPlayer() {
        if (!NowPlayingRepository.hasNowPlaying(this)) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        int audioResId = NowPlayingRepository.getAudioResId(this);
        Song s = SongRepository.findByAudioResId(audioResId);

        if (s == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }

        miniPlayer.setVisibility(View.VISIBLE);
        ivMiniCover.setImageResource(s.getCoverResId());
        tvMiniTitle.setText(s.getTitle());
        tvMiniArtist.setText(s.getArtist());
        btnMiniPlay.setImageResource(R.drawable.ic_play);
    }
    private void openPlayerFromNowPlaying() {
        int[] q = NowPlayingRepository.getQueueIds(this);
        int idx = NowPlayingRepository.getQueueIndex(this);

        if (q == null || q.length == 0) return;

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_QUEUE_AUDIO_IDS, q);
        intent.putExtra(PlayerActivity.EXTRA_QUEUE_INDEX, idx);
        startActivity(intent);
    }
}