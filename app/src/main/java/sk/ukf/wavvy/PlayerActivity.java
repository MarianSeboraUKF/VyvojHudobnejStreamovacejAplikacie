package sk.ukf.wavvy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private Button btnPlayPause;
    private Button btnStop;
    private SeekBar seekBar;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;
    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private android.widget.ImageView ivCover;
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null && !isUserSeeking) {
                long pos = player.getCurrentPosition();
                seekBar.setProgress((int) pos);
                tvCurrentTime.setText(formatTime(pos));
            }
            uiHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        tvSongTitle = findViewById(R.id.tvSongTitle);
        tvSongArtist = findViewById(R.id.tvSongArtist);
        ivCover = findViewById(R.id.ivCover);

        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        int coverResId = getIntent().getIntExtra("coverResId", R.drawable.test_cover);
        int audioResId = getIntent().getIntExtra("audioResId", R.raw.test_track);

        tvSongTitle.setText(title != null ? title : "Unknown title");
        tvSongArtist.setText(artist != null ? artist : "Unknown artist");
        ivCover.setImageResource(coverResId);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnStop = findViewById(R.id.btnStop);

        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        player = new ExoPlayer.Builder(this).build();

        MediaItem mediaItem = MediaItem.fromUri("android.resource://" + getPackageName() + "/" + audioResId);
        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long dur = player.getDuration();
                    if (dur > 0) {
                        seekBar.setMax((int) dur);
                        tvTotalTime.setText(formatTime(dur));
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    stopPlayback();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updateButtonText();
            }
        });

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnStop.setOnClickListener(v -> stopPlayback());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player != null) {
                    player.seekTo(sb.getProgress());
                }
            }
        });

        updateButtonText();
    }

    @Override
    protected void onStart() {
        super.onStart();
        uiHandler.post(progressUpdater);
    }

    @Override
    protected void onStop() {
        super.onStop();
        uiHandler.removeCallbacks(progressUpdater);
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(progressUpdater);
        if (player != null) {
            player.release();
            player = null;
        }
    }
    private void togglePlayPause() {
        if (player == null) return;

        if (player.isPlaying()) player.pause();
        else player.play();

        updateButtonText();
    }
    private void stopPlayback() {
        if (player == null) return;

        player.pause();
        player.seekTo(0);

        seekBar.setProgress(0);
        tvCurrentTime.setText("00:00");

        updateButtonText();
    }
    private void updateButtonText() {
        if (player == null) return;
        btnPlayPause.setText(player.isPlaying() ? "Pause" : "Play");
    }
    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}